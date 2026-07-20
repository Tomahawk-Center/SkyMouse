package session

import "sync/atomic"

type UdpState struct {
	lastSeqId atomic.Uint32
}

func (t *UdpState) VerifyAndSetNewSequenceId(newSeqId uint32) (swapped bool) {
	for {
		curr := t.lastSeqId.Load()

		if int32(newSeqId-curr) <= 0 { // RFC 1982
			return false
		}
		if t.lastSeqId.CompareAndSwap(curr, newSeqId) {
			return true
		}
	}
}
