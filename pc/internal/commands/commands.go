package commands

import (
	"fmt"
	"os/exec"
	"syscall"
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
