package com.thirtyseconds.platform

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class Telemetry(context: Context) {
    private val available = FirebaseApp.getApps(context).isNotEmpty()
    private val analytics = if (available) FirebaseAnalytics.getInstance(context) else null
    private val crashes = if (available) FirebaseCrashlytics.getInstance() else null
    fun consent(enabled: Boolean) {
        analytics?.setAnalyticsCollectionEnabled(enabled)
        crashes?.setCrashlyticsCollectionEnabled(enabled)
    }
    fun event(name: String, game: String, score: Int = 0) {
        analytics?.logEvent(name, Bundle().apply { putString("game", game); putLong("score", score.toLong()) })
    }
    fun error(error: Throwable) { crashes?.recordException(error) }
}
