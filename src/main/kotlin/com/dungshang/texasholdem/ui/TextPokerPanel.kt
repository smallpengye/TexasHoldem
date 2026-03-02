package com.dungshang.texasholdem.ui

import com.dungshang.texasholdem.engine.GameEngine
import com.dungshang.texasholdem.i18n.I18n
import com.dungshang.texasholdem.model.*
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 简化版德州扑克面板
 * 纯文本日志格式，黑白灰配色，所有信息排成一列展示
 */
class TextPokerPanel(private val gameEngine: GameEngine = GameEngine()) : JPanel(), GameEngine.GameEventListener {

    private val gameDisplayArea: JTextArea
    private val messageArea: JTextArea

    private val foldButton: JButton
    private val checkButton: JButton
    private val callButton: JButton
    private val raiseButton: JButton
    private val allInButton: JButton
    private val raiseSpinner: JSpinner
    private val newHandButton: JButton
    private val newGameButton: JButton

    init {
        layout = BorderLayout()
        background = Color(0x1A, 0x1A, 0x1A)

        gameDisplayArea = JTextArea().apply {
            isEditable = false
            font = Font("Monospaced", Font.PLAIN, 12)
            background = Color(0x1A, 0x1A, 0x1A)
            foreground = Color(0xCC, 0xCC, 0xCC)
            border = EmptyBorder(8, 8, 8, 8)
            lineWrap = true
            wrapStyleWord = true
        }

        messageArea = JTextArea().apply {
            isEditable = false
            font = Font("Monospaced", Font.PLAIN, 11)
            background = Color(0x22, 0x22, 0x22)
            foreground = Color(0x99, 0x99, 0x99)
            border = EmptyBorder(4, 8, 4, 8)
            lineWrap = true
            wrapStyleWord = true
            rows = 6
        }

        foldButton = createButton(I18n.get("ui.fold"))
        checkButton = createButton(I18n.get("ui.check"))
        callButton = createButton(I18n.get("ui.call"))
        raiseButton = createButton(I18n.get("ui.raise"))
        allInButton = createButton(I18n.get("ui.all_in"))
        newHandButton = createButton(I18n.get("ui.new_hand"))
        newGameButton = createButton(I18n.get("ui.new_game"))

        raiseSpinner = JSpinner(SpinnerNumberModel(20, 1, 100000, 10)).apply {
            preferredSize = Dimension(70, 24)
            font = Font("Monospaced", Font.PLAIN, 11)
        }

        val mainSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = JScrollPane(gameDisplayArea).apply {
                border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color(0x40, 0x40, 0x40))
                minimumSize = Dimension(0, 100)
            }
            bottomComponent = JScrollPane(messageArea).apply {
                border = null
                minimumSize = Dimension(0, 60)
            }
            resizeWeight = 0.75
            dividerSize = 3
            border = null
        }

        add(mainSplit, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.SOUTH)

        gameEngine.addListener(this)
        setupButtonListeners()

        gameDisplayArea.text = buildWelcomeGuideText()
    }

    private fun createButton(text: String): JButton {
        return JButton(text).apply {
            font = Font("Monospaced", Font.PLAIN, 11)
            preferredSize = Dimension(56, 24)
            isFocusPainted = false
        }
    }

    private fun createActionPanel(): JPanel {
        val panel = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 4, 4)
            background = Color(0x22, 0x22, 0x22)
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, Color(0x40, 0x40, 0x40))
        }

        panel.add(foldButton)
        panel.add(checkButton)
        panel.add(callButton)
        panel.add(raiseButton)
        panel.add(raiseSpinner)
        panel.add(allInButton)
        panel.add(Box.createHorizontalStrut(16))
        panel.add(newHandButton)
        panel.add(newGameButton)

        return panel
    }

    private fun setupButtonListeners() {
        foldButton.addActionListener { gameEngine.handlePlayerAction(PlayerAction.FOLD) }
        checkButton.addActionListener { gameEngine.handlePlayerAction(PlayerAction.CHECK) }
        callButton.addActionListener { gameEngine.handlePlayerAction(PlayerAction.CALL) }
        raiseButton.addActionListener {
            val amount = raiseSpinner.value as Int
            gameEngine.handlePlayerAction(PlayerAction.RAISE, amount)
        }
        allInButton.addActionListener { gameEngine.handlePlayerAction(PlayerAction.ALL_IN) }

        newHandButton.addActionListener {
            if (gameEngine.canContinueGame()) {
                gameEngine.startNewHand()
            } else {
                showSetupAndStart()
            }
        }

        newGameButton.addActionListener { showSetupAndStart() }
    }

    fun showSetupAndStart() {
        val dialog = GameSetupDialog(null)
        if (dialog.showAndGet()) {
            val playerName = dialog.getPlayerName()
            val playerChips = dialog.getPlayerChips()
            val botConfigs = dialog.getBotConfigs()

            gameEngine.setupGame(playerName, playerChips, botConfigs)
            gameEngine.startNewHand()

            messageArea.text = ""
            appendMessage(I18n.get("ui.game_start"))
            repaint()
        }
    }

    private fun refreshDisplay() {
        val state = gameEngine.gameState
        val builder = StringBuilder()

        val phaseLabel = when (state.currentPhase) {
            GamePhase.WAITING -> I18n.get("phase.waiting")
            GamePhase.PRE_FLOP -> I18n.get("phase.pre_flop")
            GamePhase.FLOP -> I18n.get("phase.flop")
            GamePhase.TURN -> I18n.get("phase.turn")
            GamePhase.RIVER -> I18n.get("phase.river")
            GamePhase.SHOWDOWN -> I18n.get("phase.showdown")
            GamePhase.GAME_OVER -> I18n.get("phase.game_over")
        }
        builder.appendLine("═══════════════════════════════════════")
        builder.appendLine("  ${I18n.get("ui.phase_label", phaseLabel)}    ${I18n.get("ui.pot", state.pot)}")
        builder.appendLine("═══════════════════════════════════════")

        val communityText = if (state.communityCards.isEmpty()) {
            I18n.get("text.community_empty")
        } else {
            state.communityCards.joinToString("  ") { it.toString() }
        }
        builder.appendLine("  ${I18n.get("text.community_cards", communityText)}")
        builder.appendLine("───────────────────────────────────────")

        val nameColWidth = (state.players.maxOfOrNull { it.name.length } ?: 6).coerceAtLeast(6) + 5
        val chipsColWidth = 10
        val statusColWidth = 8
        val cardsColWidth = 14
        val betColWidth = 10

        for ((index, player) in state.players.withIndex()) {
            val nameWithMarker = buildString {
                append(player.name)
                if (player.isDealer) append("[D]")
                if (index == state.currentPlayerIndex &&
                    state.currentPhase != GamePhase.SHOWDOWN &&
                    state.currentPhase != GamePhase.GAME_OVER) {
                    append("►")
                }
            }

            val statusText = when {
                player.isFolded -> I18n.get("text.player_folded")
                player.isAllIn -> I18n.get("text.player_all_in")
                else -> ""
            }

            val cardsText = when {
                player.holeCards.isEmpty() -> ""
                player.isFolded -> "[--] [--]"
                !player.isBot || state.currentPhase == GamePhase.SHOWDOWN ->
                    player.holeCards.joinToString(" ") { "[$it]" }
                else -> "[??] [??]"
            }

            val betText = if (player.currentBet > 0) I18n.get("text.bet_amount", player.currentBet) else ""

            val lastAction = state.actionHistory
                .lastOrNull { it.player.id == player.id && it.phase == state.currentPhase }
            val actionText = if (lastAction != null && player.isBot) {
                val actionLabel = when (lastAction.action) {
                    PlayerAction.FOLD -> I18n.get("action.fold")
                    PlayerAction.CHECK -> I18n.get("action.check")
                    PlayerAction.CALL -> I18n.get("action.call")
                    PlayerAction.RAISE -> I18n.get("action.raise_display", lastAction.amount)
                    PlayerAction.ALL_IN -> I18n.get("action.all_in")
                }
                "→$actionLabel"
            } else ""

            val chipsStr = "${player.chips}"
            builder.appendLine(
                "  ${nameWithMarker.padEnd(nameColWidth)}" +
                "${chipsStr.padStart(chipsColWidth)}  " +
                "${statusText.padEnd(statusColWidth)}" +
                "${cardsText.padEnd(cardsColWidth)}" +
                "${betText.padEnd(betColWidth)}" +
                actionText
            )
        }

        builder.appendLine("───────────────────────────────────────")

        gameDisplayArea.text = builder.toString()
        gameDisplayArea.caretPosition = 0

        updateActionButtons()
    }

    private fun updateActionButtons() {
        val availableActions = gameEngine.getAvailableActions()
        val humanPlayer = gameEngine.getHumanPlayer()

        foldButton.isEnabled = PlayerAction.FOLD in availableActions && humanPlayer != null
        checkButton.isEnabled = PlayerAction.CHECK in availableActions && humanPlayer != null
        callButton.isEnabled = PlayerAction.CALL in availableActions && humanPlayer != null
        raiseButton.isEnabled = PlayerAction.RAISE in availableActions && humanPlayer != null
        allInButton.isEnabled = PlayerAction.ALL_IN in availableActions && humanPlayer != null

        if (PlayerAction.RAISE in availableActions && humanPlayer != null) {
            val minRaise = gameEngine.getMinRaiseAmount()
            val maxRaise = gameEngine.getMaxRaiseAmount()
            raiseSpinner.model = SpinnerNumberModel(minRaise, minRaise, maxRaise, 10)
        }

        newHandButton.isEnabled = gameEngine.canContinueGame() &&
                (gameEngine.gameState.currentPhase == GamePhase.GAME_OVER ||
                        gameEngine.gameState.currentPhase == GamePhase.SHOWDOWN)
        newGameButton.isEnabled = true
    }

    private fun appendMessage(message: String) {
        SwingUtilities.invokeLater {
            messageArea.append("$message\n")
            messageArea.caretPosition = messageArea.document.length
        }
    }

    override fun onGameStateChanged() {
        SwingUtilities.invokeLater { refreshDisplay() }
    }

    override fun onPlayerAction(player: Player, action: PlayerAction, amount: Int) {
        val actionText = when (action) {
            PlayerAction.FOLD -> I18n.get("action.fold")
            PlayerAction.CHECK -> I18n.get("action.check")
            PlayerAction.CALL -> I18n.get("action.call")
            PlayerAction.RAISE -> I18n.get("action.raise_amount", amount)
            PlayerAction.ALL_IN -> I18n.get("action.all_in")
        }
        appendMessage("${player.name} $actionText")
    }

    override fun onPhaseChanged(phase: GamePhase) {
        val phaseText = when (phase) {
            GamePhase.WAITING -> I18n.get("phase.waiting")
            GamePhase.PRE_FLOP -> I18n.get("phase.pre_flop")
            GamePhase.FLOP -> I18n.get("phase.flop")
            GamePhase.TURN -> I18n.get("phase.turn")
            GamePhase.RIVER -> I18n.get("phase.river")
            GamePhase.SHOWDOWN -> I18n.get("phase.showdown")
            GamePhase.GAME_OVER -> I18n.get("phase.game_over")
        }
        appendMessage("── $phaseText ──")
    }

    override fun onGameMessage(message: String) {
        appendMessage(message)
    }

    override fun onShowdown(results: List<GameEngine.ShowdownResult>) {
        appendMessage(I18n.get("text.showdown_title"))
        results.forEach { result ->
            val handName = result.handResult.handRank.localizedName
            val cards = result.player.holeCards.joinToString(" ") { it.toString() }
            appendMessage("${result.player.name}: $cards ($handName)")
            if (result.winAmount > 0) {
                appendMessage(I18n.get("text.showdown_wins", result.player.name, result.winAmount))
            }
        }
        appendMessage(I18n.get("text.showdown_end"))
    }

    override fun onGameOver(winner: Player) {
        appendMessage(I18n.get("ui.game_over_winner", winner.name))
    }

    private fun buildWelcomeGuideText(): String {
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("  ${I18n.get("text.welcome_title")}")
            appendLine("═══════════════════════════════════════")
            appendLine()
            appendLine("  ${I18n.get("guide.title")}")
            appendLine("───────────────────────────────────────")
            appendLine("  ${I18n.get("guide.overview")}")
            appendLine()
            appendLine("  ${I18n.get("guide.flow_title")}")
            appendLine("  ${I18n.get("guide.flow_1")}")
            appendLine("  ${I18n.get("guide.flow_2")}")
            appendLine("  ${I18n.get("guide.flow_3")}")
            appendLine("  ${I18n.get("guide.flow_4")}")
            appendLine("  ${I18n.get("guide.flow_5")}")
            appendLine()
            appendLine("  ${I18n.get("guide.actions_title")}")
            appendLine("  ${I18n.get("guide.action_fold")}")
            appendLine("  ${I18n.get("guide.action_check")}")
            appendLine("  ${I18n.get("guide.action_call")}")
            appendLine("  ${I18n.get("guide.action_raise")}")
            appendLine("  ${I18n.get("guide.action_allin")}")
            appendLine()
            appendLine("  ${I18n.get("guide.hand_title")}")
            appendLine("  ${I18n.get("guide.hand_list")}")
            appendLine()
            appendLine("───────────────────────────────────────")
            appendLine("  ${I18n.get("guide.start_hint")}")
            appendLine("═══════════════════════════════════════")
        }
    }
}
