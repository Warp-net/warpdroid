/*
 * Warpdroid - a Warpnet Android client.
 * Copyright (C) 2026 Warpdroid contributors.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.keylesspalace.tusky.components.pairing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.keylesspalace.tusky.MainActivity
import com.keylesspalace.tusky.R
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.launch
import site.warpnet.transport.dto.AuthNodeInfo

/**
 * Single-screen pairing flow: camera preview → confirm → connect → hand off
 * to [MainActivity]. Errors return the user to the scanner (camera stays on)
 * so they can retry without backing out of the activity.
 */
@AndroidEntryPoint
class PairingActivity : AppCompatActivity() {

    @Inject lateinit var moshi: Moshi

    @Inject lateinit var pairingCoordinator: PairingCoordinator

    private lateinit var previewView: PreviewView
    private lateinit var progress: View
    private lateinit var messagePanel: View
    private lateinit var messageTitle: android.widget.TextView
    private lateinit var messageBody: android.widget.TextView
    private lateinit var connectButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var scanPrompt: View

    private val validator by lazy { AuthNodeInfoValidator(moshi) }
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()
    private var analyzer: QrCodeAnalyzer? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showFatalMessage(getString(R.string.warpnet_pair_camera_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pairing)
        previewView = findViewById(R.id.previewView)
        progress = findViewById(R.id.progress)
        messagePanel = findViewById(R.id.messagePanel)
        messageTitle = findViewById(R.id.messageTitle)
        messageBody = findViewById(R.id.messageBody)
        connectButton = findViewById(R.id.connectButton)
        cancelButton = findViewById(R.id.cancelButton)
        scanPrompt = findViewById(R.id.scanPrompt)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        analyzer?.close()
        cameraProvider?.unbindAll()
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val qrAnalyzer = QrCodeAnalyzer { payload -> onQrScanned(payload) }
            analyzer = qrAnalyzer
            imageAnalysis.setAnalyzer(cameraExecutor, qrAnalyzer)

            provider.unbindAll()
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onQrScanned(raw: String) {
        // Analyzer fires off a camera thread; hop back onto the main thread
        // before touching views.
        runOnUiThread {
            when (val result = validator.validate(raw)) {
                is ValidationResult.Valid -> showConfirmation(result.authNodeInfo, result.rawJson)
                is ValidationResult.Invalid -> {
                    Toast.makeText(
                        this,
                        getString(R.string.warpnet_pair_invalid_qr, result.reason),
                        Toast.LENGTH_LONG,
                    ).show()
                    // Re-arm the analyzer so the user can scan again without
                    // leaving the activity.
                    analyzer?.close()
                    analyzer = QrCodeAnalyzer { payload -> onQrScanned(payload) }.also { next ->
                        val provider = cameraProvider ?: return@runOnUiThread
                        provider.unbindAll()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        imageAnalysis.setAnalyzer(cameraExecutor, next)
                        provider.bindToLifecycle(
                            this,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    }
                }
            }
        }
    }

    private fun showConfirmation(info: AuthNodeInfo, rawJson: String) {
        scanPrompt.visibility = View.GONE
        messagePanel.visibility = View.VISIBLE
        messageTitle.text = getString(R.string.warpnet_pair_title)
        val shortNodeId = info.nodeInfo.id.let { if (it.length > 16) "${it.take(8)}…${it.takeLast(6)}" else it }
        val firstAddr = info.nodeInfo.addresses.firstOrNull().orEmpty()
        messageBody.text = getString(
            R.string.warpnet_pair_confirm_body,
            info.identity.owner.username,
            shortNodeId,
            firstAddr,
        )
        connectButton.setOnClickListener { runPairing(info, rawJson) }
        cancelButton.setOnClickListener { finish() }
    }

    private fun runPairing(info: AuthNodeInfo, rawJson: String) {
        messagePanel.visibility = View.GONE
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val outcome = pairingCoordinator.pair(info, rawJson)
            progress.visibility = View.GONE
            handleOutcome(outcome)
        }
    }

    private fun handleOutcome(outcome: PairingOutcome) {
        when (outcome) {
            is PairingOutcome.Success -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                finish()
            }
            is PairingOutcome.Rejected -> showFatalMessage(
                getString(R.string.warpnet_pair_error_rejected, outcome.code, outcome.message),
            )
            is PairingOutcome.PeerIdMismatch -> showFatalMessage(
                getString(R.string.warpnet_pair_error_peer_mismatch),
            )
            is PairingOutcome.TransportError -> showFatalMessage(
                getString(R.string.warpnet_pair_error_transport, outcome.message),
            )
        }
    }

    private fun showFatalMessage(text: String) {
        scanPrompt.visibility = View.GONE
        progress.visibility = View.GONE
        messagePanel.visibility = View.VISIBLE
        messageTitle.text = getString(R.string.warpnet_pair_title)
        messageBody.text = text
        connectButton.visibility = View.GONE
        cancelButton.text = getString(R.string.action_cancel)
        cancelButton.setOnClickListener { finish() }
    }
}
