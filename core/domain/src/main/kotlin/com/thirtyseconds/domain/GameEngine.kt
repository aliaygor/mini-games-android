package com.thirtyseconds.domain

import kotlin.math.*
import kotlin.random.Random

enum class Category { QUICK, PUZZLE, LOGIC, SKILL, MEMORY }
enum class GameKind(val category: Category) {
    BLAST(Category.PUZZLE), TRAFFIC(Category.PUZZLE), DIFFERENCE(Category.SKILL),
    DOORS(Category.LOGIC), COLOR(Category.QUICK), REFLEX(Category.SKILL), MATH(Category.QUICK),
    MEMORY(Category.MEMORY), PRECISION(Category.SKILL), SEQUENCE(Category.QUICK),
    WORD(Category.LOGIC), MERGE(Category.PUZZLE), PATTERN(Category.MEMORY), SUDOKU(Category.LOGIC), LOGIC(Category.LOGIC)
}
enum class Phase { PREVIEW, READY, WAITING }
enum class Tier { EASY, NORMAL, HARD, EXPERT, MASTER }
data class Difficulty(val tier: Tier, val grid: Int, val windowMs: Long, val multiplier: Double, val tolerance: Int)
object DifficultyManager {
    fun forLevel(level: Int): Difficulty {
        val l=level.coerceAtLeast(1)
        val tier=when {l<=5->Tier.EASY;l<=15->Tier.NORMAL;l<=30->Tier.HARD;l<=50->Tier.EXPERT;else->Tier.MASTER}
        return Difficulty(tier, when(tier){Tier.EASY->4;Tier.NORMAL->5;else->6},
            (4200L-l*40).coerceAtLeast(1200),1.0+ln(l.toDouble())*.55,when(tier){Tier.EASY->7;Tier.NORMAL->6;Tier.HARD->5;else->3})
    }
}
data class Vehicle(val id: Int,val x: Int,val y: Int,val length: Int,val horizontal: Boolean)
data class GameState(
    val kind: GameKind,val seed: Int,val level: Int=1,val language: String="en",
    val durationMs: Long=45_000,val remainingMs: Long=durationMs,val elapsedMs: Long=0,
    val score: Int=0,val baseScore: Int=0,val speedScore: Int=0,val comboScore: Int=0,
    val hits: Int=0,val mistakes: Int=0,val combo: Int=0,val maxCombo: Int=0,
    val round: Int=0,val phase: Phase=Phase.READY,val phaseMs: Long=0,val responseMs: Long=0,
    val target: Int=0,val options: List<Int> = emptyList(),val board: List<Int> = emptyList(),val size: Int=4,
    val sequence: List<Int> = emptyList(),val cursor: Int=0,val found: Set<Int> = emptySet(),
    val cleared: Set<Int> = emptySet(),val fallOrigins: List<Int> = emptyList(),val burstColors: Map<Int,Int> = emptyMap(),
    val colorGoals: Map<Int,Int> = emptyMap(),val collected: Map<Int,Int> = emptyMap(),
    val selected: Int=-1,val moves: Int=0,val moveLimit: Int=0,val goal: Int=0,
    val vehicles: List<Vehicle> = emptyList(),val optimalMoves: Int=0,
    val expression: String="",val word: String="",val letters: String="",val typed: String="",
    val solution: List<Int> = emptyList(),val givens: Set<Int> = emptySet(),
    val reactionTimes: List<Long> = emptyList(),val targetX: Float=.5f,val targetY: Float=.5f,
    val completed: Boolean=false,val failed: Boolean=false,val hint: Int=-1,
) {
    val finished get()=remainingMs<=0 || completed || failed
    val difficulty get()=DifficultyManager.forLevel(level)
    val accuracy get()=if(hits+mistakes==0) 0.0 else hits.toDouble()/(hits+mistakes)
}
data class ScoreBreakdown(val base: Int,val speed: Int,val accuracy: Int,val combo: Int,val perfect: Int,val multiplier: Double) {
    val total get()=((base.toLong()+speed+accuracy+combo+perfect)*multiplier).roundToInt().coerceIn(0,2_000_000)
}
object ScoreManager {
    fun breakdown(s: GameState)=ScoreBreakdown(s.baseScore,s.speedScore,(s.accuracy*300).toInt(),s.comboScore,
        if(s.hits>0 && s.mistakes==0) 500 else 0,s.difficulty.multiplier)
    fun hit(s: GameState,base: Int=100): GameState {
        val combo=s.combo+1
        val n=s.copy(hits=s.hits+1,combo=combo,maxCombo=max(s.maxCombo,combo),baseScore=s.baseScore+base,
            speedScore=s.speedScore+((s.difficulty.windowMs-s.responseMs).coerceAtLeast(0)/30).toInt(),
            comboScore=s.comboScore+min(combo,20)*10,responseMs=0)
        return n.copy(score=breakdown(n).total)
    }
    fun miss(s: GameState): GameState {
        val n=s.copy(mistakes=s.mistakes+1,combo=0,responseMs=0,remainingMs=(s.remainingMs-700).coerceAtLeast(0))
        return n.copy(score=breakdown(n).total)
    }
}
interface MiniGame {
    fun start(seed: Int,level: Int,language: String): GameState
    fun tick(state: GameState,deltaMs: Long): GameState
    fun input(state: GameState,choice: Int): GameState
}
object GameEngine {
    fun start(kind: GameKind,seed: Int,level: Int=1,language: String="en"): GameState {
        val duration=if(kind in setOf(GameKind.BLAST,GameKind.TRAFFIC,GameKind.DOORS,GameKind.SUDOKU,GameKind.DIFFERENCE,GameKind.MERGE)) 90_000L else 45_000L
        val s=GameState(kind,seed,level.coerceAtLeast(1),language,duration,duration)
        return when(kind) {
            GameKind.BLAST->BlastGame.start(s)
            GameKind.TRAFFIC->TrafficGame.start(s)
            GameKind.DOORS->PathGame.start(s)
            GameKind.SUDOKU->SudokuGame.start(s)
            GameKind.MERGE->MergeGame.start(s)
            GameKind.DIFFERENCE->s.copy(goal=5,board=(0..4).toList())
            else->next(s)
        }
    }
    internal fun next(s: GameState): GameState {
        val r=Random(s.seed+s.round*7919)
        val d=s.difficulty
        val n=s.copy(round=s.round+1,cursor=0,found=emptySet(),phase=Phase.READY,responseMs=0,phaseMs=d.windowMs,typed="",hint=-1)
        return when(s.kind) {
            GameKind.COLOR->{
                val colors=if(s.level>=16) 6 else 4
                val target=r.nextInt(colors);val ink=(target+1+r.nextInt(colors-1))%colors;val followInk=r.nextBoolean()
                val size=if(s.level>=31) 6 else if(s.level>=6) 5 else 4
                val cells=MutableList(size*size){r.nextInt(colors)}.apply {this[r.nextInt(this.size)]=if(followInk) ink else target}
                n.copy(size=size,target=if(followInk) ink else target,selected=ink,goal=if(followInk) 1 else 0,expression=target.toString(),board=cells)
            }
            GameKind.REFLEX->n.copy(phase=Phase.WAITING,phaseMs=r.nextLong(1200,3500),target=if(s.level>=6) r.nextInt(3) else 1,goal=8+(s.level/10).coerceAtMost(4),expression=if(r.nextInt(3)==0)"decoy" else "")
            GameKind.MATH->{
                val a=r.nextInt(8,25+s.level);val b=r.nextInt(2,12);val c=r.nextInt(1,20)
                val mode=r.nextInt(if(s.level>=16)4 else if(s.level>=6)3 else 2)
                val (result,equation)=when(mode){0->a*b to "$a × $b";1->a+b+c to "$a + $b + $c";2->(a+b)*c to "($a + $b) × $c";else->a*b-c to "$a × $b − $c"}
                val choices=mutableSetOf(result)
                while(choices.size<4)choices+=result+listOf(-b,b,-c,c,-10,10,-1,1).random(r)
                n.copy(expression="$equation = ?",target=result,options=choices.shuffled(r),phaseMs=d.windowMs*2)
            }
            GameKind.MEMORY->{val size=d.grid; val count=(5+s.level/5).coerceAtMost(12);n.copy(size=size,sequence=(0 until size*size).shuffled(r).take(count),board=List(size*size){0},phase=Phase.PREVIEW,phaseMs=count*previewStepMs(s.level),goal=1)}
            GameKind.PRECISION->n.copy(targetX=r.nextFloat()*.7f+.15f,targetY=r.nextFloat()*.6f+.2f,responseMs=300)
            GameKind.SEQUENCE->{val count=(7+s.level/8).coerceAtMost(12);val values=generateSequence {r.nextInt(-20-s.level,80+s.level)}.distinct().take(count).toList(); val descending=r.nextBoolean();n.copy(board=values,sequence=values.indices.sortedBy {values[it]}.let {if(descending) it.reversed() else it},goal=if(descending) 1 else 0,phaseMs=d.windowMs*3)}
            GameKind.WORD->{val pool=Dictionary.words(s.language).filter {it.length in 5..(6+s.level/10).coerceAtMost(12)};val word=(pool.ifEmpty {Dictionary.words(s.language)}).random(r);n.copy(word=word,letters=word.toList().shuffled(r).joinToString(""),phaseMs=max(d.windowMs*4,8000))}
            GameKind.PATTERN->{val count=(5+s.level/5).coerceAtMost(12);val shown=List(count){r.nextInt(if(s.level>=16)6 else 4)};val inverse=s.level>=6 && r.nextBoolean();n.copy(board=shown,sequence=if(inverse)shown.map{when(it){0->2;2->0;1->3;3->1;else->it}} else shown,goal=if(inverse)1 else 0,phase=Phase.PREVIEW,phaseMs=count*previewStepMs(s.level))}
            GameKind.LOGIC->LogicGame.next(n,r)
            else->n
        }
    }
    fun tick(s: GameState,deltaMs: Long): GameState {
        if(s.finished || deltaMs<=0) return s
        val dt=min(deltaMs,s.remainingMs)
        var n=s.copy(remainingMs=s.remainingMs-dt,elapsedMs=s.elapsedMs+dt,responseMs=s.responseMs+dt)
        if(n.remainingMs==0L) return n.copy(completed=ChallengeRules.qualifies(n.kind,n.hits,n.mistakes,n.maxCombo,n.completed))
        if(s.phase!=Phase.READY) {
            val left=s.phaseMs-dt
            n=n.copy(phaseMs=max(0,left))
            if(left<=0) n=n.copy(phase=Phase.READY,phaseMs=if(s.kind==GameKind.REFLEX) 2500 else s.difficulty.windowMs,responseMs=max(0,-left))
        } else if(s.kind==GameKind.REFLEX) {
            n=n.copy(phaseMs=(s.phaseMs-dt).coerceAtLeast(0))
            if(n.phaseMs==0L)n=next(ScoreManager.miss(n))
        } else if(s.kind in setOf(GameKind.COLOR,GameKind.MATH,GameKind.SEQUENCE,GameKind.WORD,GameKind.LOGIC)) {
            n=n.copy(phaseMs=max(0,s.phaseMs-dt))
            if(n.phaseMs==0L) n=next(ScoreManager.miss(n))
        }
        if(s.kind==GameKind.PRECISION) {
            val angle=n.elapsedMs/1000f*(.9f+s.level*.035f)
            n=n.copy(targetX=.5f+sin(angle)*.32f,targetY=.5f+cos(angle*1.3f)*.28f)
        }
        return n
    }
    fun act(s: GameState,choice: Int): GameState {
        if(s.finished) return s
        return when(s.kind) {
            GameKind.BLAST->BlastGame.act(s,choice)
            GameKind.TRAFFIC->TrafficGame.act(s,choice)
            GameKind.DOORS->PathGame.act(s,choice)
            GameKind.SUDOKU->SudokuGame.act(s,choice)
            GameKind.MERGE->MergeGame.act(s,choice)
            GameKind.DIFFERENCE->if(choice in 0..4 && choice !in s.found) ScoreManager.hit(s,300).let {it.copy(found=s.found+choice,completed=(s.found+choice).size==5)} else if(choice==-1) ScoreManager.miss(s) else s
            GameKind.REFLEX->{
                if(s.phase==Phase.WAITING) next(ScoreManager.miss(s))
                else if(choice!=s.target) next(ScoreManager.miss(s))
                else {val times=s.reactionTimes+s.responseMs;val hit=ScoreManager.hit(s,(1200-s.responseMs).toInt().coerceIn(50,1200));if(times.size>=s.goal) hit.copy(reactionTimes=times,completed=true) else next(hit.copy(reactionTimes=times))}
            }
            GameKind.COLOR->if(choice !in s.board.indices) s else next(if(s.board[choice]==s.target) ScoreManager.hit(s) else ScoreManager.miss(s))
            GameKind.MATH,GameKind.LOGIC->next(if(choice==s.target) ScoreManager.hit(s) else ScoreManager.miss(s))
            GameKind.MEMORY->{
                if(s.phase!=Phase.READY || choice !in s.board.indices || choice in s.found) s
                else if(choice in s.sequence && (s.goal==0 || choice==s.sequence[s.cursor])) {
                    val found=s.found+choice;val hit=ScoreManager.hit(s).copy(found=found,cursor=s.cursor+1)
                    if(found.size==s.sequence.size) next(hit) else hit
                } else next(ScoreManager.miss(s))
            }
            GameKind.SEQUENCE,GameKind.PATTERN->{
                if(s.phase!=Phase.READY) s
                else if(choice==s.sequence[s.cursor]) {val hit=ScoreManager.hit(s).copy(cursor=s.cursor+1,found=s.found+choice);if(hit.cursor==s.sequence.size) next(hit) else hit}
                else next(ScoreManager.miss(s))
            }
            GameKind.WORD->{
                if(choice==-2) s.copy(typed="",found=emptySet())
                else if(choice !in s.letters.indices || choice in s.found) s
                else {val typed=s.typed+s.letters[choice];val found=s.found+choice
                    if(typed.length==s.word.length) {if(Dictionary.valid(s.language,typed,s.word)) next(ScoreManager.hit(s,typed.length*100)) else next(ScoreManager.miss(s))}
                    else s.copy(typed=typed,found=found)}
            }
            GameKind.PRECISION->s
        }
    }
    fun pointer(s: GameState,x: Float,y: Float): GameState {
        if(s.finished || s.kind!=GameKind.PRECISION || s.responseMs<180) return s
        val distance=hypot(x-s.targetX,y-s.targetY)
        val radius=precisionRadius(s.level)
        return if(distance>radius) ScoreManager.miss(s) else ScoreManager.hit(s,when {distance<radius*.2f->500;distance<radius*.5f->300;else->100})
    }
    fun hint(s: GameState): GameState {
        if(s.finished) return s
        return when(s.kind) {
            GameKind.DIFFERENCE->s.copy(hint=(0..4).firstOrNull {it !in s.found}?:-1)
            GameKind.SUDOKU->s.copy(hint=s.board.indices.firstOrNull {s.board[it]==0}?:-1)
            else->s
        }
    }
}
fun previewStepMs(level:Int)=(700L-level*5).coerceAtLeast(300)
fun precisionRadius(level:Int)=(.09f-level*.0009f).coerceAtLeast(.025f)
object Dictionary {
    private val en=listOf("RATE","TEAR","EAST","SEAT","STAR","TARS","NOTE","TONE","STONE","TONES","ONSET","EARTH","HEART","NIGHT","THING","SILENT","LISTEN","ENLIST","STREAM","MASTER","PLANET","TRAVEL","NETWORK","REACTOR","CAPTURE","LANTERN","PICTURE","COMPLETE")
    private val tr=listOf("KALE","LAKE","KARE","RENK","KART","KAPI","MASA","KASA","OYUN","AKIL","HIZLI","ZAMAN","KİTAP","BULUT","DENİZ","ORMAN","KELİME","HEDEF","YILDIZ","DENGE","SOKAK","ANKARA","İSTANBUL","BİLGİSAYAR")
    fun words(language: String)=if(language=="tr") tr else en
    fun valid(language: String,typed: String,original: String)=typed in words(language) && typed.toList().sorted()==original.toList().sorted()
}
