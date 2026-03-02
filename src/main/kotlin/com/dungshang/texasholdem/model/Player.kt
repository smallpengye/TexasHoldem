package com.dungshang.texasholdem.model

/**
 * 玩家模型
 * 
 * @property id 玩家唯一标识
 * @property name 玩家名称
 * @property chips 玩家筹码数量
 * @property holeCards 手牌（2张）
 * @property currentBet 当前轮下注额
 * @property totalBetInRound 本手牌总下注额
 * @property isFolded 是否弃牌
 * @property isAllIn 是否全下
 * @property isBot 是否是机器人
 * @property aggressiveness 松紧度 1-10（1最紧，10最松）
 * @property isDealer 是否是庄家
 */
data class Player(
    val id: Int,
    var name: String,
    var chips: Int,
    var holeCards: MutableList<Card> = mutableListOf(),
    var currentBet: Int = 0,
    var totalBetInRound: Int = 0,
    var isFolded: Boolean = false,
    var isAllIn: Boolean = false,
    var isBot: Boolean = false,
    var aggressiveness: Int = 5,
    var isDealer: Boolean = false,
    var hasActedThisRound: Boolean = false
) {
    /**
     * 重置每手牌的状态
     * 清空手牌、重置下注等，但不重置筹码
     */
    fun reset() {
        holeCards.clear()
        currentBet = 0
        totalBetInRound = 0
        isFolded = false
        isAllIn = false
        isDealer = false
        hasActedThisRound = false
    }

    /**
     * 下注
     * 
     * @param amount 下注金额
     * @return 实际下注额（不能超过筹码）
     */
    fun bet(amount: Int): Int {
        val actualBet = minOf(amount, chips)
        chips -= actualBet
        currentBet += actualBet
        totalBetInRound += actualBet
        
        if (chips == 0) {
            isAllIn = true
        }
        
        return actualBet
    }
}
