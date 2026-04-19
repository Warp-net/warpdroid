package node

import (
	"testing"
)

func TestNewClient(t *testing.T) {
	client, err := newClient(nil, []string{})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}
	defer client.close()

	if client.host == nil {
		t.Fatal("Host is nil")
	}

	if client.ctx == nil {
		t.Fatal("Context is nil")
	}
}

func TestNewClientWithPSK(t *testing.T) {
	// Valid 32-byte PSK
	psk := make([]byte, 32)
	for i := range psk {
		psk[i] = byte(i)
	}

	client, err := newClient(psk, []string{})
	if err != nil {
		t.Fatalf("failed to create client with PSK: %v", err)
	}
	defer client.close()

	if client.host == nil {
		t.Fatal("Host is nil")
	}
}

func TestGetPeerID(t *testing.T) {
	client, err := newClient(nil, []string{})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}
	defer client.close()

	peerID := client.getPeerID()
	if peerID == "" {
		t.Fatal("Peer ID is empty")
	}

	// Peer ID should start with "12D3KooW" for Ed25519 keys
	if len(peerID) < 8 {
		t.Fatalf("Peer ID too short: %s", peerID)
	}
}

func TestIsConnectedBeforeConnection(t *testing.T) {
	client, err := newClient(nil, []string{})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}
	defer client.close()

	if client.isConnected() {
		t.Fatal("Client should not be connected initially")
	}
}

func TestDisconnectBeforeConnection(t *testing.T) {
	client, err := newClient(nil, []string{})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}
	defer client.close()

	err = client.disconnect()
	if err != nil {
		t.Fatalf("Disconnect should not error when not connected: %v", err)
	}
}

func TestSendMessageBeforeConnection(t *testing.T) {
	client, err := newClient(nil, []string{})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}
	defer client.close()

	_, err = client.stream("/test/protocol", []byte("test"))
	if err == nil {
		t.Fatal("SendMessage should error when not connected")
	}

	expectedError := "not connected to desktop node"
	if err.Error() != expectedError {
		t.Fatalf("Expected error '%s', got '%s'", expectedError, err.Error())
	}
}

func TestClose(t *testing.T) {
	client, err := newClient(nil, []string{})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}

	err = client.close()
	if err != nil {
		t.Fatalf("Close failed: %v", err)
	}

	// Calling Close again should not panic
	err = client.close()
	if err != nil {
		t.Fatalf("Second Close failed: %v", err)
	}
}
