package emulator

import (
	"fmt"
	"log"
	"time"

	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
	"github.com/go-vgo/robotgo"
	"google.golang.org/protobuf/proto"
)

type TransportType string

const (
	TCP TransportType = "tcp"
	UDP TransportType = "udp"
)

type Event struct {
	SessionId string
	Transport TransportType
	Data      proto.Message
}

type Emulator struct {
	eventsChan        chan Event
	displaysBounds    []screenBounds
	displaysSafeZones []screenBounds
	borderHits        [4]bool
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
	case *protoapi.EmulatorEvent_KeyboardStringEvent:
		e.handleKeyboardString(ev.KeyboardStringEvent)
	case *protoapi.EmulatorEvent_KeyboardTapEvent:
		e.handleKeyboardTap(sessionId, ev.KeyboardTapEvent)
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
		newHits := e.detectBorderHit(x, y, newX, newY)
		newBorderHits := e.borderHits

		for _, s := range newHits {
			newBorderHits[s] = true
		}

		var needHaptic bool

		for i, c := range e.borderHits {
			if c == false && newBorderHits[i] == true {
				needHaptic = true
				break
			}
		}

		if needHaptic {
			ev := Event{SessionId: sessionId, Transport: UDP, Data: &protoapi.ServerEvent{
				Type:        protoapi.HapticEventType_EVENT_EDGE_HIT,
				TimestampMs: time.Now().UnixMilli(),
			}}

			select {
			case e.eventsChan <- ev:
			default:
			}
		}

		e.borderHits = newBorderHits

	} else if iOld != iNew {
		ev := Event{SessionId: sessionId, Transport: UDP, Data: &protoapi.ServerEvent{
			Type:        protoapi.HapticEventType_EVENT_BORDER_CROSSING,
			TimestampMs: time.Now().UnixMilli(),
		}}
		select {
		case e.eventsChan <- ev:
		default:
		}
	} else {

		for i := range e.borderHits {
			if e.isInSafeZoneSide(iNew, x, y, borderSide(i)) {
				e.borderHits[i] = false
			}
		}

	}

	_ = robotgo.Move(newX, newY)
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
		_ = robotgo.ScrollDir(delta, "up")
	case ev.DeltaY < 0:
		_ = robotgo.ScrollDir(delta*-1, "down")
	default:
		return
	}
}

func (e *Emulator) handleKeyboardString(ev *protoapi.KeyboardStringEvent) {
	text := ev.GetText()
	if text != "" {
		robotgo.Type(ev.Text)
	}
}

func (e *Emulator) handleKeyboardTap(sessionId string, ev *protoapi.KeyboardTapEvent) {
	if ev == nil {
		return
	}

	var needSendVolumeAnswer bool
	var key string
	switch ev.Key {
	case protoapi.KeyboardKey_KEY_ENTER:
		key = robotgo.Enter
	case protoapi.KeyboardKey_KEY_BACKSPACE:
		key = robotgo.Backspace
	case protoapi.KeyboardKey_KEY_VOL_DOWN:
		key = robotgo.AudioVolDown
		needSendVolumeAnswer = true
	case protoapi.KeyboardKey_KEY_VOL_UP:
		key = robotgo.AudioVolUp
		needSendVolumeAnswer = true
	case protoapi.KeyboardKey_KEY_VOL_MUTE:
		key = robotgo.AudioMute
		needSendVolumeAnswer = true
	case protoapi.KeyboardKey_KEY_AUDIO_PAUSE:
		key = robotgo.AudioPause
	case protoapi.KeyboardKey_KEY_AUDIO_NEXT:
		key = robotgo.AudioNext
	case protoapi.KeyboardKey_KEY_AUDIO_PREV:
		key = robotgo.AudioPrev
	default:
		return
	}

	var err error
	switch ev.State {
	case protoapi.ButtonState_STATE_DOWN:
		err = robotgo.KeyDown(key)
	case protoapi.ButtonState_STATE_UP:
		err = robotgo.KeyUp(key)
	}
	if err != nil {
		log.Printf("Keyboard click err: %v\n", err)
	}

	if needSendVolumeAnswer {
		ev := Event{SessionId: sessionId, Transport: TCP, Data: &protoapi.CurrentVolumeAnswer{
			Volume:  0,
			IsMuted: false,
		}}
		select {
		case e.eventsChan <- ev:
		default:
		}
	}
}
