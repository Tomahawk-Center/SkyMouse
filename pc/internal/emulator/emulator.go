package emulator

import (
	"fmt"
	"log"
	"time"

	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"github.com/go-vgo/robotgo"
)

type Event struct {
	SessionId string
	Data      *protoapi.ServerEvent
}

type Emulator struct {
	eventsChan        chan Event
	displaysBounds    []screenBounds
	displaysSafeZones []screenBounds
	isBorderHit       bool
}

func calculateBoundsWithPadding(bounds screenBounds, padding int) (screenBounds, error) {
	if padding < 0 {
		return screenBounds{}, fmt.Errorf("padding must be non-negative")
	}
	bounds.x += padding
	bounds.y += padding
	bounds.w -= padding * 2
	bounds.h -= padding * 2
	return bounds, nil
}

func NewEmulator(ch chan Event) (*Emulator, error) {
	var d []screenBounds
	var dS []screenBounds
	for i := range robotgo.DisplaysNum() {
		x, y, w, h := robotgo.GetDisplayBounds(i)
		b := screenBounds{x, y, w, h}
		d = append(d, b)

		bS, err := calculateBoundsWithPadding(b, 4)
		if err != nil {
			return nil, err
		}
		dS = append(dS, bS)
		// best variables naming ever...
	}

	return &Emulator{
		eventsChan:        ch,
		displaysBounds:    d,
		displaysSafeZones: dS,
	}, nil
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

func (e *Emulator) isInSafeZone(displayIndex, x, y int) bool {
	bS := e.displaysSafeZones[displayIndex]
	return x >= bS.x && x < bS.x+bS.w && y >= bS.y && y < bS.y+bS.h
}

func (e *Emulator) Handle(sessionId string, event *protoapi.EmulatorEvent) {

	switch ev := event.Event.(type) {
	case *protoapi.EmulatorEvent_Mouse:
		e.handleMouse(sessionId, ev.Mouse)
	case *protoapi.EmulatorEvent_Click:
		e.handleClick(ev.Click)
	case *protoapi.EmulatorEvent_Scroll:
		e.handleScroll(ev.Scroll)
	}
}

func (e *Emulator) handleMouse(sessionId string, ev *protoapi.MouseEvent) {
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
			ev := Event{SessionId: sessionId, Data: &protoapi.ServerEvent{
				Type:        protoapi.HapticEventType_EVENT_EDGE_HIT,
				TimestampMs: time.Now().UnixMilli(),
			}}

			select {
			case e.eventsChan <- ev:
			default:
			}
		}

		e.isBorderHit = true

	} else if iOld != iNew {
		ev := Event{SessionId: sessionId, Data: &protoapi.ServerEvent{
			Type:        protoapi.HapticEventType_EVENT_BORDER_CROSSING,
			TimestampMs: time.Now().UnixMilli(),
		}}
		select {
		case e.eventsChan <- ev:
		default:
		}
	} else {

		if e.isInSafeZone(iNew, x, y) {
			e.isBorderHit = false
		}

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

func (e *Emulator) HandleKeyboardString(ev *protoapi.KeyboardStringEvent) {
	if ev == nil || ev.Text == "" {
		return
	}
	robotgo.Type(ev.Text)
}

func (e *Emulator) HandleKeyboardTap(ev *protoapi.KeyboardTapEvent) {
	if ev == nil {
		return
	}

	var key string
	switch ev.Key {
	case protoapi.KeyboardKey_KEY_ENTER:
		key = "enter"
	case protoapi.KeyboardKey_KEY_BACKSPACE:
		key = "backspace"
	default:
		return
	}

	switch ev.State {
	case protoapi.ButtonState_STATE_DOWN:
		_ = robotgo.KeyDown(key)
	case protoapi.ButtonState_STATE_UP:
		_ = robotgo.KeyUp(key)
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
