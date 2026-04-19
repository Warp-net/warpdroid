package node

import (
	"time"
)

type AuthNodeInfo struct {
	Identity Identity `json:"identity"`
	NodeInfo NodeInfo `json:"node_info"`
}

type Identity struct {
	Owner Owner  `json:"owner"`
	Token string `json:"token"`
	PSK   string `json:"psk"`
}

type Owner struct {
	CreatedAt       time.Time `json:"created_at"`
	NodeId          string    `json:"node_id"`
	UserId          string    `json:"user_id"`
	RedundantUserID string    `json:"id"`
	Username        string    `json:"username"`
}

type NodeInfo struct {
	OwnerId        string     `json:"owner_id"`
	ID             string     `json:"node_id"`
	Version        *string    `json:"version"`
	Addresses      []string   `json:"addresses"`
	StartTime      time.Time  `json:"start_time"`
	RelayState     string     `json:"relay_state"`
	BootstrapPeers []AddrInfo `json:"bootstrap_peers"`
	Reachability   int        `json:"reachability"`
	Protocols      []string   `json:"protocols"`
	Hash           string     `json:"hash"`
}

type AddrInfo struct {
	ID    string   `json:"id"`
	Addrs []string `json:"addrs"`
}
