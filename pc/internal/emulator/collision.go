package emulator

type borderSide uint8

const (
	leftSide borderSide = iota
	rightSide
	topSide
	bottomSide
)

type corner uint8

const (
	leftTopCorner corner = iota
	rightTopCorner
	rightBottomCorner
	leftBottomCorner
)

func (e *Emulator) isInSafeZoneSide(displayIndex, x, y int, side borderSide) bool {
	dsz := e.displaysSafeZones[displayIndex]
	switch side {
	case leftSide:
		return x >= dsz.x
	case rightSide:
		return x < dsz.x+dsz.w
	case topSide:
		return y >= dsz.y
	case bottomSide:
		return y < dsz.y+dsz.h
	default:
		panic("invalid side")
	}
}

func (e *Emulator) detectBorderHit(x, y, newX, newY int) []borderSide {
	var sides []borderSide
	iOld := e.getDisplayIndex(x, y)
	iNew := e.getDisplayIndex(newX, newY)
	if iNew != -1 && iNew != iOld {
		return sides
	}

	db := e.displaysBounds[iOld]

	if newX < db.x {
		sides = append(sides, leftSide)
	} else if newX >= db.x+db.w {
		sides = append(sides, rightSide)
	}

	if newY < db.y {
		sides = append(sides, topSide)
	} else if newY >= db.y+db.h {
		sides = append(sides, bottomSide)
	}

	return sides
}
