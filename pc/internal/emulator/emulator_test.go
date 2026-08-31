package emulator

import (
	"testing"

	"github.com/Tomahawk-Center/SkyMouse/pc/pkg/protoapi"
)

func TestEmulator_Handle(t *testing.T) {
	tests := []struct {
		name  string
		event *protoapi.EmulatorEvent
	}{
		{
			name:  "nil EmulatorEvent",
			event: &protoapi.EmulatorEvent{},
		},
		{
			name:  "nil EmulatorEvent.Event",
			event: &protoapi.EmulatorEvent{Event: nil},
		},
		{
			name: "empty MouseEvent",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_Mouse{
					Mouse: &protoapi.MouseEvent{},
				},
			},
		},
		{
			name: "empty ClickEvent",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_Click{
					Click: &protoapi.ClickEvent{},
				},
			},
		},
		{
			name: "empty ScrollEvent",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_Scroll{
					Scroll: &protoapi.ScrollEvent{},
				},
			},
		},
		{
			name: "empty KeyboardStringEvent",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_KeyboardStringEvent{
					KeyboardStringEvent: &protoapi.KeyboardStringEvent{},
				},
			},
		},
		{
			name: "nil KeyboardTapEvent",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_KeyboardTapEvent{
					KeyboardTapEvent: nil,
				},
			},
		},
		{
			name: "empty KeyboardTapEvent",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_KeyboardTapEvent{
					KeyboardTapEvent: &protoapi.KeyboardTapEvent{},
				},
			},
		},
		{
			name: "ClickEvent with Button set but missing State (unspecified/zero)",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_Click{
					Click: &protoapi.ClickEvent{
						Button: protoapi.MouseButton_BUTTON_LEFT,
					},
				},
			},
		},
		{
			name: "ClickEvent with State set but missing Button",
			event: &protoapi.EmulatorEvent{
				Event: &protoapi.EmulatorEvent_Click{
					Click: &protoapi.ClickEvent{
						State: protoapi.ButtonState_STATE_DOWN,
					},
				},
			},
		},
	}

	ch := make(chan Event, 10)

	emu, err := NewEmulator(ch)
	if err != nil {
		t.Fatal(err)
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			defer func() {
				if r := recover(); r != nil {
					t.Fatalf("Handle() panicked on test case '%s': %v", tt.name, r)
				}
			}()

			emu.Handle("test_session_id", tt.event)
		})
	}
}
