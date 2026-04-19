/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package site.warpnet.transport

import java.io.File
import java.security.KeyPairGenerator

/**
 * Loads or generates the 64-byte libp2p Ed25519 private key that the AAR's
 * `Initialize` method consumes. The key is `seed(32) || publicKey(32)`, as
 * expected by `crypto.UnmarshalEd25519PrivateKey` in go-libp2p.
 *
 * Persistence lives under the app's private files directory; there is no
 * encryption at rest because the key is already the user's long-term libp2p
 * identity and the file lives in app-private storage. If a pairing flow
 * later demands hardware-backed protection, move the read/write calls onto
 * EncryptedFile without changing the public signature here.
 */
class Ed25519IdentityStore(private val keyFile: File) {

    /** Read from disk or generate on first use. Returns 64 raw bytes. */
    fun loadOrCreate(): ByteArray {
        existing()?.let { return it }
        val fresh = generate()
        keyFile.parentFile?.mkdirs()
        keyFile.writeBytes(fresh)
        return fresh
    }

    /** Same as [loadOrCreate] but hex-encoded (lowercase), ready for the AAR. */
    @OptIn(ExperimentalStdlibApi::class)
    fun loadOrCreateHex(): String = loadOrCreate().toHexString()

    private fun existing(): ByteArray? {
        if (!keyFile.exists()) return null
        val bytes = keyFile.readBytes()
        return if (bytes.size == KEY_SIZE) bytes else null
    }

    private fun generate(): ByteArray {
        // Conscrypt is installed at security-provider priority 1 by
        // TuskyApplication, which gives us a working Ed25519 KeyPairGenerator
        // on every supported API level (min SDK 24). The encoded forms are
        // standard RFC 8410 PKCS#8 / X.509; the last 32 bytes of each
        // encoding are the raw seed and public key respectively.
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val pkcs8 = kp.private.encoded
        val spki = kp.public.encoded
        require(pkcs8.size >= SEED_SIZE && spki.size >= PUB_SIZE) {
            "Unexpected Ed25519 encoding lengths (pkcs8=${pkcs8.size}, spki=${spki.size})"
        }
        val seed = pkcs8.copyOfRange(pkcs8.size - SEED_SIZE, pkcs8.size)
        val pub = spki.copyOfRange(spki.size - PUB_SIZE, spki.size)
        return seed + pub
    }

    private companion object {
        const val SEED_SIZE = 32
        const val PUB_SIZE = 32
        const val KEY_SIZE = SEED_SIZE + PUB_SIZE
    }
}
