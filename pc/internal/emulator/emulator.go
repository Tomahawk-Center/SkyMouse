package emulator

import (
	"log"
	"time"

	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"github.com/go-vgo/robotgo"
)

type Emulator struct {
	eventsChan     chan *protoapi.ServerEvent
	displaysBounds []screenBounds
	isBorderHit    bool
}

func NewEmulator(ch chan *protoapi.ServerEvent) *Emulator {
	var d []screenBounds
	for i := range robotgo.DisplaysNum() {
		x, y, w, h := robotgo.GetDisplayBounds(i)
		d = append(d, screenBounds{x, y, w, h})
	}

	return &Emulator{
		eventsChan:     ch,
		displaysBounds: d,
	}
}

// getDisplayIndex returns the index of the display where the cursor is located;
// if the cursor is outside the bounds of all displays, it returns -1
func (e *Emulator) getDisplayIndex(x, y int) int {
	for i, d := range e.displaysBounds {
		sx, sy, sw, sh := d.x, d.y, d.w, d.h
		if x >= sx && x < sx+sw && y >= sy && y < sy+sh {
			return i
		}
	}
	return -1
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
	if ev.DeltaX == 0 && ev.DeltaY == 0 {
		return
	}

	x, y := robotgo.Location()

	newX := x + int(ev.DeltaX)
	newY := y + int(ev.DeltaY)

	iOld := e.getDisplayIndex(x, y)
	iNew := e.getDisplayIndex(newX, newY)

	if iNew == -1 {
		if !e.isBorderHit {
			select {
			case e.eventsChan <- &protoapi.ServerEvent{
				Type:        protoapi.HapticEventType_EVENT_EDGE_HIT,
				TimestampMs: time.Now().UnixMilli(),
			}:
			default:
			}
		}

		e.isBorderHit = true
	} else if iOld != iNew {
		select {
		case e.eventsChan <- &protoapi.ServerEvent{
			Type:        protoapi.HapticEventType_EVENT_BORDER_CROSSING,
			TimestampMs: time.Now().UnixMilli(),
		}:
		default:
		}
	} else {
		e.isBorderHit = false
	}

	robotgo.Move(newX, newY)
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
	delta := int(ev.DeltaY)
	log.Println("DeltaY:", ev.DeltaY)

	switch {
	case ev.DeltaY > 0:
		robotgo.ScrollDir(delta, "up")
	case ev.DeltaY < 0:
		robotgo.ScrollDir(delta*-1, "down")
	default:
		return
	}
}
