/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.keylesspalace.tusky.components.pairing

import com.squareup.moshi.JsonClass
import site.warpnet.transport.dto.AuthNodeInfo
import site.warpnet.transport.dto.Identity

/**
 * Persisted shape for a successful pairing. Keeps the full [Identity] so the
 * node can re-assert the paired token, plus the pinned peer ID and addresses
 * required to reconnect without re-scanning. The pin is the peer ID the user
 * confirmed on screen; on every reconnect we verify the libp2p handshake
 * produced that same ID before trusting the connection.
 */
@JsonClass(generateAdapter = true)
data class PairedNode(
    val identity: Identity,
    val pinnedPeerId: String,
    val addresses: List<String>,
    val network: String,
    val bootstrapAddrs: List<String>,
) {
    companion object {
        fun from(info: AuthNodeInfo): PairedNode = PairedNode(
            identity = info.identity,
            pinnedPeerId = info.nodeInfo.id,
            addresses = info.nodeInfo.addresses,
            network = info.nodeInfo.network,
            bootstrapAddrs = info.nodeInfo.bootstrapPeers.flatMap { peer ->
                peer.addrs.map { "$it/p2p/${peer.id}" }
            },
        )
    }
}
