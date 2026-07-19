package server

import "github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"

type EventHandler interface {
	Handle(sessionId string, event *protoapi.EmulatorEvent)
}
