package udp

import (
	"errors"
	"fmt"
	"log"
	"net"
	"sync"
	"time"

	"github.com/Tomahawk-Center/SkyMouse/pc/internal/server"
	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"google.golang.org/protobuf/proto"
)

type Server struct {
	addr       *net.UDPAddr
	sm         *server.SessionManager
	conn       *net.UDPConn
	quitCh     chan struct{}
	wg         sync.WaitGroup
	handler    server.EventHandler
	mu         sync.Mutex
	addrByUuid map[string]*net.UDPAddr
}

func NewServer(addr string, sm *server.SessionManager, handler server.EventHandler) (*Server, error) {
	if handler == nil {
		return nil, errors.New("handler cannot be nil")
	}

	ad, err := net.ResolveUDPAddr("udp", addr)
	if err != nil {
		return nil, err
	}
	return &Server{
		addr:       ad,
		sm:         sm,
		quitCh:     make(chan struct{}),
		handler:    handler,
		addrByUuid: make(map[string]*net.UDPAddr),
	}, nil

}

func (s *Server) Start() error {
	c, err := net.ListenUDP("udp", s.addr)
	if err != nil {
		return err
	}

	s.conn = c

	s.wg.Add(1)
	go s.acceptLoop()

	log.Printf("UDP Server started on %s\n", s.addr)
	return nil
}

func (s *Server) Stop() {
	close(s.quitCh)
	if s.conn != nil {
		_ = s.conn.Close()
	}
	s.wg.Wait()
	log.Println("Server UDP shut down successfully")
}

func (s *Server) Port() (int, error) {
	if s.conn == nil {
		return 0, fmt.Errorf("UDP Server not started")
	}
	addr := s.conn.LocalAddr().(*net.UDPAddr)
	return addr.Port, nil
}

// SendProto sends protobuf message to last connected IP addr
func (s *Server) SendProto(sessionId string, msg proto.Message) error { // TODO RC?
	if s.conn == nil {
		return fmt.Errorf("UDP Server not started")
	}
	data, err := proto.Marshal(msg)
	if err != nil {
		return fmt.Errorf("could not marshal proto: %v", err)
	}

	s.mu.Lock()
	addr, ok := s.addrByUuid[sessionId]
	s.mu.Unlock()
	if !ok {
		return errors.New("udp addr not found")
	}
	_, err = s.conn.WriteToUDP(data, addr)
	if err != nil {
		return fmt.Errorf("could not send proto: %v", err)
	}
	return nil
}

func (s *Server) acceptLoop() {
	defer s.wg.Done()

	buf := make([]byte, 65535)
	for {

		n, remoteAddr, err := s.conn.ReadFromUDP(buf)
		if err != nil {
			select {
			case <-s.quitCh:
				return
			default:
			}

			if errors.Is(err, net.ErrClosed) {
				return
			}

			log.Printf("UDP ReadFromUDP error: %s\n", err)
			continue
		}
		// log.Printf("UDP Received %d bytes from %s: %s\n", n, remoteAddr, string(buf[:n]))
		var msg protoapi.UdpMessageToServer
		if err := proto.Unmarshal(buf[:n], &msg); err != nil {
			log.Printf("UDP Protobuf unmarshal error from %s: %v\n", remoteAddr, err)
			continue
		}

		udpToken := msg.UdpToken
		sess, ok := s.sm.GetByUdpToken(udpToken)
		if !ok {
			continue
		}
		sess.SetLastActive(time.Now())

		s.mu.Lock()
		s.addrByUuid[sess.Id()] = remoteAddr
		s.mu.Unlock()

		if !sess.IsHandshake() {
			continue
		}

		emEv := msg.GetEmulatorEvent()
		if emEv != nil {
			s.handler.Handle(sess.Id(), emEv)
		}

	}
}
