package com.dungshang.texasholdem.model
import com.dungshang.texasholdem.i18n.I18n

enum class HandRank(val value: Int, val displayName: String, val i18nKey: String) {
    ROYAL_FLUSH(10, "Royal Flush", "hand.royal_flush"),
    STRAIGHT_FLUSH(9, "Straight Flush", "hand.straight_flush"),
    FOUR_OF_A_KIND(8, "Four of a Kind", "hand.four_of_a_kind"),
    FULL_HOUSE(7, "Full House", "hand.full_house"),
    FLUSH(6, "Flush", "hand.flush"),
    STRAIGHT(5, "Straight", "hand.straight"),
    THREE_OF_A_KIND(4, "Three of a Kind", "hand.three_of_a_kind"),
    TWO_PAIR(3, "Two Pair", "hand.two_pair"),
    ONE_PAIR(2, "One Pair", "hand.one_pair"),
    HIGH_CARD(1, "High Card", "hand.high_card");

    val localizedName: String get() = I18n.get(i18nKey)
}