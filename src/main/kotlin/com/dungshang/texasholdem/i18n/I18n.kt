package com.dungshang.texasholdem.i18n

/**
 * 国际化管理器
 * 支持中文和英文，默认中文
 */
object I18n {
    enum class Language(val displayName: String) {
        ZH("中文"),
        EN("English")
    }

    var currentLanguage: Language = Language.EN
        private set

    private val listeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun setLanguage(language: Language) {
        if (currentLanguage != language) {
            currentLanguage = language
            listeners.forEach { it() }
        }
    }

    fun toggleLanguage() {
        setLanguage(if (currentLanguage == Language.ZH) Language.EN else Language.ZH)
    }

    fun get(key: String): String {
        val map = if (currentLanguage == Language.ZH) zhStrings else enStrings
        return map[key] ?: key
    }

    fun get(key: String, vararg args: Any): String {
        val template = get(key)
        return String.format(template, *args)
    }

    private val zhStrings = mapOf(
        // 游戏阶段
        "phase.waiting" to "等待开始",
        "phase.pre_flop" to "翻牌前",
        "phase.flop" to "翻牌",
        "phase.turn" to "转牌",
        "phase.river" to "河牌",
        "phase.showdown" to "摊牌",
        "phase.game_over" to "游戏结束",

        // 游戏阶段（圈）
        "phase.flop_round" to "翻牌圈",
        "phase.turn_round" to "转牌圈",
        "phase.river_round" to "河牌圈",

        // 玩家动作
        "action.fold" to "弃牌",
        "action.check" to "过牌",
        "action.call" to "跟注",
        "action.raise" to "加注",
        "action.all_in" to "全下",

        // 牌型
        "hand.royal_flush" to "皇家同花顺",
        "hand.straight_flush" to "同花顺",
        "hand.four_of_a_kind" to "四条",
        "hand.full_house" to "葫芦",
        "hand.flush" to "同花",
        "hand.straight" to "顺子",
        "hand.three_of_a_kind" to "三条",
        "hand.two_pair" to "两对",
        "hand.one_pair" to "一对",
        "hand.high_card" to "高牌",

        // GameEngine 消息
        "engine.game_setup" to "游戏已设置，共 %d 名玩家",
        "engine.cannot_continue" to "无法继续游戏，请点击「新游戏」重新开始。",
        "engine.new_hand" to "新一手牌开始",
        "engine.not_your_turn" to "当前不是您的回合",
        "engine.player_wins" to "%s 获胜，赢得 %d 筹码",
        "engine.small_blind" to "%s 下小盲 %d",
        "engine.big_blind" to "%s 下大盲 %d",
        "engine.chips_exhausted" to "您的筹码已耗尽，游戏结束！请点击「新游戏」重新开始。",
        "engine.bot_eliminated" to "%s 筹码耗尽，已离开牌桌。",
        "engine.final_winner" to "%s 获得最终胜利！请点击「新游戏」重新开始。",
        "engine.game_over_restart" to "游戏结束！请点击「新游戏」重新开始。",

        // 设置对话框
        "setup.title" to "德州扑克 - 游戏设置",
        "setup.start_game" to "开始游戏",
        "setup.player_settings" to "玩家设置",
        "setup.player_name" to "玩家名称:",
        "setup.initial_chips" to "初始筹码:",
        "setup.bot_settings" to "机器人设置",
        "setup.bot_count" to "机器人数量:",
        "setup.chips" to "筹码:",
        "setup.aggressiveness" to "松紧度:",
        "setup.agg_tight" to "%d - 紧",
        "setup.agg_medium" to "%d - 中等",
        "setup.agg_loose" to "%d - 松",
        "setup.err_name_empty" to "玩家名称不能为空",
        "setup.err_chips_positive" to "玩家筹码必须大于 0",
        "setup.err_bot_name_empty" to "Bot %d 的名称不能为空",
        "setup.err_bot_chips_positive" to "Bot %d 的筹码必须是大于 0 的整数",

        // UI 按钮和标签
        "ui.fold" to "弃牌",
        "ui.check" to "过牌",
        "ui.call" to "跟注",
        "ui.raise" to "加注",
        "ui.all_in" to "全下",
        "ui.new_hand" to "新一手",
        "ui.new_game" to "新游戏",
        "ui.raise_amount" to "加注金额:",
        "ui.pot" to "底池: %d",
        "ui.folded" to "已弃牌",
        "ui.bet" to "bet:%d",
        "ui.game_start" to "游戏开始！",
        "ui.player_info" to "玩家: %s (%d 筹码)",
        "ui.bot_info" to "机器人: %s (%d 筹码, 松紧度: %d)",
        "ui.phase_label" to "阶段: %s",
        "ui.showdown_result" to "--- 摊牌结果 ---",
        "ui.showdown_end" to "----------------",
        "ui.wins_chips" to "%s 赢得 %d 筹码!",
        "ui.game_over_winner" to "游戏结束! %s 获胜!",
        "ui.visual_tab" to "可视化",
        "ui.text_tab" to "简化",
        "ui.lang_switch" to "EN",

        // TextPokerPanel 专用
        "text.welcome_title" to "德州扑克 - 简化模式",
        "text.welcome_hint" to "点击下方「新游戏」按钮开始",

        // 游戏玩法介绍
        "guide.title" to "游戏玩法介绍",
        "guide.overview" to "德州扑克是一种公共牌扑克游戏，每位玩家发 2 张底牌，配合 5 张公共牌，组成最佳 5 张牌型。",
        "guide.flow_title" to "游戏流程",
        "guide.flow_1" to "1. 翻牌前: 发 2 张底牌，首轮下注",
        "guide.flow_2" to "2. 翻  牌: 发 3 张公共牌，第二轮下注",
        "guide.flow_3" to "3. 转  牌: 发第 4 张公共牌，第三轮下注",
        "guide.flow_4" to "4. 河  牌: 发第 5 张公共牌，最后一轮下注",
        "guide.flow_5" to "5. 摊  牌: 比较牌型，最大者赢得底池",
        "guide.actions_title" to "可用操作",
        "guide.action_fold" to "弃牌(Fold)    - 放弃本手牌",
        "guide.action_check" to "过牌(Check)   - 不加注，传给下家",
        "guide.action_call" to "跟注(Call)    - 跟上当前最大下注",
        "guide.action_raise" to "加注(Raise)   - 提高下注金额",
        "guide.action_allin" to "全下(All-In)  - 押上全部筹码",
        "guide.hand_title" to "牌型大小 (从大到小)",
        "guide.hand_list" to "皇家同花顺 > 同花顺 > 四条 > 葫芦 > 同花 > 顺子 > 三条 > 两对 > 一对 > 高牌",
        "guide.start_hint" to "点击「新游戏 / New」按钮开始游戏！",
        "text.community_cards" to "公共牌: %s",
        "text.community_empty" to "---",
        "text.player_folded" to "(已弃牌)",
        "text.player_all_in" to "(全下)",
        "text.bet_amount" to "下注:%d",
        "text.showdown_title" to "══ 摊牌结果 ══",
        "text.showdown_end" to "══════════════",
        "text.showdown_wins" to "  >> %s 赢得 %d",

        // 边池
        "engine.side_pot_win" to "%s 赢得边池 %d 筹码",

        // 动作显示（带金额）
        "action.call_amount" to "跟注 %d",
        "action.raise_amount" to "加注 %d",
        "action.raise_display" to "加注%d"
    )

    private val enStrings = mapOf(
        // 游戏阶段
        "phase.waiting" to "Waiting",
        "phase.pre_flop" to "Pre-Flop",
        "phase.flop" to "Flop",
        "phase.turn" to "Turn",
        "phase.river" to "River",
        "phase.showdown" to "Showdown",
        "phase.game_over" to "Game Over",

        // 游戏阶段（圈）
        "phase.flop_round" to "Flop Round",
        "phase.turn_round" to "Turn Round",
        "phase.river_round" to "River Round",

        // 玩家动作
        "action.fold" to "Fold",
        "action.check" to "Check",
        "action.call" to "Call",
        "action.raise" to "Raise",
        "action.all_in" to "All-In",

        // 牌型
        "hand.royal_flush" to "Royal Flush",
        "hand.straight_flush" to "Straight Flush",
        "hand.four_of_a_kind" to "Four of a Kind",
        "hand.full_house" to "Full House",
        "hand.flush" to "Flush",
        "hand.straight" to "Straight",
        "hand.three_of_a_kind" to "Three of a Kind",
        "hand.two_pair" to "Two Pair",
        "hand.one_pair" to "One Pair",
        "hand.high_card" to "High Card",

        // GameEngine 消息
        "engine.game_setup" to "Game setup complete, %d players",
        "engine.cannot_continue" to "Cannot continue. Click 'New Game' to restart.",
        "engine.new_hand" to "New hand started",
        "engine.not_your_turn" to "It's not your turn",
        "engine.player_wins" to "%s wins, earning %d chips",
        "engine.small_blind" to "%s posts small blind %d",
        "engine.big_blind" to "%s posts big blind %d",
        "engine.chips_exhausted" to "You're out of chips! Click 'New Game' to restart.",
        "engine.bot_eliminated" to "%s is out of chips and left the table.",
        "engine.final_winner" to "%s wins the game! Click 'New Game' to restart.",
        "engine.game_over_restart" to "Game over! Click 'New Game' to restart.",

        // 设置对话框
        "setup.title" to "Texas Hold'em - Game Setup",
        "setup.start_game" to "Start Game",
        "setup.player_settings" to "Player Settings",
        "setup.player_name" to "Player Name:",
        "setup.initial_chips" to "Initial Chips:",
        "setup.bot_settings" to "Bot Settings",
        "setup.bot_count" to "Bot Count:",
        "setup.chips" to "Chips:",
        "setup.aggressiveness" to "Aggr:",
        "setup.agg_tight" to "%d - Tight",
        "setup.agg_medium" to "%d - Medium",
        "setup.agg_loose" to "%d - Loose",
        "setup.err_name_empty" to "Player name cannot be empty",
        "setup.err_chips_positive" to "Player chips must be greater than 0",
        "setup.err_bot_name_empty" to "Bot %d name cannot be empty",
        "setup.err_bot_chips_positive" to "Bot %d chips must be a positive integer",

        // UI 按钮和标签
        "ui.fold" to "Fold",
        "ui.check" to "Check",
        "ui.call" to "Call",
        "ui.raise" to "Raise",
        "ui.all_in" to "All-In",
        "ui.new_hand" to "Next",
        "ui.new_game" to "New",
        "ui.raise_amount" to "Raise:",
        "ui.pot" to "Pot: %d",
        "ui.folded" to "Folded",
        "ui.bet" to "bet:%d",
        "ui.game_start" to "Game started!",
        "ui.player_info" to "Player: %s (%d chips)",
        "ui.bot_info" to "Bot: %s (%d chips, aggr: %d)",
        "ui.phase_label" to "Phase: %s",
        "ui.showdown_result" to "--- Showdown ---",
        "ui.showdown_end" to "----------------",
        "ui.wins_chips" to "%s wins %d chips!",
        "ui.game_over_winner" to "Game over! %s wins!",
        "ui.visual_tab" to "Visual",
        "ui.text_tab" to "Text",
        "ui.lang_switch" to "中文",

        // TextPokerPanel 专用
        "text.welcome_title" to "Texas Hold'em - Text Mode",
        "text.welcome_hint" to "Click 'New' button below to start",

        // Game Guide
        "guide.title" to "How to Play",
        "guide.overview" to "Texas Hold'em is a community card poker game. Each player gets 2 hole cards and shares 5 community cards to make the best 5-card hand.",
        "guide.flow_title" to "Game Flow",
        "guide.flow_1" to "1. Pre-Flop : 2 hole cards dealt, first betting round",
        "guide.flow_2" to "2. Flop     : 3 community cards dealt, second betting",
        "guide.flow_3" to "3. Turn     : 4th community card dealt, third betting",
        "guide.flow_4" to "4. River    : 5th community card dealt, final betting",
        "guide.flow_5" to "5. Showdown : Compare hands, best hand wins the pot",
        "guide.actions_title" to "Available Actions",
        "guide.action_fold" to "Fold    - Give up your hand",
        "guide.action_check" to "Check   - Pass without betting",
        "guide.action_call" to "Call    - Match the current bet",
        "guide.action_raise" to "Raise   - Increase the bet amount",
        "guide.action_allin" to "All-In  - Bet all your chips",
        "guide.hand_title" to "Hand Rankings (High to Low)",
        "guide.hand_list" to "Royal Flush > Straight Flush > Four of a Kind > Full House > Flush > Straight > Three of a Kind > Two Pair > One Pair > High Card",
        "guide.start_hint" to "Click 'New' button to start the game!",
        "text.community_cards" to "Board: %s",
        "text.community_empty" to "---",
        "text.player_folded" to "(Folded)",
        "text.player_all_in" to "(All-In)",
        "text.bet_amount" to "bet:%d",
        "text.showdown_title" to "== Showdown ==",
        "text.showdown_end" to "==============",
        "text.showdown_wins" to "  >> %s wins %d",

        // Side pot
        "engine.side_pot_win" to "%s wins side pot of %d chips",

        // 动作显示（带金额）
        "action.call_amount" to "Call %d",
        "action.raise_amount" to "Raise %d",
        "action.raise_display" to "Raise %d"
    )
}
