/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package site.warpnet.transport

/**
 * Initialisation parameters for the Warpnet native client.
 *
 * [pskBase64] is the shared-key used to scope the libp2p private network.
 * Both desktop and mobile peers must hold the same 32-byte key. In the paired
 * model, the desktop node hands this key to the phone during pairing and the
 * phone stores it in [androidx.security.crypto.EncryptedSharedPreferences].
 *
 * [bootstrapAddrs] is a list of libp2p multiaddr strings. Each must include
 * the peer ID segment (`/p2p/<peerid>`) so the TLS/Noise handshake can verify
 * identity. The binding connects to at least one of these during
 * [WarpnetClient.initialise] and fails if none reach.
 *
 * [desktopPeerAddr] is the full multiaddr of the paired desktop node. The
 * transport calls `Connect()` with this value after init so every request
 * flows through one specific peer.
 */
data class WarpnetConfig(
    val pskBase64: String,
    val bootstrapAddrs: List<String>,
    val desktopPeerAddr: String,
)
