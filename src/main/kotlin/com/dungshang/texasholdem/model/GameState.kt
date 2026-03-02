package com.dungshang.texasholdem.model

/**
 * 游戏阶段
 */
enum class GamePhase {
    WAITING,      // 等待开始
    PRE_FLOP,     // 翻牌前（发手牌）
    FLOP,         // 翻牌（发3张公共牌）
    TURN,         // 转牌（发第4张公共牌）
    RIVER,        // 河牌（发第5张公共牌）
    SHOWDOWN,     // 摊牌
    GAME_OVER     // 游戏结束
}

/**
 * 玩家动作
 * 
 * @property displayName 动作显示名称
 */
enum class PlayerAction(val displayName: String) {
    FOLD("弃牌"),
    CHECK("过牌"),
    CALL("跟注"),
    RAISE("加注"),
    ALL_IN("全下")
}

/**
 * 动作记录
 * 
 * @property player 执行动作的玩家
 * @property action 动作类型
 * @property amount 动作涉及的金额
 * @property phase 动作发生的游戏阶段
 */
data class ActionRecord(
    val player: Player,
    val action: PlayerAction,
    val amount: Int,
    val phase: GamePhase
)

/**
 * 游戏状态
 * 
 * 管理德州扑克游戏的所有状态信息，包括玩家、公共牌、底池、当前阶段等
 */
class GameState {
    var players: MutableList<Player> = mutableListOf()
    var communityCards: MutableList<Card> = mutableListOf()
    var pot: Int = 0
    var currentPhase: GamePhase = GamePhase.WAITING
    var currentPlayerIndex: Int = 0
    var dealerIndex: Int = 0
    var smallBlind: Int = 10
    var bigBlind: Int = 20
    var currentBet: Int = 0
    var deck: Deck = Deck()
    var actionHistory: MutableList<ActionRecord> = mutableListOf()
    var lastRaiseAmount: Int = 0

    /**
     * 获取未弃牌的活跃玩家
     * 
     * @return 活跃玩家列表
     */
    fun getActivePlayers(): List<Player> {
        return players.filter { !it.isFolded }
    }

    /**
     * 获取下一个活跃玩家的索引
     * 
     * @param fromIndex 起始索引
     * @return 下一个活跃玩家的索引
     */
    fun getNextActivePlayerIndex(fromIndex: Int): Int {
        val activeCount = getActivePlayers().size
        if (activeCount <= 1) {
            return -1
        }

        var nextIndex = (fromIndex + 1) % players.size
        var attempts = 0
        
        while (attempts < players.size) {
            val player = players[nextIndex]
            if (!player.isFolded && !player.isAllIn) {
                return nextIndex
            }
            nextIndex = (nextIndex + 1) % players.size
            attempts++
        }
        
        return -1
    }

    /**
     * 判断当前轮是否结束
     * 
     * @return true 如果所有活跃玩家下注相同或全下
     */
    fun isRoundComplete(): Boolean {
        val activePlayers = getActivePlayers()
        if (activePlayers.size <= 1) {
            return true
        }

        val allInPlayers = activePlayers.filter { it.isAllIn }
        val nonAllInPlayers = activePlayers.filter { !it.isAllIn }

        if (nonAllInPlayers.isEmpty()) {
            return true
        }

        val allActed = nonAllInPlayers.all { it.hasActedThisRound }
        if (!allActed) {
            return false
        }

        val firstBet = nonAllInPlayers[0].currentBet
        return nonAllInPlayers.all { it.currentBet == firstBet }
    }

    /**
     * 重置游戏状态
     * 准备开始新一手牌
     */
    fun reset() {
        communityCards.clear()
        pot = 0
        currentPhase = GamePhase.WAITING
        currentBet = 0
        deck = Deck()
        actionHistory.clear()
        lastRaiseAmount = 0
        
        players.forEach { it.reset() }
    }
}
