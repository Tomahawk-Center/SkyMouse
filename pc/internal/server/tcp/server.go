package tcp

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"sync"

	"github.com/Tomahawk-Center/SkyMouse/pc/internal/commands"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/session"
	"github.com/Tomahawk-Center/SkyMouse/pc/internal/util/version_verifier"
	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"google.golang.org/protobuf/proto"
)

var (
	ErrConnNotFound = errors.New("connection not found")
	ErrNilConn      = errors.New("nil connection")
)

type Server struct {
	addr        string
	sm          *session.Manager
	ln          net.Listener
	quitCh      chan struct{}
	wg          sync.WaitGroup
	handler     server.EventHandler
	getUdpPort  func() (int, error)
	protobufVer string
	mu          sync.Mutex
	conns       map[string]net.Conn
}

func NewServer(
	addr string,
	sessionManager *session.Manager,
	handler server.EventHandler,
	udpPortProvider func() (int, error),
	serverVersion string,
) (*Server, error) {
	if handler == nil {
		return nil, errors.New("handler cannot be nil")
	}

	if sessionManager == nil {
		return nil, errors.New("sessionManager cannot be nil")
	}

	return &Server{
		addr:        addr,
		quitCh:      make(chan struct{}),
		conns:       make(map[string]net.Conn),
		handler:     handler,
		getUdpPort:  udpPortProvider,
		sm:          sessionManager,
		protobufVer: serverVersion,
	}, nil
}

func (s *Server) Start() error {
	ln, err := net.Listen("tcp", s.addr)
	if err != nil {
		return err
	}

	s.ln = ln

	s.wg.Add(1)
	go s.acceptLoop()

	log.Printf("TCP Server started on %s\n", ln.Addr().String())
	return nil
}

func (s *Server) Stop() {
	close(s.quitCh)
	if s.ln != nil {
		_ = s.ln.Close()
	}
	s.wg.Wait()
	log.Println("Server TCP shut down successfully")
}

func (s *Server) StopForce() {
	close(s.quitCh)
	if s.ln != nil {
		_ = s.ln.Close()
	}

	s.mu.Lock()
	for _, conn := range s.conns {
		_ = conn.Close()
	}
	s.mu.Unlock()

	s.wg.Wait()
	log.Println("Server TCP forcibly shut down")
}

func (s *Server) acceptLoop() {
	defer s.wg.Done()

	for {
		conn, err := s.ln.Accept()
		if err != nil {
			select {
			case <-s.quitCh:
				return
			default:
				log.Printf("Accept connection failed: %v\n", err)
				continue
			}
		}

		s.wg.Add(1)
		go s.handleConnection(conn)
	}
}

func (s *Server) handleConnection(conn net.Conn) {
	defer s.wg.Done()

	sess := s.sm.CreateSession()
	id := sess.Id()

	defer func() {
		_ = conn.Close()
		s.mu.Lock()
		delete(s.conns, id)
		s.mu.Unlock()
		s.sm.RemoveSession(id)
	}()

	s.mu.Lock()
	s.conns[id] = conn
	s.mu.Unlock()

	log.Printf("New connection from %s\n", conn.RemoteAddr().String())

	for {
		var size int32
		err := binary.Read(conn, binary.BigEndian, &size)
		if err != nil {
			if err != io.EOF {
				log.Printf("Read size failed: %v\n", err)
				return
			}
			return
		}

		buf := make([]byte, size)
		_, err = io.ReadFull(conn, buf)

		if err != nil {
			log.Printf("Read body failed: %v\n", err)
			return
		}

		var msg protoapi.MessageToServer
		err = proto.Unmarshal(buf, &msg)
		if err != nil {
			log.Printf("Unmarshal message failed: %v\n", err)
			return
		}

		log.Println("Received message:")
		log.Println(&msg)

		s.routeMessage(sess, &msg)
	}
}

func (s *Server) sendProto(sessionId string, msg proto.Message) error {
	b, err := proto.Marshal(msg)
	if err != nil {
		return fmt.Errorf("send protobuf message failed: %w", err)
	}

	packet := make([]byte, 4+len(b))
	binary.BigEndian.PutUint32(packet[0:4], uint32(len(b)))
	copy(packet[4:], b)

	s.mu.Lock()
	conn, ok := s.conns[sessionId]
	s.mu.Unlock()

	if !ok {
		return fmt.Errorf("send protobuf message failed: %w", ErrConnNotFound)
	}

	c := conn
	if c == nil {
		return fmt.Errorf("send protobuf message failed: %w", ErrNilConn)
	}

	_, err = c.Write(packet)
	if err != nil {
		return fmt.Errorf("send protobuf message failed: %w", err)
	}

	return nil
}

func (s *Server) handlePing(sessionId string) error {
	msg := &protoapi.MessageToClient{
		Event: &protoapi.MessageToClient_Pong{},
	}

	return s.sendProto(sessionId, msg)
}

func (s *Server) handleClientHello(sess *session.Session, clientHelloMsg *protoapi.ClientHello) error {
	serverHello := &protoapi.ServerHello{}
	serverHello.ServerVersion = s.protobufVer
	udpPort, err := s.getUdpPort()
	if err != nil {
		udpPort = 0
	}
	serverHello.UdpPort = int32(udpPort)
	serverHello.UdpToken = sess.UdpToken()

	msg := &protoapi.MessageToClient{
		Event: &protoapi.MessageToClient_ServerHello{
			ServerHello: serverHello,
		},
	}

	b, err := proto.Marshal(msg)
	if err != nil {
		return err
	}

	packet := make([]byte, 4+len(b))

	binary.BigEndian.PutUint32(packet[0:4], uint32(len(b))) // TODO perf may be improved
	copy(packet[4:], b)
	s.mu.Lock()
	conn, ok := s.conns[sess.Id()]
	s.mu.Unlock()
	if !ok {
		return errors.New("connection not found")
	}
	c := conn
	if c == nil {
		return errors.New("nil connection")
	}
	_, err = c.Write(packet)
	if err != nil {
		return err
	}

	err = version_verifier.VerifyClientVersion(clientHelloMsg.ClientVersion, s.protobufVer)
	if err != nil {
		log.Printf("Handshake state is not set because version check failed for session: %s, reason: %v", sess.Id(), err)
	} else {
		sess.SetIsHandshake(true)
		log.Println("Handshake state set to true for session:", sess.Id())
	}

	return nil
}

func (s *Server) routeMessage(sess *session.Session, m *protoapi.MessageToServer) {
	switch m.Event.(type) {
	case *protoapi.MessageToServer_EmulatorEvent:
		if sess.IsHandshake() {
			s.handler.Handle(sess.Id(), m.GetEmulatorEvent())
		}

	case *protoapi.MessageToServer_Command:
		if sess.IsHandshake() {
			var err error
			switch m.GetCommand() {
			case protoapi.CommandEvent_COMMAND_SHUT_DOWN:
				err = commands.Shutdown()
			case protoapi.CommandEvent_COMMAND_SLEEP:
				err = commands.Sleep()
			case protoapi.CommandEvent_COMMAND_LOCK_SCREEN:
				err = commands.LockScreen()
			default:
				return
			}
			if err != nil {
				log.Printf("Command execute failed: %v", err)
			}
		}

	case *protoapi.MessageToServer_Ping:
		err := s.handlePing(sess.Id())
		if err != nil {
			log.Printf("Send pong failed: %v\n", err)
		}

	case *protoapi.MessageToServer_ClientHello:
		err := s.handleClientHello(sess, m.GetClientHello())
		if err != nil {
			log.Printf("Send client hello failed: %v\n", err)
		}
	}
}
