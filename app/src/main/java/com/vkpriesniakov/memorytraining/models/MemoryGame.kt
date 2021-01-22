package com.vkpriesniakov.memorytraining.models

import com.vkpriesniakov.memorytraining.utils.DEFAULT_ICONS_LIST

class MemoryGame(private val boardSize: BoardSize, private val customImages: List<String>?){
    val cards:List<MemoryCard>
    var numPairsFound = 0
    private var numCardsFlips = 0

    private var indexOfSingleSelectedCard:Int? = null

    init {
        if (customImages == null) {
            val chosenImages = DEFAULT_ICONS_LIST.shuffled().take(boardSize.getNumPairs())
            val randomizedImages = (chosenImages + chosenImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        }else{
            val randomizedImages = (customImages+customImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(), it)}
        }
    }

    fun flipCard(position: Int):Boolean {
        numCardsFlips++
        val card = cards[position]
        var foundMatch = false

        if(indexOfSingleSelectedCard == null){
            //0 or 2 cards were flipped over
            restoreCards()
            indexOfSingleSelectedCard = position
        } else{
            //1 card flipped over
                foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }

        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
       return if (cards[position1].identifier != cards[position2].identifier ){
            false
        } else{
            cards[position1].isMatch = true
            cards[position2].isMatch = true
            numPairsFound++
            true
       }
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatch) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        return numCardsFlips / 2
    }
}
