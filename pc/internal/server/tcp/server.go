package tcp

import (
	"encoding/binary"
	"errors"
	"io"
	"log"
	"net"
	"sync"

	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server"
	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"google.golang.org/protobuf/proto"
)

type Server struct {
	addr       string
	sm         *server.SessionManager
	ln         net.Listener
	quitCh     chan struct{}
	wg         sync.WaitGroup
	handler    server.EventHandler
	getUdpPort func() (int, error)
	mu         sync.Mutex
	conns      map[string]net.Conn
}

func NewServer(addr string, sessionManager *server.SessionManager, handler server.EventHandler, udpPortProvider func() (int, error)) (*Server, error) {
	if handler == nil {
		return nil, errors.New("handler cannot be nil")
	}

	if sessionManager == nil {
		return nil, errors.New("sessionManager cannot be nil")
	}

	return &Server{
		addr:       addr,
		quitCh:     make(chan struct{}),
		conns:      make(map[string]net.Conn),
		handler:    handler,
		getUdpPort: udpPortProvider,
		sm:         sessionManager,
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

	log.Printf("TCP Server started on %s\n", s.addr)
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

	session := s.sm.CreateSession()
	id := session.Id()

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

		s.routeMessage(session, &msg)
	}
}

func (s *Server) handlePing() error {
	//TODO
	return errors.New("ping-pong not implemented yet")

	//pong := &protoapi.Pong{}
	//
	////TODO wrap pong in MessageToClient
	//
	//b, err := proto.Marshal(pong)
	//if err != nil {
	//	return err
	//}
	//
	//_, err = sess.conn.Write(b)
	//if err != nil {
	//	return err
	//}
	//return nil
}

func (s *Server) handleClientHello(sess *server.Session) error {
	serverHello := &protoapi.ServerHello{}
	serverHello.ServerVersion = "2.0" // TODO remove hardcoded server version
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

	sess.SetIsHandshake(true)
	return nil
}

func (s *Server) routeMessage(sess *server.Session, m *protoapi.MessageToServer) {
	switch m.Event.(type) {
	case *protoapi.MessageToServer_EmulatorEvent:
		if sess.IsHandshake() {
			s.handler.Handle(sess.Id(), m.GetEmulatorEvent())
		}

	case *protoapi.MessageToServer_Ping:
		err := s.handlePing()
		if err != nil {
			log.Printf("Send pong failed: %v\n", err)
		}

	case *protoapi.MessageToServer_ClientHello:
		err := s.handleClientHello(sess)
		if err != nil {
			log.Printf("Send client hello failed: %v\n", err)
		}
	}
}
