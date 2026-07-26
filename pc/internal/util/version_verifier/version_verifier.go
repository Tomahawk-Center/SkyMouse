package version_verifier

import (
	"fmt"
	"log"
	"strconv"
	"strings"
)

// VerifyClientVersion expects client and server version strings in format "X.Y", where X and Y are integers.
//
// X is the major version and Y is the minor version.
// Returns nil if the major versions are the same.
func VerifyClientVersion(clientV, serverV string) error {
	cMajor, cMinor, err := parseVersion(clientV)
	if err != nil {
		return fmt.Errorf("client version parse failed: %w", err)
	}
	sMajor, sMinor, err := parseVersion(serverV)
	if err != nil {
		return fmt.Errorf("server version parse failed: %w", err)
	}

	if cMajor != sMajor {
		return fmt.Errorf("major version mismatch: client %d, server %d", cMajor, sMajor)
	}

	switch {
	case sMinor > cMinor:
		log.Println("Warning: server minor version is higher than client version")
	case sMinor < cMinor:
		log.Println("Warning: server minor version is lower than client version")
	}

	return nil
}

func parseVersion(version string) (major int, minor int, err error) {
	parts := strings.Split(version, ".")
	if len(parts) != 2 {
		return 0, 0, fmt.Errorf("invalid version format %q: expected major.minor", version)
	}
	major, err = strconv.Atoi(parts[0])
	if err != nil {
		return 0, 0, fmt.Errorf("invalid major version %q: %w", parts[0], err)
	}
	minor, err = strconv.Atoi(parts[1])
	if err != nil {
		return 0, 0, fmt.Errorf("invalid minor version %q: %w", parts[1], err)
	}
	return major, minor, nil
}
