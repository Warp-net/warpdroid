package node

import (
	"encoding/base64"
	"testing"
)

func TestInitialize(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	result := Initialize("", []string{})
	if result != "" {
		t.Fatalf("Initialize failed: %s", result)
	}

	if clientInstance == nil {
		t.Fatal("Client instance is nil after initialization")
	}

	// Cleanup
	Shutdown()
}

func TestInitializeWithPSK(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	// Create a valid 32-byte PSK
	psk := make([]byte, 32)
	for i := range psk {
		psk[i] = byte(i)
	}
	pskBase64 := base64.StdEncoding.EncodeToString(psk)

	result := Initialize(pskBase64, []string{})
	if result != "" {
		t.Fatalf("Initialize with PSK failed: %s", result)
	}

	if clientInstance == nil {
		t.Fatal("Client instance is nil after initialization with PSK")
	}

	// Cleanup
	Shutdown()
}

func TestInitializeWithInvalidPSK(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	// Invalid PSK (not 32 bytes)
	psk := make([]byte, 16)
	pskBase64 := base64.StdEncoding.EncodeToString(psk)

	result := Initialize(pskBase64, []string{})
	if result == "" {
		t.Fatal("Initialize should fail with invalid PSK length")
	}

	if !contains(result, "PSK must be exactly 32 bytes") {
		t.Fatalf("Expected PSK length error, got: %s", result)
	}
}

func TestInitializeWithInvalidBase64(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	result := Initialize("not-valid-base64!@#$", []string{})
	if result == "" {
		t.Fatal("Initialize should fail with invalid base64")
	}

	if !contains(result, "invalid PSK") {
		t.Fatalf("Expected invalid PSK error, got: %s", result)
	}
}

func TestGetClientPeerID(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	// Should return empty string when not initialized
	peerID := PeerID()
	if peerID != "" {
		t.Fatalf("Expected empty peer ID before init, got: %s", peerID)
	}

	// Initialize client
	Initialize("", []string{})

	// Should return peer ID after initialization
	peerID = PeerID()
	if peerID == "" {
		t.Fatal("Peer ID is empty after initialization")
	}

	// Cleanup
	Shutdown()
}

func TestCheckConnection(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	// Should return "false" when not initialized
	result := IsConnected()
	if result != "false" {
		t.Fatalf("Expected 'false' before init, got: %s", result)
	}

	// Initialize client
	Initialize("", []string{})

	// Should return "false" when initialized but not connected
	result = IsConnected()
	if result != "false" {
		t.Fatalf("Expected 'false' when not connected, got: %s", result)
	}

	// Cleanup
	Shutdown()
}

func TestConnectToNodeNotInitialized(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	addrInfo := "/ip4/207.154.221.44/tcp/4011/p2p/12D3KooWMKZFrp1BDKg9amtkv5zWnLhuUXN32nhqMvbtMdV2hz7j"
	result := Connect(addrInfo)
	if result == "" {
		t.Fatal("ConnectToNode should fail when not initialized")
	}

	if result != "client not initialized" {
		t.Fatalf("Expected 'client not initialized', got: %s", result)
	}
}

func TestSendRequestNotInitialized(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	result := Stream("/test/protocol", "{}")

	if result != "client not initialized" {
		t.Fatalf("Expected 'client not initialized' error, got: %v", result)
	}
}

func TestDisconnectFromNode(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	// Should not error when not initialized
	result := Disconnect()
	if result != "" {
		t.Fatalf("DisconnectFromNode should not error when not initialized, got: %s", result)
	}

	// Initialize and disconnect
	Initialize("", []string{})
	result = Disconnect()
	if result != "" {
		t.Fatalf("DisconnectFromNode failed: %s", result)
	}

	// Cleanup
	Shutdown()
}

func TestShutdown(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	// Should not error when not initialized
	result := Shutdown()
	if result != "" {
		t.Fatalf("Shutdown should not error when not initialized, got: %s", result)
	}

	// Initialize and shutdown
	Initialize("", []string{})
	result = Shutdown()
	if result != "" {
		t.Fatalf("Shutdown failed: %s", result)
	}

	// Verify client instance is nil after shutdown
	if clientInstance != nil {
		t.Fatal("Client instance should be nil after shutdown")
	}
}

func TestMultipleShutdowns(t *testing.T) {
	// Clean up before test
	clientInstance = nil

	Initialize("", []string{})

	result := Shutdown()
	if result != "" {
		t.Fatalf("First shutdown failed: %s", result)
	}

	// Second shutdown should not error
	result = Shutdown()
	if result != "" {
		t.Fatalf("Second shutdown failed: %s", result)
	}
}

// Helper function
func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(s) > len(substr) &&
		(s[:len(substr)] == substr || s[len(s)-len(substr):] == substr ||
			findSubstring(s, substr)))
}

func findSubstring(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}
