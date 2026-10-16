package com.thirtyseconds.app

import android.content.Context
import com.thirtyseconds.data.DataStorePlayerRepository
import com.thirtyseconds.domain.PlayerRepository
import com.thirtyseconds.platform.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun player(@ApplicationContext context:Context):PlayerRepository=DataStorePlayerRepository(context)
    @Provides @Singleton fun telemetry(@ApplicationContext context:Context)=Telemetry(context)
    @Provides @Singleton fun ads()=AdController(BuildConfig.REWARDED_ID,BuildConfig.INTERSTITIAL_ID)
    @Provides @Singleton fun cloud(@ApplicationContext context:Context,repository:PlayerRepository)=FirebaseServices(context,repository,BuildConfig.COMPETITION_URL)
}
