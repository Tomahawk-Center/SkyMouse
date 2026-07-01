package emulator

import (
	"log"

	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"github.com/go-vgo/robotgo"
)

type Emulator struct{}

func NewEmulator() *Emulator {
	return &Emulator{}
}

func (e *Emulator) Handle(event *protoapi.MessageToServer) {

	switch ev := event.Event.(type) {
	case *protoapi.MessageToServer_Mouse:
		e.handleMouse(ev.Mouse)
	case *protoapi.MessageToServer_Click:
		e.handleClick(ev.Click)
	case *protoapi.MessageToServer_Scroll:
		e.handleScroll(ev.Scroll)
	}
}

func (e *Emulator) handleMouse(ev *protoapi.MouseEvent) {
	// TODO
}

func (e *Emulator) handleClick(ev *protoapi.ClickEvent) {
	var btn string
	switch ev.Button {
	case protoapi.MouseButton_BUTTON_LEFT:
		btn = "left"
	case protoapi.MouseButton_BUTTON_RIGHT:
		btn = "right"
	case protoapi.MouseButton_BUTTON_MIDDLE:
		btn = "center"
	default:
		return
	}

	var err error
	switch ev.State {
	case protoapi.ButtonState_STATE_DOWN:
		err = robotgo.MouseDown(btn)
	case protoapi.ButtonState_STATE_UP:
		err = robotgo.MouseUp(btn)
	}

	if err != nil {
		log.Printf("Mouse click err: %v\n", err)
	}
}

func (e *Emulator) handleScroll(ev *protoapi.ScrollEvent) {
	// TODO
}
