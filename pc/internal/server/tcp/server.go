package tcp

import (
	"encoding/binary"
	"io"
	"log"
	"net"
	"sync"

	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"google.golang.org/protobuf/proto"
)

type Server struct {
	addr    string
	ln      net.Listener
	quitCh  chan struct{}
	wg      sync.WaitGroup
	mu      sync.Mutex
	conns   map[net.Conn]struct{}
	handler EventHandler
}

func NewServer(addr string, handler EventHandler) *Server {
	return &Server{
		addr:    addr,
		quitCh:  make(chan struct{}),
		conns:   make(map[net.Conn]struct{}),
		handler: handler,
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
				log.Printf("Copy connection failed: %v\n", err)
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

	s.mu.Lock()
	s.conns[conn] = struct{}{}
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

		if s.handler != nil {
			s.handler.Handle(&msg)
		}
		_, err = conn.Write([]byte("ACK\n"))
		if err != nil {
			log.Printf("Send ACK failed with error: %v\n", err)
			return
		}
	}
}
