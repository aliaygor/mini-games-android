package com.thirtyseconds.data

import com.thirtyseconds.domain.*
import org.junit.Assert.*
import org.junit.Test

class PlayerCodecTest {
    @Test fun legacyMigrationKeepsWalletButArchivesOldRulesetRecords() {
        val old="""{"version":1,"coins":320,"xp":1400,"games":8,"records":{"COLOR":1000,"STACK":750}}"""
        val migrated=PlayerCodec.decode(old)
        assertEquals(320,migrated.coins);assertEquals(1400,migrated.xp)
        assertTrue(migrated.records.isEmpty());assertEquals(1000,migrated.legacyRecords["COLOR"])
        assertEquals(migrated,PlayerCodec.decode(PlayerCodec.encode(migrated)))
    }
    @Test fun newSessionAndWalletRoundTripPreservesEveryProgressField() {
        val r=GameResult("test",GameKind.TRAFFIC,8500,1,0,27,true,42000,14,12,3,breakdown=ScoreBreakdown(1000,400,300,800,500,2.0))
        val p=Economy.daily(Economy.double(Economy.record(Player(id="id",nickname="NeoRider",profileCreated=true),r),r),"2026-10-05","2026-10-04")
        assertEquals(p,PlayerCodec.decode(PlayerCodec.encode(p)))
    }
}
