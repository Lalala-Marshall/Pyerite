package com.marshall.pyerite.ui.golbalComponents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.marshall.pyerite.R
import com.marshall.pyerite.localization.ContentLanguage
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun Context.copyPlainText(
    label: String,
    text: String,
    successMessage: String,
) {
    if (text.isBlank()) return
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
}

/**
 * Long-press opens a menu that copies [name], and [translation] when it is not blank.
 * [translationLanguage] selects the translation action label.
 * The menu is anchored to [header], directly under the basic-info block.
 * The press is watched on the initial pass so pull-to-refresh and scrolling
 * can still take over once the finger moves past touch slop.
 */
@Composable
internal fun CopyNameLongPressHost(
    name: String,
    modifier: Modifier = Modifier,
    translation: String = "",
    translationLanguage: ContentLanguage = ContentLanguage.ENGLISH,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val latestHaptic by rememberUpdatedState(haptic)
    val canCopy by rememberUpdatedState(name.isNotBlank() || translation.isNotBlank())
    val copyLabel = stringResource(R.string.type_detail_copy_name_label)
    val copySuccess = stringResource(R.string.type_detail_copy_name_success)
    val translationCopy = translationCopyText(translationLanguage)
    val translationLabel = stringResource(translationCopy.label)
    val translationAction = stringResource(translationCopy.action)
    val translationSuccess = stringResource(translationCopy.success)
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis.milliseconds
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        var longPressed = false
                        val timer = launch {
                            delay(longPressTimeout)
                            if (!canCopy) return@launch
                            longPressed = true
                            latestHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuExpanded = true
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                val released = change == null || !change.pressed
                                val moved = change != null &&
                                    pastTouchSlop(down.position, change.position, touchSlop)
                                if (longPressed) {
                                    event.changes.forEach { it.consume() }
                                    if (released) break
                                } else if (released || moved) {
                                    break
                                }
                            }
                        } finally {
                            timer.cancel()
                        }
                    }
                }
            },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            header()
            CopyNameMenu(
                expanded = menuExpanded,
                name = name,
                translation = translation,
                translationAction = translationAction,
                onDismiss = { menuExpanded = false },
                onCopyName = {
                    menuExpanded = false
                    context.copyPlainText(
                        label = copyLabel,
                        text = name,
                        successMessage = copySuccess,
                    )
                },
                onCopyTranslation = {
                    menuExpanded = false
                    context.copyPlainText(
                        label = translationLabel,
                        text = translation,
                        successMessage = translationSuccess,
                    )
                },
            )
        }
        content()
    }
}

@Composable
private fun CopyNameMenu(
    expanded: Boolean,
    name: String,
    translation: String,
    translationAction: String,
    onDismiss: () -> Unit,
    onCopyName: () -> Unit,
    onCopyTranslation: () -> Unit,
) {
    if (!expanded) return
    Popup(
        popupPositionProvider = BelowSectionCenteredPopupPositionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val menuShape = RoundedCornerShape(dimensionResource(R.dimen.top_bar_dropdown_corner))
        val menuColor = colorResource(R.color.copy_name_menu_background)
        val itemColor = colorResource(R.color.text_primary)
        Column(
            modifier = Modifier
                .width(dimensionResource(R.dimen.copy_name_menu_width))
                .shadow(
                    elevation = dimensionResource(R.dimen.copy_name_menu_elevation),
                    shape = menuShape,
                )
                .background(menuColor, menuShape),
        ) {
            if (name.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.type_detail_copy_name_action),
                            color = itemColor,
                        )
                    },
                    onClick = onCopyName,
                )
            }
            if (translation.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = translationAction,
                            color = itemColor,
                        )
                    },
                    onClick = onCopyTranslation,
                )
            }
        }
    }
}

private data class TranslationCopyText(
    @param:StringRes val label: Int,
    @param:StringRes val action: Int,
    @param:StringRes val success: Int,
)

private fun translationCopyText(language: ContentLanguage): TranslationCopyText = when (language) {
    ContentLanguage.ENGLISH -> TranslationCopyText(
        label = R.string.type_detail_copy_english_translation_label,
        action = R.string.type_detail_copy_english_translation_action,
        success = R.string.type_detail_copy_english_translation_success,
    )
    ContentLanguage.CHINESE -> TranslationCopyText(
        label = R.string.type_detail_copy_chinese_translation_label,
        action = R.string.type_detail_copy_chinese_translation_action,
        success = R.string.type_detail_copy_chinese_translation_success,
    )
}

private object BelowSectionCenteredPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val y = anchorBounds.bottom.coerceIn(0, maxY)
        return IntOffset(x, y)
    }
}

private fun pastTouchSlop(start: Offset, current: Offset, slop: Float): Boolean {
    val dx = current.x - start.x
    val dy = current.y - start.y
    return dx * dx + dy * dy > slop * slop
}
