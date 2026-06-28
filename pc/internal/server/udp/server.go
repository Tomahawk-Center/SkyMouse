package udp

import (
	"errors"
	"log"
	"net"
	"sync"
)

type Server struct {
	addr   *net.UDPAddr
	conn   *net.UDPConn
	quitCh chan struct{}
	wg     sync.WaitGroup
}

func NewServer(addr string) (*Server, error) {
	ad, err := net.ResolveUDPAddr("udp", addr)
	if err != nil {
		return nil, err
	}
	return &Server{
		addr:   ad,
		quitCh: make(chan struct{}),
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
		log.Printf("UDP Received %d bytes from %s: %s\n", n, remoteAddr, string(buf[:n]))
	}
}
