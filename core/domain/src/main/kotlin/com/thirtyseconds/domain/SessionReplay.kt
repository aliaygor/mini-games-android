package com.thirtyseconds.domain

sealed interface SessionCommand {
    data class Tick(val deltaMs:Long):SessionCommand
    data class Answer(val value:Int):SessionCommand
    data class Pointer(val x:Float,val y:Float):SessionCommand
}
data class SessionReplay(val kind:GameKind,val seed:Int,val level:Int,val language:String,val commands:List<SessionCommand>) {
    fun verify():GameState {
        require(commands.size<=8000)
        var s=GameEngine.start(kind,seed,level,language)
        commands.forEach{command->
            s=when(command){
                is SessionCommand.Tick->{require(command.deltaMs in 1..10_000);GameEngine.tick(s,command.deltaMs)}
                is SessionCommand.Answer->GameEngine.act(s,command.value)
                is SessionCommand.Pointer->{require(command.x.isFinite() && command.y.isFinite() && command.x in 0f..1f && command.y in 0f..1f);GameEngine.pointer(s,command.x,command.y)}
            }
        }
        return s
    }
}
