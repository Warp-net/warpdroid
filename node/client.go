package node

import (
	"bufio"
	"bytes"
	"context"
	"crypto/rand"
	"errors"
	"fmt"
	"github.com/libp2p/go-libp2p/core/pnet"
	"io"
	"sync"
	"time"

	"github.com/libp2p/go-libp2p/core/crypto"
	"github.com/libp2p/go-libp2p/core/host"
	"github.com/libp2p/go-libp2p/core/network"
	"github.com/libp2p/go-libp2p/core/peer"
	"github.com/libp2p/go-libp2p/core/peerstore"
	"github.com/libp2p/go-libp2p/core/protocol"
	"github.com/libp2p/go-libp2p/p2p/security/noise"
	"github.com/libp2p/go-libp2p/p2p/transport/tcp"
)

type clientNode struct {
	host          host.Host
	ctx           context.Context
	cancel        context.CancelFunc
	desktopPeerID peer.ID
	mu            sync.RWMutex
}

// newClient creates a new WarpNet thin client configured as per requirements
// psk: optional pre-shared key for private network (32 bytes), can be nil
func newClient(psk []byte, bootstrapNodes []string) (*clientNode, error) {
	ctx, cancel := context.WithCancel(context.Background())

	// Generate a new private key for this client instance
	// Ed25519 has a fixed key size, so -1 is used when the parameter is not applicable
	privKey, _, err := crypto.GenerateKeyPairWithReader(crypto.Ed25519, -1, rand.Reader)
	if err != nil {
		cancel()
		return nil, fmt.Errorf("failed to generate key pair: %w", err)
	}

	// Build libp2p options matching thin client requirements
	opts := []libp2p.Option{
		libp2p.PrivateNetwork(pnet.PSK(psk)),
		libp2p.Identity(privKey),                 // Client identity
		libp2p.NoListenAddrs,                     // Client-only mode - no listening
		libp2p.DisableMetrics(),                  // Lightweight
		libp2p.DisableRelay(),                    // No relay listening
		libp2p.Ping(true),                        // Enable ping for connectivity checks
		libp2p.Security(noise.ID, noise.New),     // Noise protocol for encryption
		libp2p.Transport(tcp.NewTCPTransport),    // TCP transport
		libp2p.UserAgent("warpdroid"),            // Custom user agent
		libp2p.DisableIdentifyAddressDiscovery(), // Disable address discovery (client-only)
	}

	// Create the libp2p host
	h, err := libp2p.New(opts...)
	if err != nil {
		cancel()
		return nil, fmt.Errorf("failed to create libp2p host: %w", err)
	}

	cn := &clientNode{
		host:   h,
		ctx:    ctx,
		cancel: cancel,
	}

	connectedCount := 0
	for _, addr := range bootstrapNodes {
		if err := cn.connect(addr); err != nil {
			fmt.Printf("failed to connect to bootstrap node %s: %v\n", addr, err)
			continue
		}
		connectedCount++
	}
	if connectedCount == 0 && len(bootstrapNodes) != 0 {
		return nil, fmt.Errorf("failed to connect to any bootstrap nodes")
	}
	return cn, nil
}

func (c *clientNode) connect(peerInfo string) error {
	c.mu.Lock()
	defer c.mu.Unlock()

	if peerInfo == "" {
		return fmt.Errorf("not connected to desktop node")
	}
	addrInfo, err := peer.AddrInfoFromString(peerInfo)
	if err != nil {
		return err
	}
	if addrInfo == nil {
		return fmt.Errorf("invalid peer info: %s", peerInfo)
	}
	if len(addrInfo.ID) > 52 {
		return fmt.Errorf("stream: node id is too long: %s", peerInfo)
	}
	if err := addrInfo.ID.Validate(); err != nil {
		return err
	}

	c.host.Peerstore().AddAddrs(addrInfo.ID, addrInfo.Addrs, peerstore.PermanentAddrTTL)

	ctx, cancel := context.WithTimeout(c.ctx, 30*time.Second)
	defer cancel()

	if err := c.host.Connect(ctx, *addrInfo); err != nil {
		return fmt.Errorf("connection failed: %w", err)
	}

	c.desktopPeerID = addrInfo.ID
	return nil
}

func (c *clientNode) stream(protocolID string, data []byte) ([]byte, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.desktopPeerID == "" {
		return nil, fmt.Errorf("not connected to desktop node")
	}
	desktopPeerID := c.desktopPeerID

	if protocolID == "" {
		return nil, fmt.Errorf("empty protocol ID")
	}

	ctx, cancel := context.WithTimeout(c.ctx, 30*time.Second)
	defer cancel()

	connectedness := c.host.Network().Connectedness(desktopPeerID)
	switch connectedness {
	case network.Limited:
		ctx = network.WithAllowLimitedConn(ctx, "warpnet")
	default:
	}

	stream, err := c.host.NewStream(ctx, desktopPeerID, protocol.ID(protocolID))
	if err != nil {
		return nil, fmt.Errorf("failed to open stream: %w", err)
	}

	var rw = bufio.NewReadWriter(bufio.NewReader(stream), bufio.NewWriter(stream))
	if data != nil {
		_, err = rw.Write(data)
	}
	flush(rw)
	closeWrite(stream)
	if err != nil {
		return nil, fmt.Errorf("stream: writing: %w", err)
	}

	buf := bytes.NewBuffer(nil)
	_, err = buf.ReadFrom(rw)
	if err != nil && !errors.Is(err, io.EOF) {
		return nil, fmt.Errorf(
			"stream: reading response from %s: %w", desktopPeerID.String(), err,
		)
	}

	return buf.Bytes(), nil
}

func flush(rw *bufio.ReadWriter) {
	if err := rw.Flush(); err != nil {
		fmt.Printf("stream: close write: %s", err)
	}
}

func closeWrite(s network.Stream) {
	if err := s.CloseWrite(); err != nil {
		fmt.Printf("stream: close write: %s", err)
	}
}

func (c *clientNode) getPeerID() string {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.host.ID().String()
}

// IsConnected checks if connected to the desktop node
func (c *clientNode) isConnected() bool {
	c.mu.RLock()
	defer c.mu.RUnlock()

	if c.desktopPeerID == "" {
		return false
	}

	connectedness := c.host.Network().Connectedness(c.desktopPeerID)
	return connectedness == network.Connected || connectedness == network.Limited
}

func (c *clientNode) disconnect() error {
	c.mu.Lock()
	defer c.mu.Unlock()

	if c.desktopPeerID != "" {
		if err := c.host.Network().ClosePeer(c.desktopPeerID); err != nil {
			return fmt.Errorf("failed to close peer connection: %w", err)
		}
		c.host.Peerstore().RemovePeer(c.desktopPeerID)
		c.desktopPeerID = ""
	}

	return nil
}

func (c *clientNode) close() error {
	defer func() { recover() }()
	c.cancel()
	return c.host.Close()
}
