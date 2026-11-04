package com.example.gemini

import android.app.Application
import com.telemetrydeck.sdk.TelemetryDeck

class AntiGemApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TelemetryDeck.start(
            this,
            TelemetryDeck.Builder()
                .appID("2D4431F8-9AF6-42CC-9329-FB06B3ECD961")
                .namespace("com.santoshkurmi")
                .testMode(BuildConfig.DEBUG)
                .showDebugLogs(BuildConfig.DEBUG)
                .sendNewSessionBeganSignal(true)
        )
    }
}
