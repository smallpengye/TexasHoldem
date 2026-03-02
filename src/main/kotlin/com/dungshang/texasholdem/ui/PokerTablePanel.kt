package com.dungshang.texasholdem.ui

import com.dungshang.texasholdem.engine.GameEngine
import com.dungshang.texasholdem.model.*
import com.dungshang.texasholdem.i18n.I18n
import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 德州扑克牌桌面板，主要的游戏界面
 * 继承 JPanel 并实现 GameEngine.GameEventListener
 */
class PokerTablePanel(private val gameEngine: GameEngine = GameEngine()) : JPanel(), GameEngine.GameEventListener {
    private val messageLog = mutableListOf<String>()
    
    private val actionPanel: JPanel
    private val foldButton: JButton
    private val checkButton: JButton
    private val callButton: JButton
    private val raiseButton: JButton
    private val allInButton: JButton
    private val raiseSlider: JSlider
    private val raiseAmountLabel: JLabel
    private val newHandButton: JButton
    private val newGameButton: JButton
    private val messageArea: JTextArea
    
    private val TABLE_COLOR = Color(0x1A, 0x1A, 0x1A)
    private val TABLE_BORDER_COLOR = Color(0x80, 0x80, 0x80)
    private val CURRENT_PLAYER_HIGHLIGHT = Color(0x50, 0x50, 0x50)
    
    init {
        layout = BorderLayout()
        
        foldButton = JButton(I18n.get("ui.fold"))
        checkButton = JButton(I18n.get("ui.check"))
        callButton = JButton(I18n.get("ui.call"))
        raiseButton = JButton(I18n.get("ui.raise"))
        allInButton = JButton(I18n.get("ui.all_in"))
        raiseSlider = JSlider(0, 1000, 0)
        raiseSlider.majorTickSpacing = 100
        raiseSlider.minorTickSpacing = 50
        raiseSlider.paintTicks = true
        raiseAmountLabel = JLabel("0")
        newHandButton = JButton(I18n.get("ui.new_hand"))
        newGameButton = JButton(I18n.get("ui.new_game"))
        
        messageArea = JTextArea()
        messageArea.isEditable = false
        messageArea.font = Font("Monospaced", Font.PLAIN, 9)
        
        actionPanel = createActionPanel()
        
        add(createMessagePanel(), BorderLayout.EAST)
        add(actionPanel, BorderLayout.SOUTH)
        
        gameEngine.addListener(this)
        
        setupButtonListeners()
    }
    
    private fun createActionPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.border = EmptyBorder(5, 5, 5, 5)
        panel.preferredSize = Dimension(0, 50)
        
        val buttonPanel = JPanel()
        buttonPanel.layout = FlowLayout(FlowLayout.LEFT, 5, 5)
        
        foldButton.preferredSize = Dimension(52, 24)
        checkButton.preferredSize = Dimension(52, 24)
        callButton.preferredSize = Dimension(52, 24)
        raiseButton.preferredSize = Dimension(52, 24)
        allInButton.preferredSize = Dimension(52, 24)
        foldButton.font = Font("Arial", Font.PLAIN, 10)
        checkButton.font = Font("Arial", Font.PLAIN, 10)
        callButton.font = Font("Arial", Font.PLAIN, 10)
        raiseButton.font = Font("Arial", Font.PLAIN, 10)
        allInButton.font = Font("Arial", Font.PLAIN, 10)
        
        buttonPanel.add(foldButton)
        buttonPanel.add(checkButton)
        buttonPanel.add(callButton)
        buttonPanel.add(raiseButton)
        buttonPanel.add(allInButton)
        
        val raisePanel = JPanel()
        raisePanel.layout = FlowLayout(FlowLayout.CENTER, 5, 5)
        raisePanel.add(JLabel(I18n.get("ui.raise_amount")))
        raisePanel.add(raiseSlider)
        raisePanel.add(raiseAmountLabel)
        
        val controlPanel = JPanel()
        controlPanel.layout = FlowLayout(FlowLayout.RIGHT, 5, 5)
        controlPanel.add(newHandButton)
        controlPanel.add(newGameButton)
        
        panel.add(buttonPanel, BorderLayout.WEST)
        panel.add(raisePanel, BorderLayout.CENTER)
        panel.add(controlPanel, BorderLayout.EAST)
        
        return panel
    }
    
    private fun createMessagePanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.preferredSize = Dimension(180, 0)
        
        val scrollPane = JScrollPane(messageArea)
        scrollPane.border = EmptyBorder(5, 5, 5, 5)
        
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun setupButtonListeners() {
        foldButton.addActionListener { handlePlayerAction(PlayerAction.FOLD) }
        checkButton.addActionListener { handlePlayerAction(PlayerAction.CHECK) }
        callButton.addActionListener { handlePlayerAction(PlayerAction.CALL) }
        raiseButton.addActionListener { handlePlayerAction(PlayerAction.RAISE, raiseSlider.value) }
        allInButton.addActionListener { handlePlayerAction(PlayerAction.ALL_IN) }
        
        raiseSlider.addChangeListener { raiseAmountLabel.text = raiseSlider.value.toString() }
        
        newHandButton.addActionListener {
            if (gameEngine.canContinueGame()) {
                gameEngine.startNewHand()
            } else {
                showSetupAndStart()
            }
        }
        
        newGameButton.addActionListener { showSetupAndStart() }
    }
    
    private fun handlePlayerAction(action: PlayerAction, raiseAmount: Int = 0) {
        gameEngine.handlePlayerAction(action, raiseAmount)
    }
    
    /**
     * 显示设置对话框并开始游戏
     */
    fun showSetupAndStart() {
        val dialog = GameSetupDialog(null)
        if (dialog.showAndGet()) {
            val playerName = dialog.getPlayerName()
            val playerChips = dialog.getPlayerChips()
            val botConfigs = dialog.getBotConfigs()
            
            gameEngine.setupGame(playerName, playerChips, botConfigs)
            gameEngine.startNewHand()
            
            messageLog.clear()
            addMessage(I18n.get("ui.game_start"))
            addMessage(I18n.get("ui.player_info", playerName, playerChips))
            botConfigs.forEach { config: GameEngine.BotConfig ->
                addMessage(I18n.get("ui.bot_info", config.name, config.chips, config.aggressiveness))
            }
            
            repaint()
        }
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        drawTableBackground(g2d)

        if (gameEngine.gameState.currentPhase == GamePhase.WAITING) {
            drawGuide(g2d)
            return
        }

        drawPhaseInfo(g2d)
        drawPotInfo(g2d)
        drawCommunityCards(g2d)
        drawPlayers(g2d)
    }
    
    private fun drawTableBackground(graphics: Graphics2D) {
        val width = width
        val height = height - actionPanel.height

        graphics.color = TABLE_COLOR
        graphics.fillRect(0, 0, width, height)

        val totalCards = 5
        val cardSpacing = CardRenderer.CARD_WIDTH + 5
        val cardsWidth = totalCards * CardRenderer.CARD_WIDTH + (totalCards - 1) * 5
        val padding = 10
        val tableWidth = cardsWidth + padding * 2
        val tableHeight = CardRenderer.CARD_HEIGHT + padding * 2
        val tableX = (width - tableWidth) / 2
        val tableY = (height * 0.45 - padding).toInt()

        graphics.color = TABLE_BORDER_COLOR
        graphics.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(4f, 4f), 0f)
        graphics.drawRect(tableX, tableY, tableWidth, tableHeight)
        graphics.stroke = BasicStroke(1f)
    }
    
    private fun drawPhaseInfo(graphics: Graphics2D) {
        val width = width
        val height = height - actionPanel.height
        
        val phaseText = when (gameEngine.gameState.currentPhase) {
            GamePhase.WAITING -> I18n.get("phase.waiting")
            GamePhase.PRE_FLOP -> I18n.get("phase.pre_flop")
            GamePhase.FLOP -> I18n.get("phase.flop")
            GamePhase.TURN -> I18n.get("phase.turn")
            GamePhase.RIVER -> I18n.get("phase.river")
            GamePhase.SHOWDOWN -> I18n.get("phase.showdown")
            GamePhase.GAME_OVER -> I18n.get("phase.game_over")
        }
        
        graphics.color = Color(0xB0, 0xB0, 0xB0)
        graphics.font = Font("Arial", Font.BOLD, 11)
        val metrics = graphics.fontMetrics
        val textWidth = metrics.stringWidth(phaseText)
        
        graphics.drawString(
            phaseText,
            (width - textWidth) / 2.toFloat(),
            height * 0.25f
        )
    }
    
    private fun drawPotInfo(graphics: Graphics2D) {
        val width = width
        val height = height - actionPanel.height
        
        val potText = I18n.get("ui.pot", gameEngine.gameState.pot)
        
        graphics.color = Color(0xC0, 0xC0, 0xC0)
        graphics.font = Font("Arial", Font.BOLD, 12)
        val metrics = graphics.fontMetrics
        val textWidth = metrics.stringWidth(potText)
        
        graphics.drawString(
            potText,
            (width - textWidth) / 2.toFloat(),
            height * 0.35f
        )
    }
    
    private fun drawCommunityCards(graphics: Graphics2D) {
        val width = width
        val height = height - actionPanel.height
        
        val communityCards = gameEngine.gameState.communityCards
        val totalCards = 5
        val cardSpacing = CardRenderer.CARD_WIDTH + 5
        val totalWidth = totalCards * CardRenderer.CARD_WIDTH + (totalCards - 1) * 5
        val startX = (width - totalWidth) / 2
        val startY = height * 0.45
        
        for (i in 0 until totalCards) {
            val x = startX + i * cardSpacing
            if (i < communityCards.size) {
                CardRenderer.drawCard(graphics, communityCards[i], x, startY.toInt())
            } else {
                CardRenderer.drawEmptySlot(graphics, x, startY.toInt())
            }
        }
    }
    
    private fun drawGuide(graphics: Graphics2D) {
        val width = width
        val height = height - actionPanel.height

        graphics.font = Font("Monospaced", Font.BOLD, 14)
        graphics.color = Color(0xD0, 0xD0, 0xD0)
        val titleMetrics = graphics.fontMetrics
        val title = I18n.get("guide.title")
        graphics.drawString(title, (width - titleMetrics.stringWidth(title)) / 2, 30)

        graphics.font = Font("Monospaced", Font.PLAIN, 11)
        graphics.color = Color(0xA0, 0xA0, 0xA0)

        val lines = listOf(
            "",
            I18n.get("guide.overview"),
            "",
            I18n.get("guide.flow_title"),
            I18n.get("guide.flow_1"),
            I18n.get("guide.flow_2"),
            I18n.get("guide.flow_3"),
            I18n.get("guide.flow_4"),
            I18n.get("guide.flow_5"),
            "",
            I18n.get("guide.actions_title"),
            I18n.get("guide.action_fold"),
            I18n.get("guide.action_check"),
            I18n.get("guide.action_call"),
            I18n.get("guide.action_raise"),
            I18n.get("guide.action_allin"),
            "",
            I18n.get("guide.hand_title"),
            I18n.get("guide.hand_list"),
            "",
            I18n.get("guide.start_hint")
        )

        val lineHeight = 16
        val startY = 55
        val leftMargin = 20

        for ((index, line) in lines.withIndex()) {
            if (line == I18n.get("guide.flow_title") ||
                line == I18n.get("guide.actions_title") ||
                line == I18n.get("guide.hand_title")) {
                graphics.color = Color(0xC0, 0xC0, 0xC0)
                graphics.font = Font("Monospaced", Font.BOLD, 11)
            } else if (line == I18n.get("guide.start_hint")) {
                graphics.color = Color(0xE0, 0xA0, 0x40)
                graphics.font = Font("Monospaced", Font.BOLD, 12)
            } else {
                graphics.color = Color(0xA0, 0xA0, 0xA0)
                graphics.font = Font("Monospaced", Font.PLAIN, 11)
            }
            graphics.drawString(line, leftMargin, startY + index * lineHeight)
        }
    }

    private fun drawPlayers(graphics: Graphics2D) {
        val width = width
        val height = height - actionPanel.height
        val players = gameEngine.gameState.players
        val playerCount = players.size
        
        if (playerCount == 0) return
        
        val tableWidth = width * 0.55
        val tableHeight = height * 0.35
        val centerX = width / 2
        val centerY = height / 2
        val radiusX = tableWidth / 2 + 55
        val radiusY = tableHeight / 2 + 45
        
        val humanIndex = players.indexOfFirst { !it.isBot }
        val botPlayers = players.filter { it.isBot }
        val botCount = botPlayers.size
        
        for (i in players.indices) {
            val player = players[i]
            val angle: Double
            
            if (!player.isBot) {
                angle = Math.PI / 2.0
            } else {
                val botIndex = players.subList(0, i).count { it.isBot }
                angle = if (botCount == 1) {
                    -Math.PI / 2.0
                } else {
                    val spread = Math.PI * 0.8
                    val startAngle = -Math.PI / 2.0 - spread / 2.0
                    startAngle + spread * botIndex / (botCount - 1).coerceAtLeast(1)
                }
            }
            
            val playerX = centerX + (radiusX * Math.cos(angle)).toInt()
            val playerY = centerY - (radiusY * Math.sin(angle)).toInt()
            
            val isCurrentPlayer = (gameEngine.gameState.currentPlayerIndex == i)
            drawPlayer(graphics, player, playerX, playerY, isCurrentPlayer)
        }
    }
    
    private fun drawPlayer(graphics: Graphics2D, player: Player, centerX: Int, centerY: Int, isCurrentPlayer: Boolean) {
        val panelWidth = 80
        val hasCards = player.holeCards.isNotEmpty()
        val hasBet = player.currentBet > 0
        val lastAction = getLastActionForPlayer(player)
        val hasAction = player.isBot && lastAction != null
        val isFolded = player.isFolded
        val panelHeight = when {
            hasCards && hasBet && hasAction -> 68
            hasCards && hasBet -> 55
            hasCards && hasAction -> 58
            hasCards -> 46
            else -> 24
        }
        val x = centerX - panelWidth / 2
        val y = centerY - panelHeight / 2

        val originalComposite = graphics.composite

        if (isFolded) {
            graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)
        }

        if (isCurrentPlayer && !isFolded) {
            graphics.color = CURRENT_PLAYER_HIGHLIGHT
            graphics.fillRect(x - 2, y - 2, panelWidth + 4, panelHeight + 4)
        }

        graphics.color = TABLE_COLOR
        graphics.fillRect(x, y, panelWidth, panelHeight)

        graphics.color = if (isFolded) Color(0x40, 0x40, 0x40) else Color(0x80, 0x80, 0x80)
        graphics.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(3f, 3f), 0f)
        graphics.drawRect(x, y, panelWidth, panelHeight)
        graphics.stroke = BasicStroke(1f)

        graphics.font = Font("Monospaced", Font.PLAIN, 9)
        graphics.color = if (isFolded) Color(0x70, 0x70, 0x70) else Color(0xC0, 0xC0, 0xC0)

        var statusText = player.name
        if (player.isDealer) statusText += " D"
        if (player.isAllIn) statusText += " !"

        graphics.drawString(statusText, x + 2, y + 10)

        graphics.color = if (isFolded) Color(0x60, 0x60, 0x60) else Color(0x90, 0x90, 0x90)
        graphics.drawString("${player.chips}", x + 2, y + 20)

        var nextY = y + 24
        if (hasBet) {
            graphics.drawString(I18n.get("ui.bet", player.currentBet), x + 2, y + 30)
            nextY = y + 34
        }

        if (isFolded && hasCards) {
            graphics.composite = originalComposite
            graphics.font = Font("Monospaced", Font.BOLD, 9)
            graphics.color = Color(0xC0, 0x40, 0x40)
            val foldText = I18n.get("ui.folded")
            val foldMetrics = graphics.fontMetrics
            val foldTextWidth = foldMetrics.stringWidth(foldText)
            graphics.drawString(foldText, x + (panelWidth - foldTextWidth) / 2, nextY + CardRenderer.CARD_HEIGHT / 2 + 4)
            return
        }

        val showCards = (!player.isBot && player.holeCards.isNotEmpty()) ||
                        (gameEngine.gameState.currentPhase == GamePhase.SHOWDOWN)

        if (hasCards) {
            val cardSpacing = CardRenderer.CARD_WIDTH + 2
            val startX = x + (panelWidth - 2 * CardRenderer.CARD_WIDTH - 2) / 2

            if (showCards) {
                for (i in player.holeCards.indices) {
                    CardRenderer.drawCard(graphics, player.holeCards[i], startX + i * cardSpacing, nextY)
                }
            } else {
                CardRenderer.drawCardBack(graphics, startX, nextY)
                CardRenderer.drawCardBack(graphics, startX + cardSpacing, nextY)
            }
            nextY += CardRenderer.CARD_HEIGHT + 2
        }

        graphics.composite = originalComposite

        if (hasAction && lastAction != null) {
            val actionText = when (lastAction.action) {
                PlayerAction.FOLD -> I18n.get("action.fold")
                PlayerAction.CHECK -> I18n.get("action.check")
                PlayerAction.CALL -> if (lastAction.amount > 0) I18n.get("action.call_amount", lastAction.amount) else I18n.get("action.call")
                PlayerAction.RAISE -> I18n.get("action.raise_amount", lastAction.amount)
                PlayerAction.ALL_IN -> I18n.get("action.all_in")
            }
            graphics.font = Font("Monospaced", Font.PLAIN, 9)
            graphics.color = Color(0xE0, 0xA0, 0x40)
            graphics.drawString(actionText, x + 2, nextY + 10)
        }
    }

    private fun getLastActionForPlayer(player: Player): ActionRecord? {
        val currentPhase = gameEngine.gameState.currentPhase
        return gameEngine.gameState.actionHistory
            .lastOrNull { it.player.id == player.id && it.phase == currentPhase }
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
            raiseSlider.minimum = minRaise
            raiseSlider.maximum = maxRaise
            raiseSlider.value = minRaise
            raiseAmountLabel.text = minRaise.toString()
        }
        
        newHandButton.isEnabled = gameEngine.canContinueGame() && 
                                  (gameEngine.gameState.currentPhase == GamePhase.GAME_OVER || 
                                   gameEngine.gameState.currentPhase == GamePhase.SHOWDOWN)
        newGameButton.isEnabled = true
    }
    
    private fun addMessage(message: String) {
        SwingUtilities.invokeLater {
            messageLog.add(message)
            messageArea.append(message + "\n")
            messageArea.caretPosition = messageArea.document.length
        }
    }
    
    override fun onGameStateChanged() {
        SwingUtilities.invokeLater {
            repaint()
            updateActionButtons()
        }
    }
    
    override fun onPlayerAction(player: Player, action: PlayerAction, amount: Int) {
        val actionText = when (action) {
            PlayerAction.FOLD -> I18n.get("action.fold")
            PlayerAction.CHECK -> I18n.get("action.check")
            PlayerAction.CALL -> I18n.get("action.call")
            PlayerAction.RAISE -> I18n.get("action.raise_amount", amount)
            PlayerAction.ALL_IN -> I18n.get("action.all_in")
        }
        addMessage("${player.name} $actionText")
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
        addMessage("阶段: $phaseText")
    }
    
    override fun onGameMessage(message: String) {
        addMessage(message)
    }
    
    override fun onShowdown(results: List<GameEngine.ShowdownResult>) {
        addMessage(I18n.get("ui.showdown_result"))
        results.forEach { result ->
            val handResult = result.handResult
            addMessage("${result.player.name}: ${handResult.handRank.localizedName} - ${result.player.holeCards.joinToString(", ")}")
            if (result.winAmount > 0) {
                addMessage(I18n.get("ui.wins_chips", result.player.name, result.winAmount))
            }
        }
        addMessage(I18n.get("ui.showdown_end"))
    }
    
    override fun onGameOver(winner: Player) {
        addMessage(I18n.get("ui.game_over_winner", winner.name))
    }
}
