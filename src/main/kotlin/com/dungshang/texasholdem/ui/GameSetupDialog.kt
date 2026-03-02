package com.dungshang.texasholdem.ui

import com.dungshang.texasholdem.engine.GameEngine
import com.dungshang.texasholdem.i18n.I18n
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * 游戏设置对话框
 * 使用 IntelliJ 的 DialogWrapper 实现
 */
class GameSetupDialog(project: Project?) : DialogWrapper(project) {
    private val playerNameField = JTextField("Player")
    private val playerChipsSpinner = JSpinner(SpinnerNumberModel(1000, 1, 100000, 100))
    private val botCountSpinner = JSpinner(SpinnerNumberModel(3, 1, 8, 1))
    private val botConfigPanel = JPanel()
    private val botConfigs = mutableListOf<BotConfigRow>()
    
    data class BotConfigRow(
        val nameField: JTextField,
        val chipsField: JTextField,
        val aggressivenessSlider: JSlider,
        val aggressivenessLabel: JLabel
    )
    
    init {
        title = I18n.get("setup.title")
        setOKButtonText(I18n.get("setup.start_game"))
        init()
        
        setupBotCountListener()
        updateBotConfigPanel()
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel()
        mainPanel.layout = BorderLayout(10, 10)
        mainPanel.border = EmptyBorder(15, 15, 15, 15)
        
        mainPanel.add(createPlayerSetupPanel(), BorderLayout.NORTH)
        mainPanel.add(createBotSetupPanel(), BorderLayout.CENTER)
        
        return mainPanel
    }
    
    private fun createPlayerSetupPanel(): JPanel {
        val panel = JPanel()
        panel.layout = GridBagLayout()
        panel.border = TitledBorder(I18n.get("setup.player_settings"))
        
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.insets = Insets(5, 5, 5, 5)
        
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.weightx = 0.0
        panel.add(JLabel(I18n.get("setup.player_name")), constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        playerNameField.preferredSize = Dimension(200, 25)
        panel.add(playerNameField, constraints)
        
        constraints.gridx = 0
        constraints.gridy = 1
        constraints.weightx = 0.0
        panel.add(JLabel(I18n.get("setup.initial_chips")), constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        playerChipsSpinner.preferredSize = Dimension(200, 25)
        panel.add(playerChipsSpinner, constraints)
        
        return panel
    }
    
    private fun createBotSetupPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout(5, 5)
        panel.border = TitledBorder(I18n.get("setup.bot_settings"))
        
        val topPanel = JPanel()
        topPanel.layout = FlowLayout(FlowLayout.LEFT)
        topPanel.add(JLabel(I18n.get("setup.bot_count")))
        botCountSpinner.preferredSize = Dimension(80, 25)
        topPanel.add(botCountSpinner)
        
        botConfigPanel.layout = BoxLayout(botConfigPanel, BoxLayout.Y_AXIS)
        val scrollPane = JScrollPane(botConfigPanel)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.preferredSize = Dimension(0, 200)
        
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun setupBotCountListener() {
        botCountSpinner.addChangeListener { e: ChangeEvent ->
            updateBotConfigPanel()
        }
    }
    
    private fun updateBotConfigPanel() {
        botConfigPanel.removeAll()
        botConfigs.clear()
        
        val botCount = botCountSpinner.value as Int
        
        for (i in 1..botCount) {
            val configRow = createBotConfigRow(i)
            botConfigs.add(configRow.first)
            botConfigPanel.add(configRow.second)
            botConfigPanel.add(Box.createVerticalStrut(5))
        }
        
        botConfigPanel.revalidate()
        botConfigPanel.repaint()
    }
    
    private fun createBotConfigRow(index: Int): Pair<BotConfigRow, JPanel> {
        val row = JPanel()
        row.layout = BoxLayout(row, BoxLayout.X_AXIS)
        row.border = EmptyBorder(5, 5, 5, 5)
        row.alignmentX = Component.LEFT_ALIGNMENT
        
        val nameLabel = JLabel("Bot $index:")
        nameLabel.preferredSize = Dimension(60, 25)
        
        val nameField = JTextField("Bot $index")
        nameField.preferredSize = Dimension(100, 25)
        
        val chipsLabel = JLabel(I18n.get("setup.chips"))
        chipsLabel.preferredSize = Dimension(40, 25)
        
        val chipsField = JTextField("1000")
        chipsField.preferredSize = Dimension(80, 25)
        
        val aggLabel = JLabel(I18n.get("setup.aggressiveness"))
        aggLabel.preferredSize = Dimension(60, 25)
        
        val aggSlider = JSlider(1, 10, 5)
        aggSlider.majorTickSpacing = 1
        aggSlider.paintTicks = true
        aggSlider.preferredSize = Dimension(150, 25)
        
        val aggValueLabel = JLabel(I18n.get("setup.agg_medium", 5))
        aggValueLabel.preferredSize = Dimension(100, 25)
        
        aggSlider.addChangeListener { e: ChangeEvent ->
            val value = (e.source as JSlider).value
            aggValueLabel.text = getAggressivenessDescription(value)
        }
        
        row.add(nameLabel)
        row.add(nameField)
        row.add(Box.createHorizontalStrut(10))
        row.add(chipsLabel)
        row.add(chipsField)
        row.add(Box.createHorizontalStrut(10))
        row.add(aggLabel)
        row.add(aggSlider)
        row.add(Box.createHorizontalStrut(5))
        row.add(aggValueLabel)
        
        val configRow = BotConfigRow(nameField, chipsField, aggSlider, aggValueLabel)
        return Pair(configRow, row)
    }
    
    private fun getAggressivenessDescription(value: Int): String {
        return when {
            value <= 3 -> I18n.get("setup.agg_tight", value)
            value <= 6 -> I18n.get("setup.agg_medium", value)
            else -> I18n.get("setup.agg_loose", value)
        }
    }
    
    override fun doValidate(): ValidationInfo? {
        val playerName = playerNameField.text.trim()
        if (playerName.isEmpty()) {
            return ValidationInfo(I18n.get("setup.err_name_empty"), playerNameField)
        }
        
        val playerChips = (playerChipsSpinner.value as Number).toInt()
        if (playerChips <= 0) {
            return ValidationInfo(I18n.get("setup.err_chips_positive"), playerChipsSpinner)
        }
        
        for (i in botConfigs.indices) {
            val row = botConfigs[i]
            val botName = row.nameField.text.trim()
            if (botName.isEmpty()) {
                return ValidationInfo(I18n.get("setup.err_bot_name_empty", i + 1), row.nameField)
            }
            
            val botChips = row.chipsField.text.trim().toIntOrNull()
            if (botChips == null || botChips <= 0) {
                return ValidationInfo(I18n.get("setup.err_bot_chips_positive", i + 1), row.chipsField)
            }
        }
        
        return null
    }
    
    fun getPlayerName(): String {
        return playerNameField.text.trim()
    }
    
    fun getPlayerChips(): Int {
        return (playerChipsSpinner.value as Number).toInt()
    }
    
    fun getBotConfigs(): List<GameEngine.BotConfig> {
        return botConfigs.mapIndexed { index, row ->
            GameEngine.BotConfig(
                name = row.nameField.text.trim(),
                chips = row.chipsField.text.trim().toInt(),
                aggressiveness = row.aggressivenessSlider.value
            )
        }
    }
}