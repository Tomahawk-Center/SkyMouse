package server

import (
	"sync"
	"time"
)

type Session struct {
	id          string
	udpToken    uint32
	mu          sync.RWMutex
	lastActive  time.Time
	isHandshake bool
}

func NewSession(id string, udpToken uint32) *Session {
	return &Session{
		id:       id,
		udpToken: udpToken,
	}
}

func (s *Session) SetLastActive(lastActive time.Time) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.lastActive = lastActive
}

func (s *Session) SetIsHandshake(isHandshake bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.isHandshake = isHandshake
}

func (s *Session) Id() string {
	return s.id
}

func (s *Session) UdpToken() uint32 {
	return s.udpToken
}

func (s *Session) LastActive() time.Time {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.lastActive
}
func (s *Session) IsHandshake() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.isHandshake
}
