package tcp

import (
	"encoding/binary"
	"errors"
	"io"
	"log"
	"net"
	"sync"
	"time"

	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server"
	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"google.golang.org/protobuf/proto"
)

type Server struct {
	addr       string
	ln         net.Listener
	quitCh     chan struct{}
	wg         sync.WaitGroup
	handler    server.EventHandler
	getUdpPort func() (int, error)
	mu         sync.Mutex
	conns      map[net.Conn]*clientSession
}

func NewServer(addr string, handler server.EventHandler, udpPortProvider func() (int, error)) *Server {
	return &Server{
		addr:       addr,
		quitCh:     make(chan struct{}),
		conns:      make(map[net.Conn]*clientSession),
		handler:    handler,
		getUdpPort: udpPortProvider,
	}
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
	for conn := range s.conns {
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
	defer func(conn net.Conn) {
		_ = conn.Close()
	}(conn)

	session := &clientSession{conn: conn, lastActive: time.Now()}

	s.mu.Lock()
	s.conns[conn] = session
	s.mu.Unlock()

	defer func() {
		s.mu.Lock()
		delete(s.conns, conn)
		s.mu.Unlock()
	}()

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

		s.conns[conn].lastActive = time.Now()

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

func (s *Server) handlePing(sess *clientSession) error {
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

func (s *Server) handleClientHello(sess *clientSession, m *protoapi.MessageToServer) error {
	serverHello := &protoapi.ServerHello{}
	serverHello.ServerVersion = "1" // TODO remove hardcoded server version
	udpPort, err := s.getUdpPort()
	if err != nil {
		udpPort = 0
	}
	serverHello.UdpPort = int32(udpPort)

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
	_, err = sess.conn.Write(packet)
	return err
}

func (s *Server) routeMessage(sess *clientSession, m *protoapi.MessageToServer) {
	// TODO separate event for emulator messages in .proto
	switch m.Event.(type) {
	case *protoapi.MessageToServer_Mouse:
		s.handler.Handle(m)
	case *protoapi.MessageToServer_Click:
		s.handler.Handle(m)
	case *protoapi.MessageToServer_Scroll:
		s.handler.Handle(m)
	case *protoapi.MessageToServer_Ping:
		err := s.handlePing(sess)
		if err != nil {
			log.Printf("Send pong failed: %v\n", err)
		}
	case *protoapi.MessageToServer_ClientHello:
		err := s.handleClientHello(sess, m)
		if err != nil {
			log.Printf("Send client hello failed: %v\n", err)
		}
	}
}
