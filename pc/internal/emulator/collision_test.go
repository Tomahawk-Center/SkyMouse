package emulator

import (
	"slices"
	"testing"
)

func TestDetectBorderHit(t *testing.T) {
	e := &Emulator{
		displaysBounds: []screenBounds{
			{
				x: 0,
				y: 0,
				w: 1920,
				h: 1080,
			},
		},
	}

	tests := []struct {
		name       string
		x, y       int
		newX, newY int
		want       []borderSide
	}{
		{
			name: "no border hit",
			x:    100, y: 100,
			newX: 200, newY: 200,
			want: []borderSide{},
		},
		{
			name: "left border",
			x:    100, y: 100,
			newX: -1, newY: 100,
			want: []borderSide{leftSide},
		},
		{
			name: "right border",
			x:    100, y: 100,
			newX: 1920, newY: 100,
			want: []borderSide{rightSide},
		},
		{
			name: "top border",
			x:    100, y: 100,
			newX: 100, newY: -1,
			want: []borderSide{topSide},
		},
		{
			name: "bottom border",
			x:    100, y: 100,
			newX: 100, newY: 1080,
			want: []borderSide{bottomSide},
		},
		{
			name: "top left corner",
			x:    100, y: 100,
			newX: -1, newY: -1,
			want: []borderSide{leftSide, topSide},
		},
		{
			name: "top right corner",
			x:    100, y: 100,
			newX: 1920, newY: -1,
			want: []borderSide{rightSide, topSide},
		},
		{
			name: "bottom right corner",
			x:    100, y: 100,
			newX: 1920, newY: 1080,
			want: []borderSide{rightSide, bottomSide},
		},
		{
			name: "bottom left corner",
			x:    100, y: 100,
			newX: -1, newY: 1080,
			want: []borderSide{leftSide, bottomSide},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := e.detectBorderHit(
				tt.x, tt.y,
				tt.newX, tt.newY,
			)

			if !slices.Equal(got, tt.want) {
				t.Errorf("detectBorderHit() = %v, want %v", got, tt.want)
			}
		})
	}
}
