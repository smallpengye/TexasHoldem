package com.dungshang.texasholdem.model

/**
 * 手牌评估结果
 * 
 * @property handRank 牌型等级
 * @property bestCards 构成最佳牌型的牌
 * @property kickers 用于比较的踢脚牌
 */
data class HandResult(
    val handRank: HandRank,
    val bestCards: List<Card>,
    val kickers: List<Int>
) : Comparable<HandResult> {
    
    /**
     * 比较两个手牌结果
     * 先比较牌型等级，再比较踢脚牌
     */
    override fun compareTo(other: HandResult): Int {
        // 先比较牌型等级
        val rankComparison = this.handRank.value.compareTo(other.handRank.value)
        if (rankComparison != 0) {
            return rankComparison
        }
        
        // 牌型相同，比较踢脚牌
        for (i in this.kickers.indices) {
            if (i >= other.kickers.size) break
            val kickerComparison = this.kickers[i].compareTo(other.kickers[i])
            if (kickerComparison != 0) {
                return kickerComparison
            }
        }
        
        return 0
    }
}
