package com.dungshang.texasholdem.ui

import com.dungshang.texasholdem.model.Card
import com.dungshang.texasholdem.model.Suit
import java.awt.*

/**
 * 扑克牌渲染器，纯长方形 + 文本样式
 */
object CardRenderer {
    const val CARD_WIDTH = 32
    const val CARD_HEIGHT = 20

    fun drawCard(graphics: Graphics2D, card: Card, x: Int, y: Int) {
        graphics.color = Color(0x2A, 0x2A, 0x2A)
        graphics.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT)
        graphics.color = Color(0xA0, 0xA0, 0xA0)
        graphics.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT)

        val isRed = card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS
        graphics.color = if (isRed) Color(0xFF, 0x66, 0x66) else Color.WHITE
        graphics.font = Font("Monospaced", Font.BOLD, 11)
        val label = "${card.rank.displayName}${card.suit.symbol}"
        val metrics = graphics.fontMetrics
        val textX = x + (CARD_WIDTH - metrics.stringWidth(label)) / 2
        val textY = y + (CARD_HEIGHT + metrics.ascent - metrics.descent) / 2
        graphics.drawString(label, textX, textY)
    }

    fun drawCardBack(graphics: Graphics2D, x: Int, y: Int) {
        graphics.color = Color(0x35, 0x35, 0x35)
        graphics.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT)
        graphics.color = Color(0x70, 0x70, 0x70)
        graphics.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT)

        graphics.color = Color(0x60, 0x60, 0x60)
        graphics.font = Font("Monospaced", Font.PLAIN, 10)
        val label = "##"
        val metrics = graphics.fontMetrics
        val textX = x + (CARD_WIDTH - metrics.stringWidth(label)) / 2
        val textY = y + (CARD_HEIGHT + metrics.ascent - metrics.descent) / 2
        graphics.drawString(label, textX, textY)
    }

    fun drawEmptySlot(graphics: Graphics2D, x: Int, y: Int) {
        graphics.color = Color(0x40, 0x40, 0x40)
        graphics.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(2f, 2f), 0f)
        graphics.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT)
        graphics.stroke = BasicStroke(1f)
    }
}
