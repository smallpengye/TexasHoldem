package com.dungshang.texasholdem.model

object HandEvaluator {
    
    data class HandResult(
        val handRank: HandRank,
        val bestCards: List<Card>,
        val kickers: List<Int>
    ) : Comparable<HandResult> {
        
        override fun compareTo(other: HandResult): Int {
            if (this.handRank.value != other.handRank.value) {
                return this.handRank.value - other.handRank.value
            }
            
            for (i in this.kickers.indices) {
                if (i < other.kickers.size) {
                    val comparison = this.kickers[i] - other.kickers[i]
                    if (comparison != 0) {
                        return comparison
                    }
                }
            }
            
            return 0
        }
    }
    
    fun evaluate(holeCards: List<Card>, communityCards: List<Card>): HandResult {
        val allCards = holeCards + communityCards
        val combinations = generateCombinations(allCards, 5)
        
        var bestResult: HandResult? = null
        
        for (combination in combinations) {
            val result = evaluateFiveCards(combination)
            if (bestResult == null || result > bestResult) {
                bestResult = result
            }
        }
        
        return bestResult ?: throw IllegalStateException("Could not evaluate hand")
    }
    
    fun compareHands(result1: HandResult, result2: HandResult): Int {
        return result1.compareTo(result2)
    }
    
    private fun generateCombinations(cards: List<Card>, size: Int): List<List<Card>> {
        if (size > cards.size) {
            return emptyList()
        }
        
        if (size == cards.size) {
            return listOf(cards)
        }
        
        if (size == 1) {
            return cards.map { listOf(it) }
        }
        
        val combinations = mutableListOf<List<Card>>()
        for (i in 0..cards.size - size) {
            val first = cards[i]
            val remaining = cards.subList(i + 1, cards.size)
            val subCombinations = generateCombinations(remaining, size - 1)
            for (subCombination in subCombinations) {
                combinations.add(listOf(first) + subCombination)
            }
        }
        
        return combinations
    }
    
    private fun evaluateFiveCards(cards: List<Card>): HandResult {
        val sortedCards = cards.sortedByDescending { it.rank.value }
        val ranks = sortedCards.map { it.rank.value }
        val suits = sortedCards.map { it.suit }
        
        val isFlush = suits.all { it == suits[0] }
        val isStraight = isStraight(ranks)
        val rankCounts = ranks.groupingBy { it }.eachCount()
        val countValues = rankCounts.values.sortedDescending()
        
        val handRank = when {
            isFlush && isStraight && ranks[0] == Rank.ACE.value && ranks[4] == Rank.TEN.value -> HandRank.ROYAL_FLUSH
            isFlush && isStraight -> HandRank.STRAIGHT_FLUSH
            countValues[0] == 4 -> HandRank.FOUR_OF_A_KIND
            countValues[0] == 3 && countValues[1] == 2 -> HandRank.FULL_HOUSE
            isFlush -> HandRank.FLUSH
            isStraight -> HandRank.STRAIGHT
            countValues[0] == 3 -> HandRank.THREE_OF_A_KIND
            countValues[0] == 2 && countValues[1] == 2 -> HandRank.TWO_PAIR
            countValues[0] == 2 -> HandRank.ONE_PAIR
            else -> HandRank.HIGH_CARD
        }
        
        val kickers = calculateKickers(handRank, ranks, rankCounts)
        
        return HandResult(handRank, sortedCards, kickers)
    }
    
    private fun isStraight(ranks: List<Int>): Boolean {
        val sortedRanks = ranks.sorted()
        
        val normalStraight = (0 until sortedRanks.size - 1).all { i ->
            sortedRanks[i + 1] == sortedRanks[i] + 1
        }
        
        if (normalStraight) {
            return true
        }
        
        val wheelStraight = sortedRanks[0] == Rank.TWO.value &&
                           sortedRanks[1] == Rank.THREE.value &&
                           sortedRanks[2] == Rank.FOUR.value &&
                           sortedRanks[3] == Rank.FIVE.value &&
                           sortedRanks[4] == Rank.ACE.value
        
        return wheelStraight
    }
    
    private fun calculateKickers(handRank: HandRank, ranks: List<Int>, rankCounts: Map<Int, Int>): List<Int> {
        return when (handRank) {
            HandRank.ROYAL_FLUSH, HandRank.STRAIGHT_FLUSH -> {
                listOf(ranks[0])
            }
            HandRank.FOUR_OF_A_KIND -> {
                val quadRank = rankCounts.filter { it.value == 4 }.keys.first()
                val kicker = rankCounts.filter { it.value == 1 }.keys.first()
                listOf(quadRank, kicker)
            }
            HandRank.FULL_HOUSE -> {
                val tripleRank = rankCounts.filter { it.value == 3 }.keys.first()
                val pairRank = rankCounts.filter { it.value == 2 }.keys.first()
                listOf(tripleRank, pairRank)
            }
            HandRank.FLUSH, HandRank.STRAIGHT, HandRank.HIGH_CARD -> {
                ranks
            }
            HandRank.THREE_OF_A_KIND -> {
                val tripleRank = rankCounts.filter { it.value == 3 }.keys.first()
                val kickers = rankCounts.filter { it.value == 1 }.keys.sortedDescending()
                listOf(tripleRank) + kickers
            }
            HandRank.TWO_PAIR -> {
                val pairs = rankCounts.filter { it.value == 2 }.keys.sortedDescending()
                val kicker = rankCounts.filter { it.value == 1 }.keys.first()
                pairs + kicker
            }
            HandRank.ONE_PAIR -> {
                val pairRank = rankCounts.filter { it.value == 2 }.keys.first()
                val kickers = rankCounts.filter { it.value == 1 }.keys.sortedDescending()
                listOf(pairRank) + kickers
            }
        }
    }
}
