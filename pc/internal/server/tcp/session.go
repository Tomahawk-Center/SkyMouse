package tcp

import (
	"net"
	"time"
)

type clientSession struct {
	conn        net.Conn
	isHandshake bool
	lastActive  time.Time
}
