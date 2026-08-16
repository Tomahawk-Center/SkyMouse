package commands

import (
	"fmt"
	"os/exec"
	"sync"
	"syscall"

	"golang.design/x/clipboard"
	"golang.org/x/sys/windows/registry"
)

func Shutdown() error {
	args := []string{"/s", "/t", "0"}
	cmd := exec.Command("shutdown", args...)
	return cmd.Run()
}

var (
	powrprof        = syscall.NewLazyDLL("powrprof.dll")
	setSuspendState = powrprof.NewProc("SetSuspendState")
)

func Sleep() error {
	ret, _, err := setSuspendState.Call(
		uintptr(0),
		uintptr(0),
		uintptr(0),
	)

	if ret == 0 {
		return fmt.Errorf("failed to set suspend state: %w", err)
	}
	return nil
}

func LockScreen() error {
	cmd := exec.Command("rundll32.exe", "user32.dll,LockWorkStation")
	return cmd.Run()
}

var initClipboard = sync.OnceValue(clipboard.Init)

func WriteToClipboard(text string) error {
	err := initClipboard()
	if err != nil {
		return err
	}

	clipboard.Write(clipboard.FmtText, []byte(text))
	return nil // TODO add err handling on Write call
}

const (
	spiSetCursorSize  = 0x2029
	spiFUpdateIniFile = 0x01
	spiFSendChange    = 0x02
)

var (
	user32               = syscall.NewLazyDLL("user32.dll")
	systemParametersInfo = user32.NewProc("SystemParametersInfoW")
)

func SetCursorSize(size uint32) error {
	if size < 1 || size > 15 {
		return fmt.Errorf("size must be in 1-15 range")
	}

	accKey, err := registry.OpenKey(
		registry.CURRENT_USER,
		`Software\Microsoft\Accessibility`,
		registry.SET_VALUE,
	)
	if err != nil {
		return fmt.Errorf("failed to open accessibility reg key: %w", err)
	}
	defer func(accKey registry.Key) {
		_ = accKey.Close()
	}(accKey)

	if err := accKey.SetDWordValue("CursorSize", size); err != nil {
		return fmt.Errorf("failed to set CursorSize: %w", err)
	}

	pixelSize := 32 + (size-1)*16

	ret, _, callErr := systemParametersInfo.Call(
		uintptr(spiSetCursorSize),
		0,
		uintptr(pixelSize), // Pass pixel size in pvParam
		uintptr(spiFUpdateIniFile|spiFSendChange),
	)

	if ret == 0 {
		return fmt.Errorf("SystemParametersInfoW failed: %w", callErr)
	}

	return nil
}

func CursorSize() (int, error) {
	accKey, err := registry.OpenKey(
		registry.CURRENT_USER,
		`Software\Microsoft\Accessibility`,
		registry.QUERY_VALUE,
	)
	if err != nil {
		return 0, fmt.Errorf("failed to open accessibility reg key: %w", err)
	}
	defer func(accKey registry.Key) {
		_ = accKey.Close()
	}(accKey)

	val, _, err := accKey.GetIntegerValue("CursorSize")
	if err != nil {
		return 0, fmt.Errorf("failed to get CursorSize: %w", err)
	}
	return int(val), nil
}
