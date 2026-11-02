package com.movtery.zalithlauncher.all.controls

import androidx.compose.ui.graphics.Color
import com.movtery.layer_controller.data.ButtonShape
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayout

/*
 * Controls Studio V1.3: send-text buttons, width/height on their own, and button look (colours, text, border, corners) per state.
 * Everything is stored in the format the game already reads: send text is a ClickEvent(SendText), size is ButtonSize percentages of
 * screen height, look is the ButtonStyle's StyleConfig (normal and pressed fields). Nothing new is drawn by the game.
 */

/** Chat messages are at most 256 characters in Minecraft; a button label and message stay well short of that */
const val SEND_TEXT_MAX = 100

/** Size limits in percent of screen height (x100), the same range the game accepts */
const val SIZE_PCT_MIN = 300
const val SIZE_PCT_MAX = 10000

/** Look limits chosen for the editor (the game accepts more, but larger values only break the layout) */
const val FONT_MIN = 8
const val FONT_MAX = 30
const val BORDER_MIN = 0
const val BORDER_MAX = 12
const val RADIUS_MIN = 0
const val RADIUS_MAX = 40

/** The text to send, trimmed; null when it cannot be sent: blank, too long, or containing a control character (newline, tab...) */
fun validSendText(text: String): String? {
    val t = text.trim()
    return if (t.isEmpty() || t.length > SEND_TEXT_MAX || t.any { it.isISOControl() }) null else t
}

/** Send-text is one event on purpose: the game sends the whole message on press, so extra keys would only confuse the button */
fun sendTextEvents(text: String): List<ClickEvent> = listOf(ClickEvent(ClickEvent.Type.SendText, validSendText(text) ?: error("invalid message")))

/** A new button that sends [text]; [name] is the label on the button (e.g. "FF"), defaults to the message itself */
fun ControlLayout.addSendText(text: String, name: String, at: com.movtery.layer_controller.data.ButtonPosition?): Pair<ControlLayout, String> {
    val message = validSendText(text) ?: error("invalid message")
    val label = name.trim().ifEmpty { message }.take(24)
    val (l0, id) = addAction(StudioAction.CHAT, at = at)
    val wide = (1100 * (1 + (label.length - 4).coerceAtLeast(0) * 0.12f)).toInt().coerceAtMost(3000)
    return l0.updateButton(id) {
        it.copy(text = createTranslatable(label), _clickEvents = sendTextEvents(message), buttonSize = it.buttonSize.copy(widthPercentage = wide))
    } to id
}

/** Change the message of an existing send-text button; the name is kept unless [name] is given */
fun ControlLayout.setButtonSendText(id: String, text: String, name: String?): ControlLayout {
    val message = validSendText(text) ?: return this
    return updateButton(id) { b -> b.copy(text = createTranslatable(name?.trim()?.takeIf { it.isNotEmpty() }?.take(24) ?: b.text.default), _clickEvents = sendTextEvents(message)) }
}

/** The message a button sends, or null for other buttons */
fun sendTextOf(button: NormalData): String? = button.clickEvents.firstOrNull { it.type == ClickEvent.Type.SendText }?.key

/** Width and height set on their own, both percent of screen height (what the game reads); values are kept inside the game's range */
fun ControlLayout.setButtonSize(id: String, widthPct: Int, heightPct: Int): ControlLayout = updateButton(id) {
    it.copy(buttonSize = it.buttonSize.copy(
        type = ButtonSize.Type.Percentage,
        widthPercentage = widthPct.coerceIn(SIZE_PCT_MIN, SIZE_PCT_MAX), heightPercentage = heightPct.coerceIn(SIZE_PCT_MIN, SIZE_PCT_MAX),
        widthReference = ButtonSize.Reference.ScreenHeight, heightReference = ButtonSize.Reference.ScreenHeight
    ))
}

/** How one state (normal or pressed) of a button looks; the editor reads and writes this as one value */
data class ButtonLook(
    val background: Color,
    val content: Color,
    val fontSize: Int,
    val borderColor: Color,
    val borderWidth: Int,
    val radius: Int
)

/** The look of a button in one state; buttons without a style show the game's default look */
fun ControlLayout.lookOf(button: NormalData, pressed: Boolean): ButtonLook {
    val cfg = (styles.firstOrNull { it.uuid == button.buttonStyle } ?: com.movtery.layer_controller.data.DefaultButtonStyle).lightStyle
    return if (pressed) ButtonLook(
        background = cfg.pressedBackgroundColor, content = cfg.pressedContentColor, fontSize = cfg.pressedFontSize ?: DEFAULT_TEXT_SIZE,
        borderColor = cfg.pressedBorderColor, borderWidth = cfg.pressedBorderWidth, radius = cfg.pressedBorderRadius.topStart.toInt()
    ) else ButtonLook(
        background = cfg.backgroundColor, content = cfg.contentColor, fontSize = cfg.fontSize ?: DEFAULT_TEXT_SIZE,
        borderColor = cfg.borderColor, borderWidth = cfg.borderWidth, radius = cfg.borderRadius.topStart.toInt()
    )
}

/** The game's default text size when a style does not set one (matches the game's own default) */
const val DEFAULT_TEXT_SIZE = 14

/**
 * Writes one state's look. Only the fields of that state change (normal or pressed); the other state and the opacity are kept.
 * Values are clamped to the editor ranges. Stored like the old editor: in a ButtonStyle, applied to light and dark alike; an identical
 * existing style is reused.
 */
fun ControlLayout.setButtonLook(id: String, pressed: Boolean, look: ButtonLook): ControlLayout {
    val font = look.fontSize.coerceIn(FONT_MIN, FONT_MAX)
    val width = look.borderWidth.coerceIn(BORDER_MIN, BORDER_MAX)
    val radius = look.radius.coerceIn(RADIUS_MIN, RADIUS_MAX).toFloat()
    return restyle(id, "ALL look") { c ->
        if (pressed) c.copy(
            pressedBackgroundColor = look.background, pressedContentColor = look.content, pressedFontSize = font,
            pressedBorderColor = look.borderColor, pressedBorderWidth = width, pressedBorderRadius = ButtonShape(radius)
        ) else c.copy(
            backgroundColor = look.background, contentColor = look.content, fontSize = font,
            borderColor = look.borderColor, borderWidth = width, borderRadius = ButtonShape(radius)
        )
    }
}
