/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package site.warpnet.transport

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Coroutine-friendly wrapper around the `node.Node` gomobile binding.
 *
 * Lifecycle:
 * 1. [initialise] — start the libp2p host with the shared PSK and bootstrap
 *    peers. Idempotent.
 * 2. [connect] — open a connection to the paired desktop multiaddr. Cheap;
 *    the binding keeps a single connection and [request] rides on it.
 * 3. [request] — open one stream per call (`open → write → closeWrite →
 *    read-to-EOF`), as documented in [ProtocolIds] and
 *    `docs/warpnet-protocol.md`.
 * 4. [shutdown] — close the host. After this the instance must be discarded;
 *    the gomobile side uses a package-level `clientInstance` singleton and
 *    will refuse re-initialisation until `Shutdown` clears it.
 *
 * The binding is globally singleton on the Go side (mobile.go:11), so this
 * class also serialises access through a mutex. Do not construct more than
 * one [WarpnetClient] per process.
 */
@OptIn(ExperimentalStdlibApi::class)
class WarpnetClient(
    private val moshi: Moshi,
    private val signer: EnvelopeSigner,
    private val binding: WarpnetBinding = DefaultBinding,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Uninitialised)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val envelopeAdapter = moshi.adapter<WarpnetEnvelope>()
    private val errorAdapter = moshi.adapter<WarpnetResponseError>()

    /** Start the libp2p host. No-op if already initialised. */
    suspend fun initialise(config: WarpnetConfig) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_state.value != ConnectionState.Uninitialised) return@withLock
            val err = binding.initialize(config.pskBase64, config.bootstrapAddrs.toTypedArray())
            if (err.isNotEmpty()) {
                _state.value = ConnectionState.Failed(WarpnetException.TransportFailure(err))
                return@withLock
            }
            _state.value = ConnectionState.Disconnected
        }
    }

    /** Dial the paired desktop peer. */
    suspend fun connect(desktopPeerAddr: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_state.value == ConnectionState.Uninitialised) {
                throw WarpnetException.NotInitialised()
            }
            _state.value = ConnectionState.Connecting
            val err = binding.connect(desktopPeerAddr)
            if (err.isNotEmpty()) {
                val failure = WarpnetException.TransportFailure(err)
                _state.value = ConnectionState.Failed(failure)
                throw failure
            }
            _state.value = ConnectionState.Connected
        }
    }

    /**
     * Send one request over a fresh libp2p stream. [bodyJson] is the raw JSON
     * of the event payload (e.g. a serialised `GetUserEvent`). The envelope
     * wrapping is done here.
     */
    suspend fun request(protocolId: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        if (_state.value !is ConnectionState.Connected) {
            throw WarpnetException.NotConnected()
        }

        val envelope = WarpnetEnvelope.unsigned(
            body = bodyJson,
            nodeId = signer.peerId,
            path = protocolId,
            timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
        ).copy(signature = signer.sign(bodyJson))

        val requestJson = envelopeAdapter.toJson(envelope)
        val raw = binding.stream(protocolId, requestJson)

        // The binding returns the error message directly (no JSON wrapping)
        // when the stream itself fails — e.g. peer disconnected. Distinguish
        // by attempting to parse a ResponseError; if that fails assume the
        // string is a ProtocolError body.
        throwIfErrorResponse(raw)
        raw
    }

    /** Close the native host. After this the client must be discarded. */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        mutex.withLock {
            binding.shutdown()
            _state.value = ConnectionState.Uninitialised
        }
    }

    private fun throwIfErrorResponse(raw: String) {
        if (raw.isEmpty()) return
        // A well-formed ResponseError is exactly `{"code":N,"message":"..."}`.
        // Anything else is either a valid payload or a transport-layer error
        // string from the binding. Heuristic: only try to parse if the first
        // non-whitespace char is `{` and the JSON has "code" + "message".
        if (!raw.trimStart().startsWith("{")) {
            throw WarpnetException.TransportFailure(raw)
        }
        val err = runCatching { errorAdapter.fromJson(raw) }.getOrNull() ?: return
        if (err.code == 0 && err.message.isEmpty()) return
        throw WarpnetException.ProtocolError(err.code, err.message)
    }
}

/**
 * Indirection over the gomobile binding so unit tests can swap it out. The
 * real implementation forwards straight to `node.Node`.
 */
interface WarpnetBinding {
    fun initialize(pskBase64: String, bootstrapAddrs: Array<String>): String
    fun connect(addrInfo: String): String
    fun stream(protocolId: String, data: String): String
    fun peerId(): String
    fun isConnected(): Boolean
    fun disconnect(): String
    fun shutdown(): String
}

/**
 * Thin forwarder to the gomobile-generated `node.Node` class. Kept as an
 * `object` so we don't carry any state on the Kotlin side that might desync
 * from the Go singleton.
 *
 * NOTE: the `node.Node` symbol only exists after gomobile produces
 * `warpnet.aar`. This import resolves at compile time from the AAR.
 */
object DefaultBinding : WarpnetBinding {
    override fun initialize(pskBase64: String, bootstrapAddrs: Array<String>): String =
        node.Node.Initialize(pskBase64, bootstrapAddrs.toList())

    override fun connect(addrInfo: String): String =
        node.Node.Connect(addrInfo)

    override fun stream(protocolId: String, data: String): String =
        node.Node.Stream(protocolId, data)

    override fun peerId(): String = node.Node.PeerID()

    override fun isConnected(): Boolean = node.Node.IsConnected() == "true"

    override fun disconnect(): String = node.Node.Disconnect()

    override fun shutdown(): String = node.Node.Shutdown()
}
