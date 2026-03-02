package com.dungshang.texasholdem.strategy

import com.dungshang.texasholdem.model.*
import kotlin.random.Random

/**
 * GTO (Game Theory Optimal) 策略引擎
 * 
 * 实现基于博弈论的最优策略，结合手牌强度评估、底池赔率分析和松紧度调整
 * 来做出最优决策。
 */
object GtoStrategy {
    
    /**
     * 决定玩家的下一步动作
     * 
     * @param player 当前行动的玩家
     * @param gameState 当前游戏状态
     * @return Pair<PlayerAction, Int> 动作和对应的金额（对于 FOLD/CHECK 金额为 0）
     */
    fun decideAction(player: Player, gameState: GameState): Pair<PlayerAction, Int> {
        // 计算需要跟注的金额
        val callAmount = gameState.currentBet - player.currentBet
        
        // 评估手牌强度
        val handStrength = evaluateHandStrength(player.holeCards, gameState.communityCards)
        
        // 根据玩家的侵略性调整决策阈值
        val foldThreshold = 0.35 - (player.aggressiveness - 5) * 0.04
        val raiseThreshold = 0.70 - (player.aggressiveness - 5) * 0.03
        val bluffFrequency = 0.05 + (player.aggressiveness - 1) * 0.03
        
        // 计算底池赔率
        val potOdds = if (gameState.pot + callAmount > 0) {
            callAmount.toDouble() / (gameState.pot + callAmount)
        } else {
            0.0
        }
        
        // 决策逻辑
        val randomValue = Random.nextDouble()
        
        return when {
            // 诈唬逻辑：手牌弱但随机值小于诈唬频率
            handStrength < foldThreshold && randomValue <= bluffFrequency -> {
                val raiseAmount = calculateRaiseAmount(gameState.pot, handStrength, gameState.lastRaiseAmount, gameState.bigBlind)
                if (raiseAmount >= player.chips * 0.8) {
                    PlayerAction.ALL_IN to player.chips
                } else {
                    PlayerAction.RAISE to raiseAmount
                }
            }
            
            // 弱牌且不诈唬：弃牌
            handStrength < foldThreshold && randomValue > bluffFrequency -> {
                PlayerAction.FOLD to 0
            }
            
            // 不需要跟注时
            callAmount == 0 -> {
                if (handStrength >= raiseThreshold) {
                    val raiseAmount = calculateRaiseAmount(gameState.pot, handStrength, gameState.lastRaiseAmount, gameState.bigBlind)
                    if (raiseAmount >= player.chips * 0.8) {
                        PlayerAction.ALL_IN to player.chips
                    } else {
                        PlayerAction.RAISE to raiseAmount
                    }
                } else {
                    PlayerAction.CHECK to 0
                }
            }
            
            // 手牌足够强：加注
            handStrength >= raiseThreshold -> {
                val raiseAmount = calculateRaiseAmount(gameState.pot, handStrength, gameState.lastRaiseAmount, gameState.bigBlind)
                if (raiseAmount >= player.chips * 0.8) {
                    PlayerAction.ALL_IN to player.chips
                } else {
                    PlayerAction.RAISE to raiseAmount
                }
            }
            
            // 手牌强度满足底池赔率：跟注
            handStrength >= potOdds -> {
                PlayerAction.CALL to callAmount
            }
            
            // 其他情况：弃牌
            else -> {
                PlayerAction.FOLD to 0
            }
        }
    }
    
    /**
     * 评估手牌强度
     * 
     * @param holeCards 玩家的手牌
     * @param communityCards 公共牌
     * @return 0.0 到 1.0 的强度值
     */
    fun evaluateHandStrength(holeCards: List<Card>, communityCards: List<Card>): Double {
        return when (communityCards.isEmpty()) {
            true -> evaluatePreFlopStrength(holeCards)
            false -> evaluatePostFlopStrength(holeCards, communityCards)
        }
    }
    
    /**
     * 评估 Pre-flop 阶段的手牌强度
     * 基于起手牌分组评估系统
     */
    private fun evaluatePreFlopStrength(cards: List<Card>): Double {
        if (cards.size != 2) return 0.0
        
        val card1 = cards[0]
        val card2 = cards[1]
        
        val isPair = card1.rank == card2.rank
        val isSuited = card1.suit == card2.suit
        val highCard = maxOf(card1.rank, card2.rank)
        val lowCard = minOf(card1.rank, card2.rank)
        val gap = highCard.value - lowCard.value
        
        return when {
            // 口袋对子
            isPair -> {
                when (highCard) {
                    Rank.ACE -> 1.0
                    Rank.KING -> 0.95
                    Rank.QUEEN -> 0.90
                    Rank.JACK -> 0.85
                    Rank.TEN -> 0.80
                    Rank.NINE -> 0.70
                    Rank.EIGHT -> 0.65
                    Rank.SEVEN -> 0.60
                    Rank.SIX -> 0.55
                    Rank.FIVE -> 0.50
                    Rank.FOUR -> 0.45
                    Rank.THREE -> 0.40
                    Rank.TWO -> 0.35
                }
            }
            
            // 同花连牌
            isSuited && gap <= 2 -> {
                when (highCard) {
                    Rank.ACE -> 0.90
                    Rank.KING -> 0.80
                    Rank.QUEEN -> 0.75
                    Rank.JACK -> 0.70
                    Rank.TEN -> 0.65
                    Rank.NINE -> 0.60
                    Rank.EIGHT -> 0.55
                    else -> 0.50
                }
            }
            
            // 非同花连牌
            !isSuited && gap <= 2 -> {
                when (highCard) {
                    Rank.ACE -> 0.82
                    Rank.KING -> 0.72
                    Rank.QUEEN -> 0.65
                    Rank.JACK -> 0.60
                    Rank.TEN -> 0.55
                    else -> 0.50
                }
            }
            
            // A 带小牌同花
            isSuited && highCard == Rank.ACE -> {
                when (lowCard) {
                    Rank.KING -> 0.88
                    Rank.QUEEN -> 0.85
                    Rank.JACK -> 0.80
                    Rank.TEN -> 0.75
                    Rank.NINE -> 0.70
                    Rank.EIGHT -> 0.65
                    Rank.SEVEN -> 0.60
                    Rank.SIX -> 0.58
                    Rank.FIVE -> 0.56
                    else -> 0.55
                }
            }
            
            // 其他同花牌
            isSuited -> {
                val baseValue = (highCard.value + lowCard.value) / 28.0
                baseValue * 0.75
            }
            
            // 其他非同花牌
            else -> {
                val baseValue = (highCard.value + lowCard.value) / 28.0
                baseValue * 0.60
            }
        }
    }
    
    /**
     * 评估 Post-flop 阶段的手牌强度
     * 使用 HandEvaluator 评估当前最佳牌型
     */
    private fun evaluatePostFlopStrength(holeCards: List<Card>, communityCards: List<Card>): Double {
        val result = HandEvaluator.evaluate(holeCards, communityCards)
        
        // 基于牌型等级的基础强度
        val baseStrength = when (result.handRank) {
            HandRank.ROYAL_FLUSH -> 1.0
            HandRank.STRAIGHT_FLUSH -> 0.98
            HandRank.FOUR_OF_A_KIND -> 0.95
            HandRank.FULL_HOUSE -> 0.90
            HandRank.FLUSH -> 0.85
            HandRank.STRAIGHT -> 0.80
            HandRank.THREE_OF_A_KIND -> 0.70
            HandRank.TWO_PAIR -> 0.60
            HandRank.ONE_PAIR -> 0.45
            HandRank.HIGH_CARD -> 0.25
        }
        
        // 根据具体牌面微调强度
        return adjustStrengthByBoard(baseStrength, result.handRank, holeCards, communityCards)
    }
    
    /**
     * 根据具体牌面情况微调手牌强度
     */
    private fun adjustStrengthByBoard(
        baseStrength: Double,
        handRank: HandRank,
        holeCards: List<Card>,
        communityCards: List<Card>
    ): Double {
        // 获取最佳牌中的手牌部分
        val bestHoleCards = holeCards.filter { card ->
            communityCards.none { it.rank == card.rank && it.suit == card.suit }
        }
        
        return when (handRank) {
            HandRank.ONE_PAIR -> {
                // 顶对 vs 底对
                val pairRank = communityCards.maxByOrNull { card ->
                    communityCards.count { it.rank == card.rank }
                }?.rank ?: Rank.TWO
                
                val pairValue = pairRank.value / 14.0
                baseStrength + (pairValue - 0.5) * 0.15
            }
            
            HandRank.TWO_PAIR -> {
                // 根据两对的强度调整
                val topPairValue = communityCards.maxByOrNull { card ->
                    communityCards.count { it.rank == card.rank }
                }?.rank?.value ?: 2
                
                baseStrength + (topPairValue - 7) * 0.03
            }
            
            HandRank.THREE_OF_A_KIND -> {
                // 根据三条的强度调整
                val tripsRank = communityCards.find { card ->
                    communityCards.count { it.rank == card.rank } >= 3
                }?.rank ?: Rank.TWO
                
                val tripsValue = tripsRank.value / 14.0
                baseStrength + (tripsValue - 0.5) * 0.10
            }
            
            else -> baseStrength
        }.coerceIn(0.0, 1.0)
    }
    
    /**
     * 计算加注金额
     * 
     * @param pot 当前底池大小
     * @param handStrength 手牌强度
     * @param lastRaiseAmount 上一次加注金额
     * @param bigBlind 大盲注金额
     * @return 加注金额
     */
    private fun calculateRaiseAmount(
        pot: Int,
        handStrength: Double,
        lastRaiseAmount: Int,
        bigBlind: Int
    ): Int {
        val baseRaise = when {
            handStrength > 0.85 -> {
                // 强牌：加注底池的 75% 到 150%
                (pot * Random.nextDouble(0.75, 1.5)).toInt()
            }
            handStrength >= 0.6 -> {
                // 中等牌：加注底池的 50% 到 75%
                (pot * Random.nextDouble(0.5, 0.75)).toInt()
            }
            else -> {
                // 诈唬：加注底池的 50% 到 75%
                (pot * Random.nextDouble(0.5, 0.75)).toInt()
            }
        }
        
        // 确保加注额不小于最小加注额
        val minRaise = maxOf(lastRaiseAmount, bigBlind)
        return maxOf(baseRaise, minRaise)
    }
}