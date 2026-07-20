package session

import (
	"crypto/rand"
	"encoding/binary"
	"sync"

	"github.com/google/uuid"
)

type Manager struct {
	mu         sync.Mutex
	byUuid     map[string]*Session
	byUdpToken map[uint32]*Session
}

func NewSessionManager() *Manager {
	return &Manager{
		byUuid:     make(map[string]*Session),
		byUdpToken: make(map[uint32]*Session),
	}
}

func (sm *Manager) CreateSession() *Session {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	id := uuid.NewString()
	udpToken := sm.generateUdpTokenLocked()

	s := NewSession(id, udpToken)

	sm.byUuid[id] = s
	sm.byUdpToken[udpToken] = s
	return s
}

func (sm *Manager) GetByUuid(id string) (s *Session, ok bool) {
	sm.mu.Lock()
	defer sm.mu.Unlock()
	s, ok = sm.byUuid[id]
	return s, ok
}

func (sm *Manager) GetByUdpToken(id uint32) (s *Session, ok bool) {
	sm.mu.Lock()
	defer sm.mu.Unlock()
	s, ok = sm.byUdpToken[id]
	return s, ok
}

func (sm *Manager) RemoveSession(id string) {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	sess, ok := sm.byUuid[id]
	if !ok {
		return
	}

	delete(sm.byUuid, id)
	delete(sm.byUdpToken, sess.udpToken)
}

// generateUdpTokenLocked generates a new UDP token or panics
func (sm *Manager) generateUdpTokenLocked() uint32 {
	var b [4]byte
	for {
		if _, err := rand.Read(b[:]); err != nil {
			panic("sessionManager: crypto/rand failed to read entropy:" + err.Error())
		}

		token := binary.BigEndian.Uint32(b[:])
		if token == 0 {
			continue
		}

		if _, exists := sm.byUdpToken[token]; !exists {
			return token
		}
	}
}
