package com.thirtyseconds.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.thirtyseconds.app.R
import com.thirtyseconds.app.GameViewModel
import com.thirtyseconds.domain.*
import kotlin.math.*

@Composable internal fun Game(s:GameState,vm:GameViewModel,onHint:()->Unit={},hintReady:Boolean=false) {
    val pauseLabel=stringResource(R.string.pause)
    Column(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ArenaCard(Modifier.size(44.dp).semantics{contentDescription=pauseLabel},Blue,vm::pause){GameText("Ⅱ",Modifier.align(Alignment.Center),size=21)}
            Column(Modifier.weight(1f)){Text(stringResource(name(s.kind)),color=Muted,fontSize=11.sp,lineHeight=15.sp);GameText("%02d:%02d".format(((s.remainingMs+999)/1000)/60,((s.remainingMs+999)/1000)%60),size=25,align=androidx.compose.ui.text.style.TextAlign.Start)}
            Column(horizontalAlignment=Alignment.End){Text(label("SCORE","SKOR"),color=Muted,fontSize=10.sp,lineHeight=14.sp);GameText("%,d".format(s.score),size=25,color=Yellow)}
        }
        LinearProgressIndicator(progress={s.remainingMs.toFloat()/s.durationMs},Modifier.fillMaxWidth().height(3.dp),color=if(s.remainingMs<6000)Coral else Blue,trackColor=Panel)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Pill("Lv ${s.level} · ${s.difficulty.tier.name}",Muted);Pill("${label("Combo","Kombo")} ×${s.combo}",Violet)}
        val targetHits=ChallengeRules.minimumHits(s.kind)
        if(targetHits>0) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${label("Level target","Seviye hedefi")}: ${s.hits}/$targetHits",color=Blue,fontSize=11.sp,lineHeight=15.sp);Text(label("80% accuracy · combo ${minOf(4,targetHits)}","%80 isabet · kombo ${minOf(4,targetHits)}"),color=Muted,fontSize=11.sp,lineHeight=15.sp)}
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            when(s.kind) {
                GameKind.BLAST->BubbleBoard(s,vm)
                GameKind.TRAFFIC->TrafficScene(s,vm)
                GameKind.DIFFERENCE->DifferenceBoard(s,vm,onHint,hintReady)
                GameKind.DOORS->PathBoard(s,vm)
                GameKind.COLOR->ColorBoard(s,vm)
                GameKind.REFLEX->ReactionBoard(s,vm)
                GameKind.MATH->MathBoard(s,vm)
                GameKind.MEMORY->MemoryBoard(s,vm)
                GameKind.PRECISION->PrecisionBoard(s,vm)
                GameKind.SEQUENCE->SequenceBoard(s,vm)
                GameKind.WORD->WordBoard(s,vm)
                GameKind.MERGE->MergeBoard(s,vm)
                GameKind.PATTERN->PatternBoard(s,vm)
                GameKind.SUDOKU->SudokuBoard(s,vm)
                GameKind.LOGIC->LogicBoard(s,vm)
            }
        }
        Text("${label("Correct","Doğru")} ${s.hits} · ${label("Errors","Hata")} ${s.mistakes}",Modifier.align(Alignment.CenterHorizontally),color=Muted,fontSize=11.sp,lineHeight=16.sp)
    }
}
@Composable private fun SquareGrid(size:Int,modifier:Modifier=Modifier,cell:@Composable (Int,Modifier)->Unit) {
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val edge=minOf(maxWidth,(maxHeight.takeIf{it.value.isFinite()}?:maxWidth))
        val unit=(edge-(size-1)*6.dp)/size
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){repeat(size){row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){repeat(size){col->cell(row*size+col,Modifier.size(unit))}}}}
    }
}
@Composable private fun ColorBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        ArenaCard(Modifier.fillMaxWidth(),Blue){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Text(if(s.goal==1)label("INK","MÜREKKEP") else label("TEXT","YAZI"),fontSize=11.sp,color=Muted)
            GameText(stringResource(colorName(s.expression.toInt())),size=23,color=palette[s.selected])
            Text(label("Follow the label","Görevi takip et"),fontSize=11.sp,color=Muted)
        }}
        LinearProgressIndicator(progress={s.phaseMs.toFloat()/s.difficulty.windowMs},Modifier.fillMaxWidth(),color=Violet,trackColor=Panel)
        SquareGrid(s.size,Modifier.weight(1f).fillMaxWidth()){i,m->
            val color=palette[s.board[i]];val desc=stringResource(colorName(s.board[i]))
            Tile("",color,m.semantics{contentDescription=desc}){vm.answer(i)}
        }
    }
}
@Composable private fun PathBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(s.goal>0)label("Collect K → unlock G → reach E","K al → G aç → E noktasına ulaş") else label("Find the shortest route to E","E noktasına en kısa yolu bul"),color=Blue,fontSize=13.sp)
        SquareGrid(s.size,Modifier.weight(1f).fillMaxWidth()){i,m->
            val value=s.board[i];val c=when{ i==s.selected->Blue;value==-1->Panel;value==2 && s.goal>0->Yellow;value==3 && s.goal>0->Coral;value==4->Lime;else->Muted.copy(alpha=.22f)}
            Tile(when{i==s.selected->"●";value==2 && s.goal>0->"K";value==3 && s.goal>0->"G";value==4->"E";else->""},c,m,enabled=value!=-1){vm.answer(i)}
        }
        Text("${label("Moves","Hamle")}: ${s.moves}",color=Muted,fontSize=12.sp)
    }
}
@Composable private fun ReactionBoard(s:GameState,vm:GameViewModel) {
    val waiting=s.phase==Phase.WAITING;val decoy=waiting && s.expression=="decoy" && s.phaseMs<420;val c=if(decoy)Yellow else if(waiting)Violet else Lime
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly) {
        Text("${s.reactionTimes.size+1} / ${s.goal}",color=Muted,fontSize=14.sp)
        Box(Modifier.sizeIn(maxWidth=270.dp,maxHeight=270.dp).fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(Brush.radialGradient(listOf(c.copy(alpha=.15f),Ink))).border(3.dp,c,CircleShape).clickable{vm.answer(if(s.level>=6)1 else s.target)},contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxSize().padding(12.dp)){drawCircle(c.copy(alpha=.35f),style=Stroke(2.dp.toPx()))}
            Column(horizontalAlignment=Alignment.CenterHorizontally){GameText(if(decoy)label("HOLD","DUR") else if(waiting)label("WAIT…","BEKLE…") else if(s.level>=6) "${s.target+1}" else "GO",size=32);Text(if(waiting)label("Only green counts","Sadece yeşilde dokun") else label("Tap now","Şimdi dokun"),color=Muted,fontSize=12.sp)}
        }
        if(s.level>=6)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){repeat(3){i->Tile("${i+1}",if(waiting)Panel else Lime,Modifier.size(64.dp)){vm.answer(i)}}}
        val last=s.reactionTimes.lastOrNull()
        Text(if(last==null)label("Tap only when the ring turns green.","Halka yeşil olunca dokun.") else "${label("Last","Son")}: $last ms · ${label("Best","En iyi")}: ${s.reactionTimes.min()} ms",color=Muted,fontSize=12.sp)
    }
}
@Composable private fun MathBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.SpaceEvenly,horizontalAlignment=Alignment.CenterHorizontally) {
        Text(label("SOLVE WITHOUT A CALCULATOR","ZİHİNDEN HESAPLA"),color=Muted,fontSize=11.sp,letterSpacing=2.sp)
        GameText(s.expression,size=32)
        LinearProgressIndicator(progress={(s.phaseMs.toFloat()/(s.difficulty.windowMs*2)).coerceIn(0f,1f)},Modifier.fillMaxWidth(),color=Violet)
        s.options.chunked(2).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){row.forEach {answer->Tile("$answer",Blue,Modifier.weight(1f).height(78.dp)){vm.answer(answer)}}}}
    }
}
@Composable private fun MemoryBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(if(s.phase==Phase.PREVIEW)label("MEMORIZE","HATIRLA") else if(s.goal==1)label("REPEAT IN ORDER","AYNI SIRAYLA SEÇ") else label("SELECT THE HIGHLIGHTED CELLS","PARLAYAN HÜCRELERİ SEÇ"),color=Violet,fontSize=13.sp)
        val previewIndex=(s.responseMs/previewStepMs(s.level)).toInt().coerceAtMost(s.sequence.lastIndex)
        SquareGrid(s.size,Modifier.weight(1f).fillMaxWidth()){i,m->val lit=s.phase==Phase.PREVIEW && i==s.sequence[previewIndex] || i in s.found;Tile(if(s.phase==Phase.PREVIEW && i==s.sequence[previewIndex])"${previewIndex+1}" else if(i in s.found)"✓" else "",if(lit)Violet else Panel,m,enabled=s.phase==Phase.READY && i !in s.found){vm.answer(i)}}
        Text("${s.found.size} / ${s.sequence.size}",color=Muted,fontSize=12.sp)
    }
}
@Composable private fun PrecisionBoard(s:GameState,vm:GameViewModel) {
    val current by rememberUpdatedState(s)
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Panel.copy(alpha=.4f)).pointerInput(Unit){detectTapGestures{p->vm.pointer(p.x/size.width,p.y/size.height)}},contentAlignment=Alignment.TopCenter) {
        Text(label("CENTER = PERFECT","MERKEZ = PERFECT"),Modifier.padding(12.dp),color=Muted,fontSize=11.sp)
        Canvas(Modifier.fillMaxSize()) {
            val state=current;val radius=precisionRadius(state.level)*size.width;val center=Offset(state.targetX*size.width,state.targetY*size.height)
            drawCircle(Violet.copy(alpha=.12f),radius*1.6f,center);drawCircle(Violet,radius,center,style=Stroke(3.dp.toPx()));drawCircle(Blue,radius*.5f,center,style=Stroke(2.dp.toPx()));drawCircle(Color.White,radius*.18f,center)
        }
    }
}
@Composable private fun SequenceBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        GameText(if(s.goal==0)label("ASCENDING","ARTAN") else label("DESCENDING","AZALAN"),size=22,color=Blue)
        s.board.indices.toList().chunked(3).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{i->Tile(if(i in s.found)"✓" else s.board[i].toString(),if(i in s.found)Lime else Blue,Modifier.weight(1f).height(72.dp),i !in s.found){vm.answer(i)}};repeat(3-row.size){Spacer(Modifier.weight(1f))}}}
    }
}
@Composable private fun WordBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(label("BUILD A VALID WORD","GEÇERLİ KELİME OLUŞTUR"),color=Muted,fontSize=12.sp)
        GameText(s.typed.padEnd(s.word.length,'_'),size=32,color=Blue)
        s.letters.indices.toList().chunked(4).forEach {row->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{i->Tile(s.letters[i].toString(),Violet,Modifier.weight(1f).height(60.dp),i !in s.found){vm.answer(i)}};repeat(4-row.size){Spacer(Modifier.weight(1f))}}}
        TextButton(onClick={vm.answer(-2)}){Text(label("Clear","Temizle"))}
    }
}
@Composable private fun MergeBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(label("Merge equal neighbors. Move tiles into empty cells.","Eşit komşuları birleştir. Boş hücrelere taşı."),color=Muted,fontSize=13.sp)
        SquareGrid(4,Modifier.weight(1f).fillMaxWidth()){i,m->val v=s.board[i];Tile(if(v==0)"" else "$v",if(i==s.selected)Yellow else if(v==0)Panel else palette[(ln(v.toDouble())/ln(2.0)).toInt()%palette.size],m){vm.answer(i)}}
        Text("${label("Target","Hedef")}: ${s.goal} · ${label("Moves left","Kalan hamle")}: ${s.moveLimit-s.moves}",color=Yellow,fontSize=14.sp)
    }
}
private val signals=listOf("↑","→","↓","←","＋","×")
@Composable private fun PatternBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.SpaceEvenly) {
        Text(if(s.phase==Phase.PREVIEW)if(s.goal==1)label("MEMORIZE · INPUT OPPOSITE DIRECTIONS","HATIRLA · TERS YÖNLERİ GİR") else label("MEMORIZE THE SEQUENCE","DİZİYİ HATIRLA") else "${s.cursor} / ${s.sequence.size}",color=Muted,fontSize=12.sp)
        val previewIndex=(s.responseMs/previewStepMs(s.level)).toInt().coerceAtMost(s.board.lastIndex)
        GameText(if(s.phase==Phase.PREVIEW)signals[s.board[previewIndex]] else "${s.cursor} / ${s.sequence.size}",size=64,color=Violet)
        Text(if(s.phase==Phase.PREVIEW)"${previewIndex+1}/${s.sequence.size}" else label("Repeat the sequence","Diziyi tekrar et"),color=Muted,fontSize=12.sp)
        (0 until if(s.level>=16)6 else 4).toList().chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){row.forEach{i->Tile(signals[i],Blue,Modifier.weight(1f).height(62.dp),s.phase==Phase.READY){vm.answer(i)}}}}
    }
}
@Composable private fun SudokuBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(label("Fill every row, column and region.","Satır, sütun ve bölgeleri tamamla."),color=Muted,fontSize=12.sp)
        val bh=if(s.size==9)3 else 2;val bw=s.size/bh
        SquareGrid(s.size,Modifier.weight(1f).fillMaxWidth()){i,m->Tile(if(s.board[i]==0)"" else "${s.board[i]}",when{i==s.selected->Blue;i==s.hint->Yellow;i in s.givens->Muted.copy(alpha=.35f);else->Violet},m.border(if(i%bw==0 || (i/s.size)%bh==0)1.dp else 0.dp,Muted.copy(alpha=.35f),RoundedCornerShape(8.dp)),i !in s.givens && s.board[i]==0){vm.answer(i)}}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){(1..s.size).forEach{v->Tile("$v",Blue,Modifier.weight(1f).height(48.dp),s.selected>=0){vm.answer(1000+v)}}}
    }
}
@Composable private fun LogicPiece(code:Int,modifier:Modifier) {
    Canvas(modifier) {
        val shape=code%10;val rotation=code/10%10;val color=palette[code/100%10];val count=code/1000
        repeat(count) {i->
            val center=Offset(size.width*(i+1)/(count+1),size.height*.5f);val radius=minOf(size.width/(count+1)*.38f,size.height*.25f)
            rotate(rotation*90f,center) {
                val path=Path().apply {
                    if(shape==0){moveTo(center.x,center.y-radius);lineTo(center.x+radius,center.y+radius);lineTo(center.x-radius,center.y+radius)}
                    else if(shape==1){moveTo(center.x-radius,center.y-radius);lineTo(center.x+radius,center.y);lineTo(center.x-radius,center.y+radius);lineTo(center.x-radius*.3f,center.y)}
                    else{moveTo(center.x-radius,center.y-radius);lineTo(center.x,center.y-radius);lineTo(center.x,center.y);lineTo(center.x+radius,center.y);lineTo(center.x+radius,center.y+radius);lineTo(center.x-radius,center.y+radius)}
                    close()
                }
                drawPath(path,color);drawPath(path,Color.White.copy(alpha=.3f),style=Stroke(1.dp.toPx()))
            }
        }
    }
}
@Composable private fun LogicBoard(s:GameState,vm:GameViewModel) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(label("COMPLETE THE MATRIX","MATRİSİ TAMAMLA"),color=Muted,fontSize=12.sp)
        SquareGrid(3,Modifier.weight(1f).fillMaxWidth()){i,m->ArenaCard(m,Blue){if(i==8)GameText("?",Modifier.align(Alignment.Center),size=32,color=Blue) else LogicPiece(s.board[i],Modifier.fillMaxSize().padding(6.dp))}}
        s.options.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){row.forEach{code->ArenaCard(Modifier.weight(1f).height(68.dp),Violet,{vm.answer(code)}){LogicPiece(code,Modifier.fillMaxSize().padding(6.dp))}}}}
    }
}
internal val differenceRegions=listOf(Rect(.21f,.09f,.31f,.245f),Rect(.69f,.165f,.76f,.235f),Rect(.208f,.71f,.359f,.854f),Rect(.431f,.744f,.684f,.90f),Rect(.505f,.248f,.58f,.39f))
@Composable private fun DifferenceBoard(s:GameState,vm:GameViewModel,onHint:()->Unit,hintReady:Boolean) {
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text("${label("FIND 5 DIFFERENCES","5 FARKI BUL")} · ${s.found.size}/5",color=Blue,fontSize=13.sp)
        listOf(R.drawable.difference_cafe_a,R.drawable.difference_cafe_b).forEach{image->
            val found by rememberUpdatedState(s.found)
            Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(10.dp)).pointerInput(image){detectTapGestures{p->val normalized=Offset(p.x/size.width,p.y/size.height);val hit=differenceRegions.indexOfFirst{it.contains(normalized)};if(hit !in found)vm.answer(hit)}}) {
                Image(painterResource(image),label("Cafe comparison photograph","Kafe karşılaştırma fotoğrafı"),Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
                Canvas(Modifier.fillMaxSize()){(s.found+setOf(s.hint)).filter{it in differenceRegions.indices}.forEach{i->val r=differenceRegions[i];drawOval(if(i==s.hint)Yellow else Lime,Offset(r.left*size.width,r.top*size.height),Size(r.width*size.width,r.height*size.height),style=Stroke(2.dp.toPx()))}}
            }
        }
        TextButton(onClick=onHint,enabled=hintReady){Text(label("Hint · rewarded ad","İpucu · ödüllü reklam"))}
    }
}
