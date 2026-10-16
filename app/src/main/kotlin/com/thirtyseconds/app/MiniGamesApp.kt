package com.thirtyseconds.app

import android.app.Application
import com.thirtyseconds.domain.PlayerRepository
import com.thirtyseconds.platform.*
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MiniGamesApp: Application() {
    @Inject lateinit var repository:PlayerRepository
    @Inject lateinit var telemetry:Telemetry
    @Inject lateinit var ads:AdController
    @Inject lateinit var cloud:FirebaseServices
    override fun onCreate(){super.onCreate();cloud.observe()}
}
