package com.thirtyseconds.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.thirtyseconds.app.R
import com.thirtyseconds.app.GameViewModel
import com.thirtyseconds.app.UiState
import com.thirtyseconds.domain.*
import java.time.LocalDate
import java.time.ZoneOffset

@Composable internal fun BottomNav(screen: String,select:(String)->Unit) {
    Row(Modifier.fillMaxWidth().background(Ink.copy(alpha=.97f)).navigationBarsPadding().padding(horizontal=8.dp,vertical=6.dp)) {
        listOf("home","leaderboard","missions","profile").forEachIndexed{i,id->
            val text=listOf(label("Home","Ana sayfa"),label("Rankings","Sıralama"),label("Challenges","Mücadele"),label("Profile","Profil"))[i]
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(screen==id)Blue.copy(alpha=.1f) else Color.Transparent).clickable{select(id)}.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                val c=if(screen==id)Blue else Muted
                Canvas(Modifier.size(22.dp)) {
                    val w=size.width;val h=size.height;val line=Stroke(1.7.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round)
                    val path=Path()
                    when(i) {
                        0->{path.moveTo(w*.15f,h*.45f);path.lineTo(w*.5f,h*.14f);path.lineTo(w*.85f,h*.45f);path.moveTo(w*.24f,h*.4f);path.lineTo(w*.24f,h*.85f);path.lineTo(w*.76f,h*.85f);path.lineTo(w*.76f,h*.4f)}
                        1->{for(j in 0..2){val x=w*(.22f+j*.27f);drawLine(c,Offset(x,h*.8f),Offset(x,h*(.5f-j*.15f)),1.7.dp.toPx(),StrokeCap.Round)}}
                        2->{drawCircle(c,w*.32f,Offset(w*.5f,h*.5f),style=line);drawCircle(c,w*.12f,Offset(w*.5f,h*.5f),style=line);path.moveTo(w*.5f,h*.04f);path.lineTo(w*.5f,h*.25f);path.moveTo(w*.5f,h*.75f);path.lineTo(w*.5f,h*.96f);path.moveTo(w*.04f,h*.5f);path.lineTo(w*.25f,h*.5f);path.moveTo(w*.75f,h*.5f);path.lineTo(w*.96f,h*.5f)}
                        else->{drawCircle(c,w*.15f,Offset(w*.5f,h*.28f),style=line);path.moveTo(w*.2f,h*.86f);path.cubicTo(w*.2f,h*.48f,w*.8f,h*.48f,w*.8f,h*.86f)}
                    }
                    drawPath(path,c,style=line)
                }
                Text(text,fontSize=10.sp,color=if(screen==id)Color.White else Muted)
            }
        }
    }
}
