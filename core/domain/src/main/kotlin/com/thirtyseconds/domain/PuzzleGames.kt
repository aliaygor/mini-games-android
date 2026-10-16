package com.thirtyseconds.domain

import kotlin.math.*
import kotlin.random.Random

object BlastGame {
    fun start(s: GameState): GameState {
        val r=Random(s.seed);val size=(6+s.level/20).coerceAtMost(8)
        val colors=if(s.level>=16)6 else 5
        val board=MutableList(size*size){r.nextInt(colors)}
        if(s.level>=6) repeat(size) {board[r.nextInt(board.size)]=-1}
        // At least one legal opening even on obstacle-heavy levels.
        board[0]=0;board[1]=0
        val targets=(0 until colors).shuffled(r).take(if(s.level>=16)4 else 3).associateWith {10+(s.level/3).coerceAtMost(15)}
        return s.copy(size=size,board=board,goal=targets.values.sum(),colorGoals=targets,moveLimit=(20-s.level/12).coerceAtLeast(14))
    }
    fun group(s: GameState,index: Int): Set<Int> {
        if(index !in s.board.indices || s.board[index]<0) return emptySet()
        val color=s.board[index]%10;val seen=mutableSetOf(index);val queue=ArrayDeque<Int>().apply {add(index)}
        while(queue.isNotEmpty()) {
            val i=queue.removeFirst()
            neighbors(i,s.size).filter {it !in seen && s.board[it]>=0 && s.board[it]%10==color}.forEach {seen+=it;queue+=it}
        }
        return seen
    }
    fun act(s: GameState,index: Int): GameState {
        val connected=group(s,index)
        if(connected.size<2 && (index !in s.board.indices || s.board[index]<10)) return ScoreManager.miss(s)
        val removed=connected.toMutableSet()
        val activated=mutableSetOf<Int>()
        do {
            val specials=removed.filter {s.board[it]>=10 && it !in activated}
            specials.forEach {special->
                activated+=special
                if(s.board[special]>=20) removed+=s.board.indices.filter {s.board[it]>=0 && s.board[it]%10==s.board[special]%10}
                else removed+=s.board.indices.filter {s.board[it]>=0 && (it/s.size==special/s.size || it%s.size==special%s.size)}
            }
        } while(removed.any {s.board[it]>=10 && it !in activated})
        val r=Random(s.seed+s.moves*7919+1);val board=s.board.toMutableList()
        val origins=MutableList(board.size){it}
        removed.forEach {board[it]=-2}
        for(x in 0 until s.size) {
            // Stone cells anchor each segment; cleared cells refill only inside that segment.
            var bottom=s.size-1
            while(bottom>=0) {
                if(board[bottom*s.size+x]==-1) {bottom--;continue}
                var top=bottom;while(top>0 && board[(top-1)*s.size+x]!=-1) top--
                val survivors=(top..bottom).map {it*s.size+x}.filter {board[it]>=0}
                val values=survivors.map {board[it]}
                for(y in top..bottom) {
                    val slot=y-(bottom-values.size+1)
                    board[y*s.size+x]=if(slot>=0)values[slot] else r.nextInt(if(s.level>=16)6 else 5)
                    origins[y*s.size+x]=if(slot>=0)survivors[slot] else -(bottom-values.size+1-y)
                }
                bottom=top-1
            }
        }
        if(activated.isEmpty() && connected.size>=5 && index in board.indices && board[index]>=0) board[index]=board[index]%10+if(connected.size>=8) 20 else 10
        // A refill must always leave a legal move instead of ending in a silent dead board.
        if(board.indices.none {board[it]>=10 || (board[it]>=0 && neighbors(it,s.size).any {n->board[n]>=0 && board[n]%10==board[it]%10})}) {
            val pair=board.indices.firstOrNull {board[it]>=0 && neighbors(it,s.size).any {n->board[n]>=0}}
            if(pair!=null) board[neighbors(pair,s.size).first {board[it]>=0}]=board[pair]%10
        }
        val collected=s.collected.toMutableMap()
        removed.forEach {i->val color=s.board[i]%10;collected[color]=(collected[color]?:0)+1}
        val hit=ScoreManager.hit(s,removed.size*removed.size*15).copy(board=board,moves=s.moves+1,cleared=removed,fallOrigins=origins,burstColors=removed.associateWith {s.board[it]%10},collected=collected)
        val complete=s.colorGoals.isNotEmpty() && s.colorGoals.all {(color,target)->(collected[color]?:0)>=target} && hit.moves>=3
        return hit.copy(completed=complete,failed=hit.moves>=s.moveLimit && !complete)
    }
}
internal fun neighbors(i: Int,size: Int)=listOfNotNull(if(i%size>0)i-1 else null,if(i%size<size-1)i+1 else null,if(i>=size)i-size else null,if(i<size*(size-1))i+size else null)
object TrafficGame {
    fun occupied(v: List<Vehicle>)=buildSet {v.forEach {car->repeat(car.length){add((car.y+(if(car.horizontal)0 else it))*6+car.x+(if(car.horizontal)it else 0))}}}
    fun move(v: List<Vehicle>,id: Int,delta: Int): List<Vehicle>? {
        if(delta !in listOf(-1,1)) return null
        val car=v.firstOrNull {it.id==id}?:return null
        val x=car.x+if(car.horizontal)delta else 0;val y=car.y+if(car.horizontal)0 else delta
        if(x<0 || y<0 || x+(if(car.horizontal)car.length else 1)>6 || y+(if(car.horizontal)1 else car.length)>6) return null
        val other=occupied(v.filter {it.id!=id})
        if((0 until car.length).any { (y+(if(car.horizontal)0 else it))*6+x+(if(car.horizontal)it else 0) in other }) return null
        return v.map {if(it.id==id) it.copy(x=x,y=y) else it}
    }
    fun solved(v: List<Vehicle>)=v.first {it.id==0}.let {it.x+it.length==6}
    fun solve(v: List<Vehicle>,limit: Int=100_000): Int? {
        fun key(c: List<Vehicle>)=c.joinToString(","){if(it.horizontal) it.x.toString() else it.y.toString()}
        val seen=mutableSetOf(key(v));val queue=ArrayDeque<Pair<List<Vehicle>,Int>>().apply {add(v to 0)}
        while(queue.isNotEmpty() && seen.size<=limit) {
            val (current,depth)=queue.removeFirst();if(solved(current)) return depth
            current.forEach {car->listOf(-1,1).forEach {delta->move(current,car.id,delta)?.let {next->if(seen.add(key(next))) queue.add(next to depth+1)}}}
        }
        return null
    }
    fun start(s: GameState): GameState {
        val minimum=when(s.difficulty.tier){Tier.EASY->10;Tier.NORMAL->12;Tier.HARD->15;Tier.EXPERT->20;Tier.MASTER->25}
        val layouts=ParkingCatalogue.boards.filter {it.optimal>=minimum}
        val layout=layouts[Math.floorMod(s.seed.toLong()+s.level*7919L,layouts.size.toLong()).toInt()]
        return s.copy(size=6,vehicles=layout.cars,optimalMoves=layout.optimal,
            moveLimit=layout.optimal+when(s.difficulty.tier){Tier.EASY->7;Tier.NORMAL->6;Tier.HARD->5;Tier.EXPERT->4;Tier.MASTER->3})
    }
    fun act(s: GameState,choice: Int): GameState {
        val id=choice/10;val delta=if(choice%10==0)-1 else if(choice%10==1)1 else return s
        val moved=move(s.vehicles,id,delta)?:return s
        val n=s.copy(vehicles=moved,moves=s.moves+1)
        return if(solved(moved)) ScoreManager.hit(n,1800+max(0,500-(n.moves-s.optimalMoves)*60)).copy(completed=true)
        else n.copy(failed=n.moves>=s.moveLimit)
    }
}
object PathGame {
    fun start(s: GameState): GameState {
        val size=if(s.level>=31)13 else if(s.level>=6)11 else 9;val r=Random(s.seed);val board=MutableList(size*size){-1}
        fun carve(i: Int) {
            board[i]=0
            listOf(-2,2,-size*2,size*2).shuffled(r).forEach {offset->
                val n=i+offset
                if(n in board.indices && n/size in 1 until size-1 && n%size in 1 until size-1 && (abs(offset)!=2 || n/size==i/size) && board[n]==-1) {board[i+offset/2]=0;carve(n)}
            }
        }
        carve(size+1)
        val start=size+1;val distances=distances(board,size,start)
        val exit=distances.maxBy {it.value}.key
        val path=path(board,size,start,exit)
        if(path.size>5) {board[path[path.size/3]]=2;board[path[path.size*2/3]]=3}
        board[exit]=4
        return s.copy(board=board,size=size,selected=start,target=exit,optimalMoves=path.size-1,goal=1,moveLimit=path.size-1+s.difficulty.tolerance*2)
    }
    fun distances(board: List<Int>,size: Int,start: Int): Map<Int,Int> {
        val distances=mutableMapOf(start to 0);val q=ArrayDeque<Int>().apply {add(start)}
        while(q.isNotEmpty()) {val i=q.removeFirst();neighbors(i,size).filter {board[it]!=-1 && it !in distances}.forEach {distances[it]=distances.getValue(i)+1;q+=it}}
        return distances
    }
    fun path(board: List<Int>,size: Int,start: Int,end: Int): List<Int> {
        val d=distances(board,size,start);if(end !in d) return emptyList()
        val result=mutableListOf(end);var i=end
        while(i!=start) {i=neighbors(i,size).first {d[it]==d.getValue(i)-1};result+=i}
        return result.reversed()
    }
    fun act(s: GameState,choice: Int): GameState {
        if(choice !in neighbors(s.selected,s.size) || s.board[choice]==-1) return s
        if(s.board[choice]==3 && s.goal>0) return ScoreManager.miss(s)
        val n=s.copy(selected=choice,moves=s.moves+1,goal=if(s.board[choice]==2)0 else s.goal)
        return if(choice==s.target) ScoreManager.hit(n,2000+max(0,500-(n.moves-s.optimalMoves)*30)).copy(completed=true) else n.copy(failed=n.moves>=s.moveLimit)
    }
}
object SudokuGame {
    fun start(s: GameState): GameState {
        val size=if(s.level>=31)9 else 6;val bh=if(size==9)3 else 2;val bw=size/bh;val r=Random(s.seed)
        val symbols=(1..size).shuffled(r)
        val rowBands=(0 until size/bh).shuffled(r).flatMap {band->(0 until bh).shuffled(r).map {band*bh+it}}
        val colBands=(0 until size/bw).shuffled(r).flatMap {band->(0 until bw).shuffled(r).map {band*bw+it}}
        val solution=rowBands.flatMap {row->colBands.map {col->symbols[(row*bw+row/bh+col)%size]}}
        val board=solution.toMutableList();val desired=(12+s.level/2).coerceAtMost(size*size*2/3)
        for(i in board.indices.shuffled(r)) {
            if(board.count {it==0}>=desired) break
            val old=board[i];board[i]=0
            if(solutionCount(board,size)!=1) board[i]=old
        }
        return s.copy(size=size,board=board,solution=solution,givens=board.indices.filter {board[it]!=0}.toSet())
    }
    fun candidates(board: List<Int>,size: Int,index: Int): List<Int> {
        val row=index/size;val col=index%size;val bh=if(size==9)3 else 2;val bw=size/bh
        val used=board.indices.filter {it/size==row || it%size==col || (it/size)/bh==row/bh && (it%size)/bw==col/bw}.map {board[it]}.toSet()
        return (1..size).filter {it !in used}
    }
    fun solutionCount(board: List<Int>,size: Int,limit: Int=2): Int {
        val index=board.indices.filter {board[it]==0}.minByOrNull {candidates(board,size,it).size}?:return 1
        var count=0
        for(value in candidates(board,size,index)) {count+=solutionCount(board.toMutableList().apply {this[index]=value},size,limit-count);if(count>=limit)return count}
        return count
    }
    fun act(s: GameState,choice: Int): GameState {
        if(choice>=1000) {
            val value=choice-1000;val index=s.selected
            if(index !in s.board.indices || index in s.givens || s.board[index]!=0 || value !in 1..s.size) return s
            if(s.solution[index]!=value) return ScoreManager.miss(s)
            val board=s.board.toMutableList().apply {this[index]=value}
            return ScoreManager.hit(s,150).copy(board=board,moves=s.moves+1,completed=board.none {it==0},selected=-1)
        }
        return if(choice in s.board.indices && choice !in s.givens && s.board[choice]==0) s.copy(selected=choice) else s
    }
}
object MergeGame {
    fun start(s:GameState):GameState {
        val r=Random(s.seed)
        val board=MutableList(16){0};board[0]=2;board[1]=2
        (2..15).shuffled(r).take(6).forEach {board[it]=if(r.nextBoolean())2 else 4}
        return s.copy(board=board,goal=64 shl (s.level/12).coerceAtMost(5),moveLimit=(65-s.level/3).coerceAtLeast(35))
    }
    fun act(s: GameState,index: Int): GameState {
        if(index !in s.board.indices)return s
        if(s.selected<0)return if(s.board[index]>0)s.copy(selected=index) else s
        if(index==s.selected)return s.copy(selected=-1)
        if(index !in neighbors(s.selected,s.size))return if(s.board[index]>0)s.copy(selected=index) else s
        if(s.board[index]!=0 && s.board[index]!=s.board[s.selected])return s.copy(selected=index)
        val merged=s.board[index]>0
        val board=s.board.toMutableList();board[index]=s.board[s.selected]*(if(merged)2 else 1);board[s.selected]=0
        var chain=0
        if(merged) {
            while(true) {
                val match=neighbors(index,s.size).firstOrNull{board[it]==board[index]}?:break
                board[match]=0;board[index]*=2;chain++
            }
        }
        val r=Random(s.seed+s.moves*7919)
        val empty=board.indices.filter{board[it]==0};if(empty.isNotEmpty())board[empty.random(r)]=if(r.nextInt(5)==0)4 else 2
        val next=if(merged)ScoreManager.hit(s,board[index]*10+chain*150) else s
        val n=next.copy(board=board,selected=-1,moves=s.moves+1)
        val legal=board.any{it==0} || board.indices.any{i->neighbors(i,s.size).any{board[it]==board[i]}}
        val complete=board.max()>=s.goal
        return n.copy(completed=complete,failed=!complete && (!legal || n.moves>=s.moveLimit))
    }
}
object LogicGame {
    fun next(s:GameState,r:Random):GameState {
        val shape=r.nextInt(3);val rotation=r.nextInt(4);val color=r.nextInt(4)
        val cells=List(9){i->encode((shape+i/3+i%3)%3,
            (rotation+i/3+2*(i%3))%4,
            if(s.level>=6)(color+i/3+i%3)%4 else 0,
            if(s.level>=16)(i/3+i%3)%3+1 else 1)}
        val answer=cells.last();val choices=mutableSetOf(answer)
        val factors=buildList{add(1);add(10);if(s.level>=6)add(100);if(s.level>=16)add(1000)}
        while(choices.size<4) {
            val factor=factors.random(r);val current=(answer/factor)%10;val max=when(factor){1->3;10,100->4;else->3}
            val replacement=if(factor==1000)(current%3)+1 else (current+1+r.nextInt(max-1))%max
            choices+=answer+(replacement-current)*factor
        }
        return s.copy(board=cells.take(8),target=answer,options=choices.shuffled(r),phaseMs=s.difficulty.windowMs*2)
    }
    private fun encode(shape:Int,rotation:Int,color:Int,count:Int)=shape+rotation*10+color*100+count*1000
}

