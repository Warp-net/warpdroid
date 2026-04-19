package node

import (
	"encoding/base64"
	"fmt"
	"strings"
)

// Mobile-friendly wrapper types for gomobile compatibility
// gomobile bind has limitations on complex types

var clientInstance *clientNode

// Method creates a new WarpNet client with optional PSK
// pskBase64: base64-encoded PSK (32 bytes), empty string for no PSK
// Returns error message or empty string on success
func Initialize(pskBase64 string, bootstrapNodes string) string {
	var (
		psk []byte
		err error
	)

	if clientInstance != nil {
		return fmt.Sprintf("already initialized")
	}

	if pskBase64 != "" {
		psk, err = base64.StdEncoding.DecodeString(pskBase64)
		if err != nil {
			return fmt.Sprintf("invalid PSK: %v", err)
		}
		if len(psk) != 32 {
			return "PSK must be exactly 32 bytes"
		}
	}

	client, err := newClient(psk, strings.Split(bootstrapNodes, ","))
	if err != nil {
		return fmt.Sprintf("failed to create client: %v", err)
	}

	clientInstance = client
	return ""
}

func Connect(addrInfo string) string {
	if clientInstance == nil {
		return "client not initialized"
	}

	err := clientInstance.connect(addrInfo)
	if err != nil {
		return fmt.Sprintf("connection failed: %v", err)
	}

	return ""
}

func Stream(protocolID string, data string) string {
	if clientInstance == nil {
		return "client not initialized"
	}

	response, err := clientInstance.stream(protocolID, []byte(data))
	if err != nil {
		return err.Error()
	}

	return string(response)
}

func PeerID() string {
	if clientInstance == nil {
		return ""
	}
	return clientInstance.getPeerID()
}

func IsConnected() string {
	if clientInstance == nil {
		return "false"
	}
	if clientInstance.isConnected() {
		return "true"
	}
	return "false"
}

func Disconnect() string {
	if clientInstance == nil {
		return ""
	}

	err := clientInstance.disconnect()
	if err != nil {
		return fmt.Sprintf("disconnect failed: %v", err)
	}

	return ""
}

func Shutdown() string {
	if clientInstance == nil {
		return ""
	}

	err := clientInstance.close()
	if err != nil {
		return fmt.Sprintf("shutdown failed: %v", err)
	}

	clientInstance = nil
	return ""
}
