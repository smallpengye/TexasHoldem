package com.dungshang.texasholdem.model

import kotlin.random.Random

class Deck {
    private val cards: MutableList<Card> = mutableListOf()

    init {
        reset()
    }

    fun shuffle() {
        cards.shuffle(Random.Default)
    }

    fun deal(): Card {
        if (cards.isEmpty()) {
            throw IllegalStateException("No cards left in the deck")
        }
        return cards.removeAt(cards.size - 1)
    }

    fun dealMultiple(count: Int): List<Card> {
        if (count > cards.size) {
            throw IllegalStateException("Not enough cards in the deck. Requested: $count, Available: ${cards.size}")
        }
        val dealtCards = mutableListOf<Card>()
        repeat(count) {
            dealtCards.add(deal())
        }
        return dealtCards
    }

    fun reset() {
        cards.clear()
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                cards.add(Card(rank, suit))
            }
        }
    }

    fun remainingCards(): Int {
        return cards.size
    }
}