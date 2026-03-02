package com.dungshang.texasholdem.ui

import com.dungshang.texasholdem.engine.GameEngine
import com.dungshang.texasholdem.i18n.I18n
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Texas Hold'em 工具窗口工厂
 * 负责创建和初始化游戏工具窗口
 */
class TexasHoldemToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val sharedEngine = GameEngine()

        val visualLangButton = JButton(I18n.get("ui.lang_switch")).apply {
            font = Font("Monospaced", Font.PLAIN, 11)
            isFocusPainted = false
        }
        val textLangButton = JButton(I18n.get("ui.lang_switch")).apply {
            font = Font("Monospaced", Font.PLAIN, 11)
            isFocusPainted = false
        }

        val pokerTablePanel = PokerTablePanel(sharedEngine)
        val visualWrapper = wrapWithLangBar(pokerTablePanel, visualLangButton)
        val visualContent = contentFactory.createContent(visualWrapper, I18n.get("ui.visual_tab"), false)
        toolWindow.contentManager.addContent(visualContent)

        val textPokerPanel = TextPokerPanel(sharedEngine)
        val textWrapper = wrapWithLangBar(textPokerPanel, textLangButton)
        val textContent = contentFactory.createContent(textWrapper, I18n.get("ui.text_tab"), false)
        toolWindow.contentManager.addContent(textContent)

        val onLanguageSwitch = {
            I18n.toggleLanguage()
            visualLangButton.text = I18n.get("ui.lang_switch")
            textLangButton.text = I18n.get("ui.lang_switch")
            visualContent.displayName = I18n.get("ui.visual_tab")
            textContent.displayName = I18n.get("ui.text_tab")
            pokerTablePanel.repaint()
            textPokerPanel.repaint()
        }

        visualLangButton.addActionListener { onLanguageSwitch() }
        textLangButton.addActionListener { onLanguageSwitch() }

        toolWindow.contentManager.setSelectedContent(textContent)

        pokerTablePanel.showSetupAndStart()
    }

    private fun wrapWithLangBar(mainPanel: JPanel, langButton: JButton): JPanel {
        val wrapper = JPanel(BorderLayout())
        val topBar = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2))
        topBar.add(langButton)
        wrapper.add(topBar, BorderLayout.NORTH)
        wrapper.add(mainPanel, BorderLayout.CENTER)
        return wrapper
    }
}
