package com.thirtyseconds.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.thirtyseconds.app.GameViewModel
import com.thirtyseconds.domain.*
import kotlin.math.*

internal fun DrawScope.drawBubble(center:Offset,radius:Float,color:Color,special:Int=0,alpha:Float=1f) {
    drawCircle(Color.Black.copy(alpha=.4f*alpha),radius,center+Offset(0f,radius*.13f))
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha=.82f*alpha),color.copy(alpha=alpha),color.copy(alpha=.7f*alpha),Color(0xFF061526).copy(alpha=.95f*alpha)),center-Offset(radius*.3f,radius*.38f),radius*1.65f),radius,center)
    drawCircle(color.copy(alpha=.7f*alpha),radius*.96f,center,style=Stroke(radius*.035f))
    drawArc(Color.White.copy(alpha=.65f*alpha),205f,82f,false,center-Offset(radius*.73f,radius*.73f),Size(radius*1.46f,radius*1.46f),style=Stroke(radius*.075f,cap=StrokeCap.Round))
    drawOval(Color.White.copy(alpha=.55f*alpha),center-Offset(radius*.38f,radius*.53f),Size(radius*.32f,radius*.18f))
    if(special==1) {
        drawLine(Color.White,center-Offset(radius*.55f,0f),center+Offset(radius*.55f,0f),radius*.12f,StrokeCap.Round)
        drawLine(Color.White,center-Offset(0f,radius*.55f),center+Offset(0f,radius*.55f),radius*.12f,StrokeCap.Round)
    } else if(special==2) {
        drawCircle(Color.White,radius*.35f,center,style=Stroke(radius*.08f))
        repeat(8){i->val a=i*PI/4;drawCircle(Color.White,radius*.07f,center+Offset(cos(a).toFloat(),sin(a).toFloat())*radius*.53f)}
    }
}

@Composable internal fun BubbleBoard(s:GameState,vm:GameViewModel) {
    val transition=remember(s.seed){Animatable(1f)}
    var selected by remember(s.seed){mutableIntStateOf(-1)}
    var shake by remember(s.seed){mutableIntStateOf(0)}
    val shakeProgress=remember{Animatable(1f)}
    val current by rememberUpdatedState(s)
    LaunchedEffect(s.moves) {if(s.moves>0){selected=-1;transition.snapTo(0f);transition.animateTo(1f,tween(620,easing=LinearEasing))}}
    LaunchedEffect(shake) {if(shake>0){shakeProgress.snapTo(0f);shakeProgress.animateTo(1f,tween(280))}}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column {Text(label("COLLECT THE COLORS","HEDEF RENKLERİ TOPLA"),color=Muted,fontSize=10.sp,lineHeight=14.sp,letterSpacing=1.sp);Text("${s.colorGoals.entries.sumOf{(c,n)->(s.collected[c]?:0).coerceAtMost(n)}} / ${s.goal}",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)}
            Column(horizontalAlignment=Alignment.End){Text(label("MOVES LEFT","KALAN HAMLE"),color=Muted,fontSize=10.sp,letterSpacing=1.sp);Text("${s.moveLimit-s.moves}",color=Blue,fontSize=25.sp,fontWeight=FontWeight.Bold)}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            s.colorGoals.forEach {(color,target)->
                val have=(s.collected[color]?:0).coerceAtMost(target)
                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(palette[color].copy(alpha=.12f)).padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Canvas(Modifier.size(22.dp)){drawBubble(Offset(size.width/2,size.height/2),size.width*.44f,palette[color])}
                    Text(if(have==target)"✓" else "${target-have}",color=if(have==target)Lime else Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            val edge=minOf(maxWidth,maxHeight)
            Canvas(Modifier.size(edge).clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF142D43),Color(0xFF081824)))).pointerInput(s.seed) {
                detectTapGestures {point->
                    if(transition.value<1f)return@detectTapGestures
                    val pad=size.width*.035f;val unit=(size.width-2*pad)/current.size
                    val x=floor((point.x-pad)/unit).toInt();val y=floor((point.y-pad)/unit).toInt()
                    if(x !in 0 until current.size || y !in 0 until current.size)return@detectTapGestures
                    val index=y*current.size+x
                    if(current.board[index]<0)return@detectTapGestures
                    if(BlastGame.group(current,index).size<2 && current.board[index]<10){selected=index;shake++;return@detectTapGestures}
                    vm.answer(index)
                }
            }) {
                val pad=size.width*.035f;val unit=(size.width-2*pad)/s.size;val radius=unit*.445f
                val t=transition.value;val falling=((t-.22f)/.78f).coerceIn(0f,1f);val eased=FastOutSlowInEasing.transform(falling)
                val halo=BlastGame.group(s,selected)
                repeat(s.size*s.size){i->val center=Offset(pad+(i%s.size+.5f)*unit,pad+(i/s.size+.5f)*unit);drawCircle(Color.Black.copy(alpha=.12f),radius,center)}
                s.board.forEachIndexed {i,value->
                    val col=i%s.size;val row=i/s.size
                    val origin=if(s.moves>0 && s.fallOrigins.size==s.board.size)s.fallOrigins[i] else i
                    val originRow=if(origin>=0)origin/s.size else origin
                    val y=if(t<1f)originRow+(row-originRow)*eased else row.toFloat()
                    var center=Offset(pad+(col+.5f)*unit,pad+(y+.5f)*unit)
                    if(i==selected && shakeProgress.value<1f)center+=Offset(sin(shakeProgress.value*PI*6).toFloat()*unit*.08f,0f)
                    if(value<0) {
                        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF566579),Color(0xFF1D2937))),center-Offset(radius*.8f,radius*.8f),Size(radius*1.6f,radius*1.6f),CornerRadius(radius*.25f))
                        drawLine(Color.White.copy(alpha=.15f),center-Offset(radius*.5f,radius*.4f),center+Offset(radius*.45f,-radius*.4f),2.dp.toPx())
                    } else {
                        if(i in halo && halo.size>1)drawCircle(palette[value%10].copy(alpha=.2f),radius*1.14f,center)
                        drawBubble(center,radius,palette[value%10],value/10)
                    }
                }
                if(t<.6f && s.moves>0) {
                    val burst=(t/.6f).coerceIn(0f,1f);val opacity=1f-burst
                    s.cleared.forEach {i->
                        val center=Offset(pad+(i%s.size+.5f)*unit,pad+(i/s.size+.5f)*unit);val c=palette[s.burstColors[i]?:0]
                        drawCircle(c.copy(alpha=opacity*.7f),radius*(.6f+burst*1.2f),center,style=Stroke(radius*.06f))
                        repeat(8){p->val angle=p*PI/4+i*.37;val distance=radius*(.3f+burst*1.8f);drawCircle(c.copy(alpha=opacity),radius*.1f,center+Offset(cos(angle).toFloat()*distance,sin(angle).toFloat()*distance))}
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            Text(label("Collect the target colors","İstenen renkleri patlat"),color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Medium)
            Text("  ·  ",color=Muted)
            Text(label("Tap to pop","Dokun ve patlat"),color=Muted,fontSize=13.sp)
        }
        Text(label("5+ creates a cross blast. 8+ clears a whole color.","5+ çapraz patlatma, 8+ tüm rengi patlatır."),color=Muted,fontSize=11.sp)
    }
}
