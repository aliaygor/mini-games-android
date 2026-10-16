package com.thirtyseconds.data

import com.thirtyseconds.domain.*
import org.json.JSONArray
import org.json.JSONObject

object PlayerCodec {
    fun encode(p: Player): String=JSONObject().apply {
        put("version",2);put("id",p.id);put("nickname",p.nickname);put("profileCreated",p.profileCreated);put("joined",p.joined)
        put("wallet",JSONObject(p.wallet));put("coins",p.coins);put("xp",p.xp);put("games",p.games);put("totalScore",p.totalScore);put("perfectGames",p.perfectGames)
        put("records",JSONObject().apply {p.records.forEach{(k,v)->put(k.name,v)}})
        put("gameLevels",JSONObject().apply {p.gameLevels.forEach{(k,v)->put(k.name,v)}})
        put("legacyRecords",JSONObject(p.legacyRecords))
        put("results",JSONArray().apply {p.results.values.forEach {r->put(JSONObject().apply {
            put("id",r.id);put("kind",r.kind.name);put("score",r.score);put("hits",r.hits);put("mistakes",r.mistakes);put("level",r.level);put("completed",r.completed)
            put("durationMs",r.durationMs);put("moves",r.moves);put("optimalMoves",r.optimalMoves);put("maxCombo",r.maxCombo);put("bestReactionMs",r.bestReactionMs);put("averageReactionMs",r.averageReactionMs);put("day",r.day);put("dailyChallenge",r.dailyChallenge)
            put("breakdown",JSONObject().apply {put("base",r.breakdown.base);put("speed",r.breakdown.speed);put("accuracy",r.breakdown.accuracy);put("combo",r.breakdown.combo);put("perfect",r.breakdown.perfect);put("multiplier",r.breakdown.multiplier)})
        })}})
        put("day",p.day);put("dailyGames",p.dailyGames);put("dailyScore",p.dailyScore);put("dailyKinds",JSONArray(p.dailyKinds.map{it.name}));put("claimed",JSONArray(p.claimed.toList()))
        put("achievements",JSONArray(p.achievements.toList()));put("rewardClaims",JSONArray(p.rewardClaims.toList()));put("doubledIds",JSONArray(p.doubledIds.toList()))
        put("lastRewardDay",p.lastRewardDay);put("streak",p.streak);put("lastSession",p.lastSession);put("doubledSession",p.doubledSession)
        put("owned",JSONArray(p.owned.toList()));put("theme",p.theme);put("language",p.language);put("sound",p.sound);put("music",p.music);put("haptics",p.haptics);put("telemetry",p.telemetry)
    }.toString()
    fun decode(raw: String?): Player {
        if(raw==null)return Player()
        val j=JSONObject(raw)
        fun strings(name: String)=j.optJSONArray(name)?.let{a->(0 until a.length()).map{a.getString(it)}}?:emptyList()
        fun gameMap(name: String):Map<GameKind,Int> { val obj=j.optJSONObject(name)?:return emptyMap();return obj.keys().asSequence().mapNotNull {key->runCatching{GameKind.valueOf(key)}.getOrNull()?.let{it to obj.getInt(key)}}.toMap() }
        val migrated=j.optInt("version",1)>=2
        val records=if(migrated)gameMap("records") else emptyMap()
        val legacy=j.optJSONObject(if(migrated)"legacyRecords" else "records")
        val results=mutableMapOf<String,GameResult>()
        j.optJSONArray("results")?.let {a->repeat(a.length()){i->val r=a.getJSONObject(i);val kind=runCatching{GameKind.valueOf(r.getString("kind"))}.getOrNull()?:return@repeat
            val b=r.optJSONObject("breakdown")
            val result=GameResult(r.getString("id"),kind,r.getInt("score"),r.getInt("hits"),r.getInt("mistakes"),r.optInt("level",1),r.optBoolean("completed"),r.optLong("durationMs"),r.optInt("moves"),r.optInt("optimalMoves"),r.optInt("maxCombo"),r.optLong("bestReactionMs"),r.optLong("averageReactionMs"),
                if(b==null)ScoreBreakdown(r.getInt("score"),0,0,0,0,1.0) else ScoreBreakdown(b.optInt("base"),b.optInt("speed"),b.optInt("accuracy"),b.optInt("combo"),b.optInt("perfect"),b.optDouble("multiplier",1.0)),r.optString("day","2026-01-01"),r.optBoolean("dailyChallenge"))
            results[result.id]=result
        }}
        return Player(id=j.optString("id"),nickname=j.optString("nickname"),profileCreated=j.optBoolean("profileCreated"),joined=j.optString("joined"),wallet=j.optJSONObject("wallet")?.let{w->w.keys().asSequence().associateWith{w.getInt(it)}}?:emptyMap(),coins=j.optInt("coins"),xp=j.optInt("xp"),games=j.optInt("games"),totalScore=j.optInt("totalScore"),perfectGames=j.optInt("perfectGames"),records=records,
            gameLevels=gameMap("gameLevels").mapValues{it.value.coerceAtLeast(1)},legacyRecords=legacy?.keys()?.asSequence()?.associateWith{legacy.optInt(it)}?:emptyMap(),results=results,
            day=j.optString("day"),dailyGames=j.optInt("dailyGames"),dailyScore=j.optInt("dailyScore"),dailyKinds=strings("dailyKinds").mapNotNull{runCatching{GameKind.valueOf(it)}.getOrNull()}.toSet(),claimed=strings("claimed").mapNotNull{it.toIntOrNull()}.toSet(),
            achievements=strings("achievements").toSet(),rewardClaims=strings("rewardClaims").toSet(),doubledIds=strings("doubledIds").toSet(),lastRewardDay=j.optString("lastRewardDay"),streak=j.optInt("streak"),lastSession=j.optString("lastSession"),doubledSession=j.optString("doubledSession"),owned=strings("owned").toSet()+"lime",theme=j.optString("theme","lime"),language=j.optString("language"),sound=j.optBoolean("sound",true),music=j.optBoolean("music"),haptics=j.optBoolean("haptics",true),telemetry=j.optBoolean("telemetry"))
    }
}
