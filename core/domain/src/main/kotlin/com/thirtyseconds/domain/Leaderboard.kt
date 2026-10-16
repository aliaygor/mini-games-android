package com.thirtyseconds.domain

interface LeaderboardRepository {
    suspend fun page(boardId: String,cursor: String?=null): RankingPage
    suspend fun rank(boardId: String,userId: String): LeaderboardEntry?
}
data class RankingPage(val entries: List<LeaderboardEntry>,val cursor: String?,val cached: Boolean=false)
object RankingBoards {
    fun id(period: RankingPeriod,game: GameKind?,date: java.time.LocalDate): String {
        val prefix=when(period){RankingPeriod.GLOBAL->"global";RankingPeriod.DAILY->"daily_$date";RankingPeriod.WEEKLY->"weekly_${date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)}_${date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"}
        return "${prefix}_${game?.name?:"all"}"
    }
}
