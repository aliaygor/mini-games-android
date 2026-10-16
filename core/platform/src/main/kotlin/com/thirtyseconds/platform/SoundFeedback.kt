package com.thirtyseconds.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class SoundFeedback(private val context:Context) {
    private val pool=SoundPool.Builder().setMaxStreams(4).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ready=mutableSetOf<Int>()
    private val hit=pool.load(context,R.raw.feedback_hit,1)
    private val pop=pool.load(context,R.raw.feedback_pop,1)
    private val miss=pool.load(context,R.raw.feedback_miss,1)
    private val complete=pool.load(context,R.raw.feedback_complete,1)
    private var musicPlayer:MediaPlayer?=null
    init {pool.setOnLoadCompleteListener{_,id,status->if(status==0)ready+=id}}
    fun play(success:Boolean,combo:Int=0){val id=if(success)hit else miss;if(id in ready)pool.play(id,.45f,.45f,1,0,if(success)(1f+combo.coerceAtMost(10)*.025f) else 1f)}
    fun pop(combo:Int=0){if(pop in ready)pool.play(pop,.6f,.6f,1,0,1f+combo.coerceAtMost(10)*.035f)}
    fun complete(){if(complete in ready)pool.play(complete,.5f,.5f,1,0,1f)}
    fun music(enabled:Boolean){if(enabled){if(musicPlayer==null)musicPlayer=MediaPlayer.create(context,R.raw.ambient_loop)?.apply{isLooping=true;setVolume(.3f,.3f)};musicPlayer?.start()}else musicPlayer?.pause()}
    fun close(){musicPlayer?.release();musicPlayer=null;pool.release()}
}
