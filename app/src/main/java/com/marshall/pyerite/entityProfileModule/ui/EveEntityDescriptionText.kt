package com.marshall.pyerite.entityProfileModule.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.marshall.pyerite.entityProfileModule.data.EveDescriptionDocument
import com.marshall.pyerite.entityProfileModule.data.EveDescriptionStyle
import com.marshall.pyerite.entityProfileModule.data.EveDescriptionTarget
import com.marshall.pyerite.entityProfileModule.data.EveEntityDescriptionConfig
import com.marshall.pyerite.entityProfileModule.data.readableEveDescriptionColor
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityRef

private const val DESCRIPTION_LINK_TAG = "eve-entity-description"

internal fun EveDescriptionDocument.toAnnotatedString(
    defaultColor: Color,
    linkColor: Color,
    background: Color,
    baseFontSize: TextUnit,
    onWebLink: (String) -> Unit,
    onEntityLink: (UniverseEntityRef) -> Unit,
): AnnotatedString {
    val backgroundArgb = background.toArgb()
    val defaultTextArgb = defaultColor.toArgb()
    return buildAnnotatedString {
        append(text)
        for (style in styles) {
            val start = style.start.coerceAtLeast(0)
            val end = style.end.coerceAtMost(text.length)
            if (start >= end) continue
            addStyle(style.toSpanStyle(baseFontSize, backgroundArgb, defaultTextArgb), start, end)
        }
        for (link in links) {
            val start = link.start.coerceAtLeast(0)
            val end = link.end.coerceAtMost(text.length)
            if (start >= end) continue
            val target = link.target
            addLink(
                LinkAnnotation.Clickable(
                    tag = DESCRIPTION_LINK_TAG,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColorFor(start, end, linkColor, backgroundArgb, defaultTextArgb),
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                    linkInteractionListener = { _ ->
                        when (target) {
                            is EveDescriptionTarget.Web -> onWebLink(target.url)
                            is EveDescriptionTarget.Entity -> onEntityLink(target.ref)
                        }
                    },
                ),
                start,
                end,
            )
        }
    }
}

internal fun EveDescriptionDocument.preferredLineHeight(baseFontSize: TextUnit): TextUnit {
    val enlarged = styles.any { style ->
        (style.fontScale ?: EveEntityDescriptionConfig.NORMAL_FONT_SCALE) >
            EveEntityDescriptionConfig.NORMAL_FONT_SCALE
    }
    if (!enlarged) return TextUnit.Unspecified
    return baseFontSize *
        EveEntityDescriptionConfig.MAX_FONT_SCALE *
        EveEntityDescriptionConfig.LINE_HEIGHT_PADDING
}

private fun EveDescriptionStyle.toSpanStyle(
    baseFontSize: TextUnit,
    backgroundArgb: Int,
    defaultTextArgb: Int,
): SpanStyle = SpanStyle(
    color = colorArgb?.let { argb ->
        Color(readableEveDescriptionColor(argb, backgroundArgb, defaultTextArgb))
    } ?: Color.Unspecified,
    fontSize = fontScale?.let { scale -> baseFontSize * scale } ?: TextUnit.Unspecified,
    fontWeight = if (bold) FontWeight.Bold else null,
    fontStyle = if (italic) FontStyle.Italic else null,
    textDecoration = if (underline) TextDecoration.Underline else null,
)

private fun EveDescriptionDocument.linkColorFor(
    start: Int,
    end: Int,
    linkColor: Color,
    backgroundArgb: Int,
    defaultTextArgb: Int,
): Color {
    val authorColor = styles.lastOrNull { style ->
        style.colorArgb != null && style.start < end && style.end > start
    }?.colorArgb ?: return linkColor
    return Color(readableEveDescriptionColor(authorColor, backgroundArgb, defaultTextArgb))
}
