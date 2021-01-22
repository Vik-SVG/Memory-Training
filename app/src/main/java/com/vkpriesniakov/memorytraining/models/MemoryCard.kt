package com.vkpriesniakov.memorytraining.models

data class MemoryCard (
        val identifier:Int,
        val imageUrs:String? = null,
        var isFaceUp:Boolean = false,
        var isMatch:Boolean = false
        ){
}