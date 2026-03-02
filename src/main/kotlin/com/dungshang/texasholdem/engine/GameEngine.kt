package com.dungshang.texasholdem.engine

import com.dungshang.texasholdem.model.*
import com.dungshang.texasholdem.strategy.GtoStrategy
import com.dungshang.texasholdem.i18n.I18n
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

class GameEngine {
    val gameState = GameState()
    private val listeners = mutableListOf<GameEventListener>()
    private val botExecutor = Executors.newSingleThreadExecutor()

    interface GameEventListener {
        fun onGameStateChanged()
        fun onPlayerAction(player: Player, action: PlayerAction, amount: Int)
        fun onPhaseChanged(phase: GamePhase)
        fun onGameMessage(message: String)
        fun onShowdown(results: List<ShowdownResult>)
        fun onGameOver(winner: Player)
    }

    data class ShowdownResult(
        val player: Player,
        val handResult: HandEvaluator.HandResult,
        val winAmount: Int
    )

    data class BotConfig(
        val name: String,
        val chips: Int,
        val aggressiveness: Int
    )

    fun addListener(listener: GameEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GameEventListener) {
        listeners.remove(listener)
    }

    fun setupGame(humanPlayerName: String, humanChips: Int, botConfigs: List<BotConfig>) {
        gameState.players.clear()
        
        val humanPlayer = Player(
            id = 0,
            name = humanPlayerName,
            chips = humanChips,
            holeCards = mutableListOf(),
            currentBet = 0,
            totalBetInRound = 0,
            isFolded = false,
            isAllIn = false,
            isBot = false,
            aggressiveness = 50,
            isDealer = false
        )
        gameState.players.add(humanPlayer)

        botConfigs.forEachIndexed { index, config ->
            val botPlayer = Player(
                id = index + 1,
                name = config.name,
                chips = config.chips,
                holeCards = mutableListOf(),
                currentBet = 0,
                totalBetInRound = 0,
                isFolded = false,
                isAllIn = false,
                isBot = true,
                aggressiveness = config.aggressiveness,
                isDealer = false
            )
            gameState.players.add(botPlayer)
        }

        gameState.dealerIndex = 0
        gameState.smallBlind = 10
        gameState.bigBlind = 20
        gameState.currentPhase = GamePhase.WAITING
        
        notifyMessage(I18n.get("engine.game_setup", gameState.players.size))
        notifyStateChanged()
    }

    fun startNewHand() {
        if (!canContinueGame()) {
            notifyMessage(I18n.get("engine.cannot_continue"))
            return
        }

        gameState.reset()
        moveDealerButton()
        gameState.deck.shuffle()

        val smallBlindIndex = getNextPlayerWithChips(gameState.dealerIndex)
        val bigBlindIndex = getNextPlayerWithChips(smallBlindIndex)

        postBlinds(smallBlindIndex, bigBlindIndex)
        dealHoleCards()
        gameState.currentPhase = GamePhase.PRE_FLOP

        val firstToActIndex = getNextNonAllInPlayerIndex(bigBlindIndex)

        if (firstToActIndex == -1) {
            notifyPhaseChanged(GamePhase.PRE_FLOP)
            notifyMessage(I18n.get("engine.new_hand"))
            runOutRemainingCards()
            notifyStateChanged()
            return
        }

        gameState.currentPlayerIndex = firstToActIndex

        notifyPhaseChanged(GamePhase.PRE_FLOP)
        notifyMessage(I18n.get("engine.new_hand"))

        val currentPlayer = gameState.players[gameState.currentPlayerIndex]
        if (currentPlayer.isBot) {
            executeBotActions()
        }
        
        notifyStateChanged()
    }

    private fun getNextPlayerIndex(fromIndex: Int): Int {
        return (fromIndex + 1) % gameState.players.size
    }

    private fun getNextPlayerWithChips(fromIndex: Int): Int {
        var nextIndex = (fromIndex + 1) % gameState.players.size
        var attempts = 0
        while (attempts < gameState.players.size) {
            if (gameState.players[nextIndex].chips > 0) {
                return nextIndex
            }
            nextIndex = (nextIndex + 1) % gameState.players.size
            attempts++
        }
        return (fromIndex + 1) % gameState.players.size
    }

    private fun getNextNonAllInPlayerIndex(fromIndex: Int): Int {
        var nextIndex = (fromIndex + 1) % gameState.players.size
        var attempts = 0
        while (attempts < gameState.players.size) {
            val player = gameState.players[nextIndex]
            if (!player.isFolded && !player.isAllIn) {
                return nextIndex
            }
            nextIndex = (nextIndex + 1) % gameState.players.size
            attempts++
        }
        return -1
    }

    fun handlePlayerAction(action: PlayerAction, raiseAmount: Int = 0) {
        if (gameState.currentPhase == GamePhase.SHOWDOWN ||
            gameState.currentPhase == GamePhase.GAME_OVER ||
            gameState.currentPhase == GamePhase.WAITING) {
            return
        }

        val currentPlayer = gameState.players[gameState.currentPlayerIndex]
        
        if (currentPlayer.isBot) {
            notifyMessage(I18n.get("engine.not_your_turn"))
            return
        }

        executeAction(currentPlayer, action, raiseAmount)

        if (gameState.getActivePlayers().size == 1) {
            val winner = gameState.getActivePlayers().first()
            winner.chips += gameState.pot
            notifyMessage(I18n.get("engine.player_wins", winner.name, gameState.pot))
            gameState.currentPhase = GamePhase.SHOWDOWN
            checkPlayersAfterHand()
            notifyStateChanged()
            return
        }

        if (gameState.isRoundComplete()) {
            advanceToNextPhase()
        } else {
            val nextIndex = gameState.getNextActivePlayerIndex(gameState.currentPlayerIndex)
            if (nextIndex == -1) {
                advanceToNextPhase()
            } else {
                gameState.currentPlayerIndex = nextIndex
                val nextPlayer = gameState.players[nextIndex]
                if (nextPlayer.isBot) {
                    executeBotActions()
                }
            }
        }
        
        notifyStateChanged()
    }

    private fun executeAction(player: Player, action: PlayerAction, raiseAmount: Int = 0) {
        val actualAmount = when (action) {
            PlayerAction.FOLD -> {
                player.isFolded = true
                0
            }
            PlayerAction.CHECK -> {
                0
            }
            PlayerAction.CALL -> {
                val callAmount = gameState.currentBet - player.currentBet
                val actualCall = minOf(callAmount, player.chips)
                val betAmount = player.bet(actualCall)
                gameState.pot += betAmount
                if (player.chips == 0) player.isAllIn = true
                betAmount
            }
            PlayerAction.RAISE -> {
                val totalBet = gameState.currentBet + raiseAmount
                val raiseTotal = totalBet - player.currentBet
                val actualRaise = minOf(raiseTotal, player.chips)
                val betAmount = player.bet(actualRaise)
                gameState.pot += betAmount
                gameState.currentBet = player.currentBet
                gameState.lastRaiseAmount = raiseAmount
                if (player.chips == 0) player.isAllIn = true
                gameState.players.filter { it != player && !it.isFolded && !it.isAllIn }
                    .forEach { it.hasActedThisRound = false }
                betAmount
            }
            PlayerAction.ALL_IN -> {
                val allInAmount = player.chips
                val betAmount = player.bet(allInAmount)
                gameState.pot += betAmount
                if (player.currentBet > gameState.currentBet) {
                    gameState.currentBet = player.currentBet
                    gameState.players.filter { it != player && !it.isFolded && !it.isAllIn }
                        .forEach { it.hasActedThisRound = false }
                }
                player.isAllIn = true
                betAmount
            }
        }

        player.hasActedThisRound = true

        gameState.actionHistory.add(
            ActionRecord(player, action, actualAmount, gameState.currentPhase)
        )
        
        notifyPlayerAction(player, action, actualAmount)
    }

    private fun executeBotActions() {
        if (gameState.currentPhase == GamePhase.SHOWDOWN || 
            gameState.currentPhase == GamePhase.GAME_OVER) {
            return
        }

        val currentPlayer = gameState.players[gameState.currentPlayerIndex]
        if (!currentPlayer.isBot || currentPlayer.isFolded || currentPlayer.isAllIn) {
            return
        }

        botExecutor.submit {
            Thread.sleep(500)

            SwingUtilities.invokeLater {
                if (gameState.currentPhase == GamePhase.SHOWDOWN || 
                    gameState.currentPhase == GamePhase.GAME_OVER) {
                    return@invokeLater
                }

                val botPlayer = gameState.players[gameState.currentPlayerIndex]
                if (!botPlayer.isBot || botPlayer.isFolded || botPlayer.isAllIn) {
                    return@invokeLater
                }

                val (action, amount) = GtoStrategy.decideAction(botPlayer, gameState)
                executeAction(botPlayer, action, amount)

                if (gameState.getActivePlayers().size == 1) {
                    val winner = gameState.getActivePlayers().first()
                    winner.chips += gameState.pot
                    notifyMessage(I18n.get("engine.player_wins", winner.name, gameState.pot))
                    gameState.currentPhase = GamePhase.SHOWDOWN
                    checkPlayersAfterHand()
                    notifyStateChanged()
                    return@invokeLater
                }

                if (gameState.isRoundComplete()) {
                    advanceToNextPhase()
                } else {
                    val nextIndex = gameState.getNextActivePlayerIndex(gameState.currentPlayerIndex)
                    if (nextIndex == -1) {
                        advanceToNextPhase()
                    } else {
                        gameState.currentPlayerIndex = nextIndex
                        val nextPlayer = gameState.players[nextIndex]
                        if (nextPlayer.isBot) {
                            executeBotActions()
                        }
                    }
                }

                notifyStateChanged()
            }
        }
    }

    private fun moveToNextPlayer(): Boolean {
        val nextIndex = gameState.getNextActivePlayerIndex(gameState.currentPlayerIndex)
        if (nextIndex == -1) return false
        gameState.currentPlayerIndex = nextIndex
        return true
    }

    private fun advanceToNextPhase() {
        gameState.players.forEach {
            it.currentBet = 0
            it.hasActedThisRound = false
        }
        gameState.currentBet = 0
        gameState.lastRaiseAmount = 0

        when (gameState.currentPhase) {
            GamePhase.PRE_FLOP -> {
                dealFlop()
                gameState.currentPhase = GamePhase.FLOP
                notifyPhaseChanged(GamePhase.FLOP)
                notifyMessage(I18n.get("phase.flop_round"))
            }
            GamePhase.FLOP -> {
                dealTurn()
                gameState.currentPhase = GamePhase.TURN
                notifyPhaseChanged(GamePhase.TURN)
                notifyMessage(I18n.get("phase.turn_round"))
            }
            GamePhase.TURN -> {
                dealRiver()
                gameState.currentPhase = GamePhase.RIVER
                notifyPhaseChanged(GamePhase.RIVER)
                notifyMessage(I18n.get("phase.river_round"))
            }
            GamePhase.RIVER -> {
                handleShowdown()
                return
            }
            else -> return
        }

        val nextActiveIndex = gameState.getNextActivePlayerIndex(gameState.dealerIndex)
        if (nextActiveIndex == -1) {
            runOutRemainingCards()
            return
        }
        gameState.currentPlayerIndex = nextActiveIndex
        val nextPlayer = gameState.players[gameState.currentPlayerIndex]
        if (nextPlayer.isBot) {
            executeBotActions()
        }
    }

    private fun runOutRemainingCards() {
        while (gameState.currentPhase != GamePhase.RIVER &&
               gameState.currentPhase != GamePhase.SHOWDOWN &&
               gameState.currentPhase != GamePhase.GAME_OVER) {
            when (gameState.currentPhase) {
                GamePhase.FLOP -> {
                    dealTurn()
                    gameState.currentPhase = GamePhase.TURN
                    notifyPhaseChanged(GamePhase.TURN)
                    notifyMessage(I18n.get("phase.turn_round"))
                }
                GamePhase.TURN -> {
                    dealRiver()
                    gameState.currentPhase = GamePhase.RIVER
                    notifyPhaseChanged(GamePhase.RIVER)
                    notifyMessage(I18n.get("phase.river_round"))
                }
                else -> break
            }
            notifyStateChanged()
        }
        handleShowdown()
    }

    private fun dealFlop() {
        val flopCards = gameState.deck.dealMultiple(3)
        gameState.communityCards.addAll(flopCards)
    }

    private fun dealTurn() {
        gameState.communityCards.add(gameState.deck.deal())
    }

    private fun dealRiver() {
        gameState.communityCards.add(gameState.deck.deal())
    }

    private fun dealHoleCards() {
        gameState.players.forEach { player ->
            player.holeCards.addAll(gameState.deck.dealMultiple(2))
        }
    }

    private fun handleShowdown() {
        gameState.currentPhase = GamePhase.SHOWDOWN
        notifyPhaseChanged(GamePhase.SHOWDOWN)
        
        val results = determineWinners()
        notifyShowdown(results)
        checkPlayersAfterHand()
        notifyStateChanged()
    }

    private fun checkPlayersAfterHand() {
        val humanPlayer = getHumanPlayer()
        if (humanPlayer != null && humanPlayer.chips <= 0) {
            gameState.currentPhase = GamePhase.GAME_OVER
            notifyMessage(I18n.get("engine.chips_exhausted"))
            notifyStateChanged()
            return
        }

        val eliminatedBots = gameState.players.filter { it.isBot && it.chips <= 0 }
        if (eliminatedBots.isNotEmpty()) {
            eliminatedBots.forEach { bot ->
                notifyMessage(I18n.get("engine.bot_eliminated", bot.name))
            }
            gameState.players.removeAll(eliminatedBots)
            if (gameState.dealerIndex >= gameState.players.size) {
                gameState.dealerIndex = 0
            }
            if (gameState.currentPlayerIndex >= gameState.players.size) {
                gameState.currentPlayerIndex = 0
            }
        }

        if (gameState.players.count { it.chips > 0 } < 2) {
            gameState.currentPhase = GamePhase.GAME_OVER
            val activePlayers = gameState.players.filter { it.chips > 0 }
            if (activePlayers.size == 1) {
                notifyMessage(I18n.get("engine.final_winner", activePlayers.first().name))
            } else {
                notifyMessage(I18n.get("engine.game_over_restart"))
            }
            notifyStateChanged()
        }
    }

    private fun determineWinners(): List<ShowdownResult> {
        val activePlayers = gameState.players.filter { !it.isFolded }
        val handResults = activePlayers.associateWith { player ->
            HandEvaluator.evaluate(player.holeCards, gameState.communityCards)
        }

        val allPlayers = gameState.players.toList()
        val winAmounts = mutableMapOf<Player, Int>()
        allPlayers.forEach { winAmounts[it] = 0 }

        val allInLevels = allPlayers
            .map { it.totalBetInRound }
            .filter { it > 0 }
            .distinct()
            .sorted()

        var processedLevel = 0

        for (level in allInLevels) {
            val layerContribution = level - processedLevel
            if (layerContribution <= 0) continue

            var potForThisLayer = 0
            val eligiblePlayers = mutableListOf<Player>()

            for (player in allPlayers) {
                val contribution = minOf(layerContribution, maxOf(0, player.totalBetInRound - processedLevel))
                potForThisLayer += contribution
                if (!player.isFolded && player.totalBetInRound >= level) {
                    eligiblePlayers.add(player)
                }
            }

            if (potForThisLayer > 0 && eligiblePlayers.isNotEmpty()) {
                val eligibleResults = eligiblePlayers.associateWith { handResults[it]!! }
                val bestInLayer = eligibleResults.values.maxOrNull()!!
                val layerWinners = eligibleResults.filter { it.value.compareTo(bestInLayer) == 0 }.keys.toList()
                val winPerPlayer = potForThisLayer / layerWinners.size

                for (winner in layerWinners) {
                    winAmounts[winner] = winAmounts[winner]!! + winPerPlayer
                }
            }

            processedLevel = level
        }

        for ((player, amount) in winAmounts) {
            player.chips += amount
        }

        return handResults.map { (player, handResult) ->
            ShowdownResult(player, handResult, winAmounts[player] ?: 0)
        }
    }

    private fun postBlinds(smallBlindIndex: Int, bigBlindIndex: Int) {
        val smallBlindPlayer = gameState.players[smallBlindIndex]
        val bigBlindPlayer = gameState.players[bigBlindIndex]

        val smallBlindAmount = minOf(gameState.smallBlind, smallBlindPlayer.chips)
        val bigBlindAmount = minOf(gameState.bigBlind, bigBlindPlayer.chips)

        gameState.pot += smallBlindPlayer.bet(smallBlindAmount)
        gameState.pot += bigBlindPlayer.bet(bigBlindAmount)

        gameState.currentBet = bigBlindAmount

        if (smallBlindPlayer.chips == 0) smallBlindPlayer.isAllIn = true
        if (bigBlindPlayer.chips == 0) bigBlindPlayer.isAllIn = true

        notifyMessage(I18n.get("engine.small_blind", smallBlindPlayer.name, smallBlindAmount))
        notifyMessage(I18n.get("engine.big_blind", bigBlindPlayer.name, bigBlindAmount))
    }

    private fun moveDealerButton() {
        gameState.players.forEach { it.isDealer = false }
        
        var newDealerIndex = gameState.dealerIndex
        var attempts = 0
        do {
            newDealerIndex = (newDealerIndex + 1) % gameState.players.size
            attempts++
        } while (gameState.players[newDealerIndex].chips == 0 && attempts < gameState.players.size)

        gameState.dealerIndex = newDealerIndex
        gameState.players[newDealerIndex].isDealer = true
    }

    fun canContinueGame(): Boolean {
        val humanPlayer = getHumanPlayer()
        if (humanPlayer != null && humanPlayer.chips <= 0) return false
        return gameState.players.count { it.chips > 0 } >= 2
    }

    fun getHumanPlayer(): Player? {
        return gameState.players.find { !it.isBot }
    }

    fun getAvailableActions(): List<PlayerAction> {
        if (gameState.currentPhase == GamePhase.SHOWDOWN ||
            gameState.currentPhase == GamePhase.GAME_OVER ||
            gameState.currentPhase == GamePhase.WAITING) return emptyList()
        if (gameState.currentPlayerIndex >= gameState.players.size) return emptyList()
        val player = gameState.players[gameState.currentPlayerIndex]
        if (player.isBot || player.isFolded) return emptyList()

        val actions = mutableListOf<PlayerAction>()

        if (gameState.currentBet > player.currentBet) {
            actions.add(PlayerAction.FOLD)
            actions.add(PlayerAction.CALL)
        } else {
            actions.add(PlayerAction.CHECK)
        }

        val minRaise = getMinRaiseAmount()
        if (player.chips > player.currentBet + minRaise) {
            actions.add(PlayerAction.RAISE)
        }

        actions.add(PlayerAction.ALL_IN)

        return actions
    }

    fun getMinRaiseAmount(): Int {
        return maxOf(gameState.bigBlind, gameState.lastRaiseAmount)
    }

    fun getMaxRaiseAmount(): Int {
        val player = gameState.players[gameState.currentPlayerIndex]
        return player.chips - (gameState.currentBet - player.currentBet)
    }

    private fun notifyStateChanged() {
        listeners.forEach { it.onGameStateChanged() }
    }

    private fun notifyPlayerAction(player: Player, action: PlayerAction, amount: Int) {
        listeners.forEach { it.onPlayerAction(player, action, amount) }
    }

    private fun notifyPhaseChanged(phase: GamePhase) {
        listeners.forEach { it.onPhaseChanged(phase) }
    }

    private fun notifyMessage(message: String) {
        listeners.forEach { it.onGameMessage(message) }
    }

    private fun notifyShowdown(results: List<ShowdownResult>) {
        listeners.forEach { it.onShowdown(results) }
    }

    private fun notifyGameOver(winner: Player) {
        listeners.forEach { it.onGameOver(winner) }
    }
}
