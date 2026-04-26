/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package site.warpnet.transport.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire types for the QR pairing handshake. Field names mirror
 * `warpdroid/node/qr.go` and `warpnet/domain/warpnet.go` / `warpnet.NodeInfo`
 * byte-for-byte so a round-trip back to the fat node reproduces whatever
 * the node originally embedded in the QR.
 *
 * `network` is present on the upstream NodeInfo but not on
 * `warpdroid/node/qr.go`; it is required for the private-network join, so
 * it is included here with the same `network` JSON name as upstream.
 */

@JsonClass(generateAdapter = true)
data class AuthNodeInfo(
    @Json(name = "token") val token: String,
    @Json(name = "psk") val psk: String,
    @Json(name = "network") val network: String = "",
    @Json(name = "addresses") val addresses: List<String> = emptyList(),
    @Json(name = "node_id") val nodeId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "bootstrap_peers") val bootstrapPeers: List<AddrInfo> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AddrInfo(
    val id: String,
    val addrs: List<String> = emptyList(),
)
