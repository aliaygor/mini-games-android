package com.thirtyseconds.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.thirtyseconds.app.R
import com.thirtyseconds.domain.*
import kotlin.math.*

internal val Ink=Color(0xFF050B18)
internal val Panel=Color(0xFF0D1B30)
internal val Muted=Color(0xFF94A7C1)
internal val Lime=Color(0xFF2BDE94)
internal val Violet=Color(0xFFA372FF)
internal val Coral=Color(0xFFFF526D)
internal val Blue=Color(0xFF31C9FF)
internal val Yellow=Color(0xFFFFCB62)
internal val palette=listOf(Blue,Coral,Lime,Yellow,Violet,Color(0xFFFF8D40))
internal fun accent(theme: String)=when(theme){"violet"->Violet;"coral"->Coral;else->Blue}
@Composable internal fun label(en: String,tr: String)=if(LocalConfiguration.current.locales[0].language=="tr")tr else en
internal fun name(kind: GameKind)=when(kind){GameKind.BLAST->R.string.game_blast;GameKind.TRAFFIC->R.string.game_traffic;GameKind.DIFFERENCE->R.string.game_difference;GameKind.DOORS->R.string.game_doors;GameKind.COLOR->R.string.game_color;GameKind.REFLEX->R.string.game_reflex;GameKind.MATH->R.string.game_math;GameKind.MEMORY->R.string.game_memory;GameKind.PRECISION->R.string.game_precision;GameKind.SEQUENCE->R.string.game_sequence;GameKind.WORD->R.string.game_word;GameKind.MERGE->R.string.game_merge;GameKind.PATTERN->R.string.game_pattern;GameKind.SUDOKU->R.string.game_sudoku;GameKind.LOGIC->R.string.game_logic}
internal fun rule(kind: GameKind)=when(kind){GameKind.BLAST->R.string.rule_blast;GameKind.TRAFFIC->R.string.rule_traffic;GameKind.DIFFERENCE->R.string.rule_difference;GameKind.DOORS->R.string.rule_doors;GameKind.COLOR->R.string.rule_color;GameKind.REFLEX->R.string.rule_reflex;GameKind.MATH->R.string.rule_math;GameKind.MEMORY->R.string.rule_memory;GameKind.PRECISION->R.string.rule_precision;GameKind.SEQUENCE->R.string.rule_sequence;GameKind.WORD->R.string.rule_word;GameKind.MERGE->R.string.rule_merge;GameKind.PATTERN->R.string.rule_pattern;GameKind.SUDOKU->R.string.rule_sudoku;GameKind.LOGIC->R.string.rule_logic}
internal fun gameColor(kind: GameKind)=palette[kind.ordinal%palette.size]
internal fun colorName(index: Int)=listOf(R.string.blue,R.string.red,R.string.green,R.string.yellow,R.string.violet,R.string.coral)[index.coerceIn(0,5)]
@Composable internal fun ArenaBackground(menu: Boolean,sky: Boolean=false,theme: String="lime") {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF070C1E),Ink,Color(0xFF051726))))
        drawCircle(Brush.radialGradient(listOf(accent(theme).copy(alpha=.09f),Color.Transparent),Offset(size.width*.95f,size.height*.25f),size.width*.8f),radius=size.width*.8f,center=Offset(size.width*.95f,size.height*.25f))
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha=.06f),Color.Transparent),Offset(0f,size.height*.7f),size.width*.7f),radius=size.width*.7f,center=Offset(0f,size.height*.7f))
    }
}
@Composable internal fun GameText(text: String,modifier: Modifier=Modifier,size: Int=22,color: Color=Color.White,align: TextAlign=TextAlign.Center) {
    Text(text,modifier,color=color,fontSize=size.sp,fontWeight=FontWeight.SemiBold,textAlign=align,lineHeight=(size+5).sp)
}
@Composable internal fun ArenaCard(modifier: Modifier=Modifier,color: Color=Blue,onClick: (() -> Unit)?=null,enabled: Boolean=true,content: @Composable BoxScope.()->Unit) {
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).98f else 1f,spring(stiffness=700f),label="press")
    val shape=RoundedCornerShape(18.dp)
    Box(modifier.scale(scale).clip(shape).background(Brush.linearGradient(listOf(Panel,color.copy(alpha=if(enabled).13f else .04f))))
        .border(1.dp,color.copy(alpha=if(enabled).28f else .12f),shape)
        .then(if(onClick!=null)Modifier.clickable(enabled=enabled,role=Role.Button,interactionSource=source,indication=null,onClick=onClick) else Modifier),content=content)
}
@Composable internal fun Pill(text: String,color: Color=Violet) {ArenaCard(color=color){Text(text,Modifier.padding(horizontal=10.dp,vertical=7.dp),color=color,fontSize=11.sp,lineHeight=15.sp,fontWeight=FontWeight.Medium)}}
@Composable internal fun Heading(title: String,subtitle: String) {Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)){GameText(title,size=26,align=TextAlign.Start);Text(subtitle,color=Muted,fontSize=13.sp,lineHeight=20.sp)}}
@Composable internal fun Action(label: String,enabled: Boolean=true,onClick:()->Unit) {
    val colors=listOf(Violet,Color(0xFF4573EF),Blue)
    Box(Modifier.fillMaxWidth().heightIn(min=52.dp).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(if(enabled)colors else listOf(Panel,Panel))).clickable(enabled=enabled,role=Role.Button,onClick=onClick).padding(14.dp),contentAlignment=Alignment.Center){GameText(label,size=15,color=if(enabled)Color.White else Muted)}
}
@Composable internal fun Panel(content: @Composable ColumnScope.()->Unit) {ArenaCard(Modifier.fillMaxWidth(),Blue){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable internal fun Stat(value: String,label: String,modifier: Modifier=Modifier,color: Color=Color.White) {Column(modifier.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=Muted,fontSize=10.sp,lineHeight=13.sp);GameText(value,size=19,color=color)}}
@Composable internal fun Tile(text: String,color: Color,modifier: Modifier,enabled: Boolean=true,onClick:()->Unit) {
    val shape=RoundedCornerShape(8.dp)
    Box(modifier.clip(shape).background(Brush.verticalGradient(listOf(color.copy(alpha=if(enabled).85f else .15f),color.copy(alpha=if(enabled).45f else .08f))))
        .border(1.dp,color.copy(alpha=if(enabled).8f else .2f),shape).clickable(enabled=enabled,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center){GameText(text,size=if(text.length>3)18 else 24)}
}
@Composable internal fun Setting(label: String,checked: Boolean,change:(Boolean)->Unit) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),color=Muted,fontSize=14.sp);Switch(checked,change,colors=SwitchDefaults.colors(checkedTrackColor=Blue,checkedThumbColor=Ink))}}
@Composable internal fun GameArt(kind: GameKind,modifier: Modifier) {
    if(kind==GameKind.TRAFFIC) {
        val red=ImageBitmap.imageResource(R.drawable.vehicle_red)
        val blue=ImageBitmap.imageResource(R.drawable.vehicle_blue)
        val van=ImageBitmap.imageResource(R.drawable.vehicle_van)
        Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF343B40))) {
            val w=size.width;val h=size.height
            repeat(5){i->drawLine(Color.White.copy(alpha=.2f),Offset(w*i/4,0f),Offset(w*i/4,h),1.dp.toPx());drawLine(Color.White.copy(alpha=.2f),Offset(0f,h*i/4),Offset(w,h*i/4),1.dp.toPx())}
            drawVehicle(red,Offset(w*.43f,h*.57f),h*.28f,w*.48f,true)
            drawVehicle(blue,Offset(w*.75f,h*.5f),w*.19f,h*.82f,false)
            drawVehicle(van,Offset(w*.18f,h*.36f),w*.16f,h*.9f,false)
            drawVehicle(blue,Offset(w*.42f,h*.88f),h*.25f,w*.42f,true)
        }
        return
    }
    if(kind==GameKind.BLAST) {
        Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF163951),Color(0xFF071622))))) {
            val unit=min(size.width/5,size.height/3);val left=(size.width-unit*5)/2;val top=(size.height-unit*3)/2
            val colors=listOf(0,0,2,1,1,0,2,2,1,4,3,3,2,4,4)
            repeat(15){i->drawBubble(Offset(left+(i%5+.5f)*unit,top+(i/5+.5f)*unit),unit*.43f,palette[colors[i]],if(i==7)1 else 0)}
        }
        return
    }
    if(kind==GameKind.DIFFERENCE) {
        Image(painterResource(R.drawable.difference_cafe_a),null,modifier.clip(RoundedCornerShape(10.dp)),contentScale=ContentScale.Crop)
        return
    }
    val color=gameColor(kind)
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(color.copy(alpha=.16f),Ink)))) {
        val w=size.width;val h=size.height;val cell=min(w,h)*.16f
        when(kind) {
            GameKind.REFLEX,GameKind.PRECISION->{val center=Offset(w*.5f,h*.5f);for(i in 0..2)drawCircle(color.copy(alpha=.85f-i*.23f),min(w,h)*(.20f+i*.1f),center,style=Stroke(3.dp.toPx()));drawCircle(color,5.dp.toPx(),center)}
            GameKind.TRAFFIC->{repeat(5){i->drawLine(Muted.copy(alpha=.15f),Offset(w*.1f+i*w*.17f,0f),Offset(w*.1f+i*w*.17f,h),1f)};repeat(5){i->drawRoundRect(if(i==2)Coral else color.copy(alpha=.7f),Offset(w*(.12f+(i%3)*.24f),h*(.12f+(i/3)*.42f)),Size(w*.16f,h*.35f),CornerRadius(6f))}}
            GameKind.DOORS->{val points=listOf(Offset(w*.15f,h*.8f),Offset(w*.15f,h*.4f),Offset(w*.5f,h*.4f),Offset(w*.5f,h*.15f),Offset(w*.85f,h*.15f));points.zipWithNext().forEach{(a,b)->drawLine(color,a,b,5.dp.toPx())};drawCircle(Lime,7.dp.toPx(),points.last())}
            GameKind.MATH,GameKind.WORD,GameKind.SEQUENCE,GameKind.MERGE->{
                val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{this.color=android.graphics.Color.WHITE;textAlign=android.graphics.Paint.Align.CENTER;textSize=min(w,h)*.2f;typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL)}
                drawContext.canvas.nativeCanvas.drawText(when(kind){GameKind.MATH->"17 × 4 = ?";GameKind.WORD->"T R A E";GameKind.SEQUENCE->"2   8   4   6";else->"2   2   4   8"},w*.5f,h*.58f,paint)
                drawLine(color,Offset(w*.25f,h*.75f),Offset(w*.75f,h*.75f),2.dp.toPx())
            }
            else->{val grid=if(kind==GameKind.MEMORY || kind==GameKind.SUDOKU)4 else if(kind==GameKind.COLOR || kind==GameKind.BLAST)5 else 3;val left=(w-grid*cell)/2;val top=(h-grid*cell)/2;repeat(grid*grid){i->val c=if(kind==GameKind.COLOR || kind==GameKind.BLAST)palette[i%palette.size] else color;drawRoundRect(c.copy(alpha=if((i+kind.ordinal)%3==0).95f else .28f),Offset(left+i%grid*cell,top+i/grid*cell),Size(cell-3.dp.toPx(),cell-3.dp.toPx()),CornerRadius(4.dp.toPx()))}}
        }
    }
}
