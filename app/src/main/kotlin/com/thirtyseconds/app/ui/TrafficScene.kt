package com.thirtyseconds.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.thirtyseconds.app.R
import com.thirtyseconds.app.GameViewModel
import com.thirtyseconds.domain.*
import kotlin.math.*

internal fun DrawScope.drawVehicle(bitmap:ImageBitmap,center:Offset,width:Float,length:Float,horizontal:Boolean) {
    rotate(if(horizontal)90f else 0f,center) {
        drawImage(bitmap,dstOffset=IntOffset((center.x-width/2).roundToInt(),(center.y-length/2).roundToInt()),dstSize=IntSize(width.roundToInt().coerceAtLeast(1),length.roundToInt().coerceAtLeast(1)))
    }
}

@Composable internal fun TrafficScene(s:GameState,vm:GameViewModel) {
    var selected by remember(s.seed){mutableIntStateOf(0)}
    val red=ImageBitmap.imageResource(R.drawable.vehicle_red)
    val blue=ImageBitmap.imageResource(R.drawable.vehicle_blue)
    val van=ImageBitmap.imageResource(R.drawable.vehicle_van)
    val car=s.vehicles.first {it.id==selected}
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Column{Text(label("MOVES","HAMLE"),color=Muted,fontSize=10.sp,letterSpacing=1.sp);Text("${s.moves} / ${s.moveLimit}",fontSize=22.sp,color=Color.White,fontWeight=FontWeight.Bold)}
            Column(horizontalAlignment=Alignment.End){Text(label("OPTIMAL ROUTE","EN KISA ÇÖZÜM"),color=Muted,fontSize=10.sp,letterSpacing=1.sp);Text("${s.optimalMoves}",fontSize=22.sp,color=Yellow,fontWeight=FontWeight.Bold)}
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            val outer=minOf(maxWidth,maxHeight);val edge=outer*.88f;val unit=edge/6;val margin=(outer-edge)/2
            Box(Modifier.size(outer).clip(RoundedCornerShape(24.dp)).background(Color(0xFF182E29))) {
                Canvas(Modifier.fillMaxSize()) {
                    val inset=size.width*.06f;val road=size.width*.88f;val cell=road/6
                    drawRoundRect(Color(0xFF0B121C),Offset(inset-3.dp.toPx(),inset-3.dp.toPx()),Size(road+6.dp.toPx(),road+6.dp.toPx()),CornerRadius(8.dp.toPx()))
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF42494E),Color(0xFF282F34))),Offset(inset,inset),Size(road,road))
                    // Restrained asphalt grain, painted bays and curb stones.
                    repeat(460){i->val x=(i*73%997)/997f;val y=(i*137%991)/991f;drawCircle(Color.White.copy(alpha=.06f),.6.dp.toPx(),Offset(inset+x*road,inset+y*road))}
                    repeat(7){i->drawLine(Color(0xFFE2E3CC).copy(alpha=.24f),Offset(inset+i*cell,inset),Offset(inset+i*cell,inset+road),1.dp.toPx());drawLine(Color(0xFFE2E3CC).copy(alpha=.24f),Offset(inset,inset+i*cell),Offset(inset+road,inset+i*cell),1.dp.toPx())}
                    repeat(18){i->val c=if(i%2==0)Color(0xFFBEC3BD) else Color(0xFF656D69);drawRect(c,Offset(inset+i*road/18,inset-5.dp.toPx()),Size(road/18-1f,4.dp.toPx()));drawRect(c,Offset(inset+i*road/18,inset+road+1.dp.toPx()),Size(road/18-1f,4.dp.toPx()))}
                    repeat(5){i->val x=if(i%2==0)size.width*.025f else size.width*.975f;val y=size.height*(.1f+i*.19f);if(i!=2) {drawCircle(Color(0xFF0D2117),size.width*.027f,Offset(x,y));drawCircle(Color(0xFF356447),size.width*.022f,Offset(x-2f,y-3f));drawCircle(Color(0xFF55845B),size.width*.011f,Offset(x-3f,y-6f))}}
                    val exit=Offset(inset+road,inset+cell*2)
                    drawRect(Color(0xFF457264),exit,Size(inset,cell))
                    drawLine(Lime,exit,exit+Offset(0f,cell),3.dp.toPx())
                    val mid=exit+Offset(inset*.5f,cell*.5f);drawLine(Color.White,mid-Offset(inset*.3f,0f),mid+Offset(inset*.25f,0f),2.dp.toPx());drawLine(Color.White,mid+Offset(inset*.25f,0f),mid+Offset(0f,-inset*.25f),2.dp.toPx());drawLine(Color.White,mid+Offset(inset*.25f,0f),mid+Offset(0f,inset*.25f),2.dp.toPx())
                }
                s.vehicles.forEach {v->key(v.id) {
                    val x by animateDpAsState(unit*v.x,tween(150),label="vehicle-x")
                    val y by animateDpAsState(unit*v.y,tween(150),label="vehicle-y")
                    Canvas(Modifier.offset(margin+x,margin+y).width(unit*(if(v.horizontal)v.length else 1)).height(unit*(if(v.horizontal)1 else v.length)).semantics{contentDescription="Vehicle ${v.id+1}"}.clickable{selected=v.id}.pointerInput(v.id,unit) {
                        var travel=0f
                        detectDragGestures(onDragStart={selected=v.id;travel=0f}){change,delta->
                            change.consume();travel+=if(v.horizontal)delta.x else delta.y
                            val step=unit.toPx()
                            while(abs(travel)>=step){val direction=if(travel>0)1 else 0;vm.answer(v.id*10+direction);travel-=if(direction==1)step else -step}
                        }
                    }) {
                        if(selected==v.id)drawRoundRect((if(v.id==0)Coral else Blue).copy(alpha=.22f),Offset(3.dp.toPx(),3.dp.toPx()),Size(size.width-6.dp.toPx(),size.height-6.dp.toPx()),CornerRadius(8.dp.toPx()))
                        drawVehicle(if(v.length==3)van else if(v.id==0)red else blue,Offset(size.width/2,size.height/2),unit.toPx()*.9f,unit.toPx()*v.length*.93f,v.horizontal)
                    }
                }}
            }
        }
        Text(label("Slide the cars. Free the red one.","Arabaları kaydır. Kırmızıya yol aç."),color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Medium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Tile(if(car.horizontal)"←" else "↑",Blue,Modifier.weight(1f).height(48.dp)){vm.answer(selected*10)}
            Tile(if(car.horizontal)"→" else "↓",Blue,Modifier.weight(1f).height(48.dp)){vm.answer(selected*10+1)}
        }
    }
}
