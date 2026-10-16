package com.thirtyseconds.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thirtyseconds.domain.*
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

private val Context.playerStore by preferencesDataStore("player_v1")
class DataStorePlayerRepository(context: Context): PlayerRepository {
    private val store=context.applicationContext.playerStore
    private val key=stringPreferencesKey("player")
    private fun day()=LocalDate.now(ZoneOffset.UTC)
    override val player=store.data.map{Economy.today(PlayerCodec.decode(it[key]),day().toString())}
    private suspend fun update(block: (Player)->Player) {store.edit{prefs->prefs[key]=PlayerCodec.encode(block(Economy.today(PlayerCodec.decode(prefs[key]),day().toString())))}}
    override suspend fun createProfile(nickname: String)=update {p->
        val name=nickname.trim().ifEmpty {listOf("DarkWolf","RapidFox","NightHawk","NeoRider").random()+Random.nextInt(10,100)}
        require(name.length in 3..20 && name.all{it.isLetterOrDigit() || it=='_'}){"Nickname must contain 3–20 letters, digits or underscores"}
        p.copy(id=p.id.ifEmpty{UUID.randomUUID().toString()},nickname=name,profileCreated=true,joined=p.joined.ifEmpty{day().toString()})
    }
    override suspend fun record(result: GameResult)=update{Economy.record(it,result)}
    override suspend fun doubleCoins(result: GameResult)=update{Economy.double(it,result)}
    override suspend fun claimDaily()=update{Economy.daily(it,day().toString(),day().minusDays(1).toString())}
    override suspend fun claimMission(index: Int)=update{Economy.claim(it,index)}
    override suspend fun buyTheme(theme: String)=update{Economy.buy(it,theme)}
    override suspend fun mergeCloud(player: Player)=update{CloudMerge.merge(it,player)}
    override suspend fun settings(language: String,sound: Boolean,haptics: Boolean,telemetry: Boolean,music: Boolean)=update{it.copy(language=language,sound=sound,haptics=haptics,telemetry=telemetry,music=music)}
}
