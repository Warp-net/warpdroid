/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package site.warpnet.transport

import com.squareup.moshi.Moshi

/**
 * Hilt module lives in the `app` module so it can bind app-scoped types. This
 * factory exists to keep knowledge of transport wiring out of the app code —
 * the module only has to know "call [createClient]" with a [Moshi] it owns.
 */
object WarpnetTransport {
    fun createClient(moshi: Moshi = Moshi.Builder().build()): WarpnetClient =
        WarpnetClient(moshi = moshi, signer = NoOpSigner())
}
