package com.marshall.pyerite.entityProfileModule.data

import com.marshall.pyerite.ui.golbalComponents.UniverseEntityKind
import com.marshall.pyerite.ui.golbalComponents.UniverseEntityRef
import java.util.Locale
import kotlin.math.pow

/**
 * ESI stores some character and corporation bios as a Python unicode literal
 * (`u'...'`) whose text is EVE HTML. `\uXXXX` stays literal until it is unescaped,
 * and tags such as `<font>` are not HTML.
 */
internal fun parseEveEntityDescription(raw: String): EveDescriptionDocument {
    if (raw.isEmpty()) return EveDescriptionDocument()
    val source = unwrapPythonUnicode(raw)
    if (source.isEmpty()) return EveDescriptionDocument()
    return parseEveHtml(source)
}

internal fun readableEveDescriptionColor(
    eveArgb: Int,
    backgroundArgb: Int,
    defaultTextArgb: Int,
): Int {
    val flattened = compositeOver(eveArgb, backgroundArgb)
    if (chroma(flattened) <= EveEntityDescriptionConfig.GRAYSCALE_CHROMA) {
        return defaultTextArgb
    }
    if (contrastRatio(flattened, backgroundArgb) >= EveEntityDescriptionConfig.MIN_CONTRAST) {
        return flattened
    }
    val toward = if (relativeLuminance(backgroundArgb) > EveEntityDescriptionConfig.BACKGROUND_LUMINANCE_MID) {
        EveEntityDescriptionConfig.BLACK_ARGB
    } else {
        EveEntityDescriptionConfig.WHITE_ARGB
    }
    var current = flattened
    repeat(EveEntityDescriptionConfig.COLOR_ADJUST_STEPS) {
        current = mixToward(current, toward)
        if (contrastRatio(current, backgroundArgb) >= EveEntityDescriptionConfig.MIN_CONTRAST) {
            return current
        }
    }
    return current
}

internal data class EveDescriptionDocument(
    val text: String = "",
    val styles: List<EveDescriptionStyle> = emptyList(),
    val links: List<EveDescriptionLink> = emptyList(),
)

internal data class EveDescriptionStyle(
    val start: Int,
    val end: Int,
    val colorArgb: Int? = null,
    val fontScale: Float? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
)

internal data class EveDescriptionLink(
    val start: Int,
    val end: Int,
    val target: EveDescriptionTarget,
)

internal sealed interface EveDescriptionTarget {
    data class Web(val url: String) : EveDescriptionTarget

    data class Entity(val ref: UniverseEntityRef) : EveDescriptionTarget
}

internal object EveEntityDescriptionConfig {
    const val MAX_PYTHON_UNWRAPS = 2
    const val UNICODE_ESCAPE_DIGITS = 4
    const val UNICODE_WIDE_ESCAPE_DIGITS = 8
    const val HEX_ESCAPE_DIGITS = 2
    const val MAX_ENTITY_LENGTH = 8
    const val REPLACEMENT_CHAR = '\uFFFD'

    const val NORMAL_FONT_SCALE = 1f
    const val REFERENCE_FONT_SIZE_PX = 12f
    const val MIN_FONT_SCALE = 0.85f
    const val MAX_FONT_SCALE = 1.35f
    const val LINE_HEIGHT_PADDING = 1.15f

    const val BYTE_MASK = 0xFF
    const val BYTE_MAX = 255.0
    const val SHIFT_ALPHA = 24
    const val SHIFT_RED = 16
    const val SHIFT_GREEN = 8
    const val SHIFT_BLUE = 0
    const val OPAQUE_ALPHA = 0xFF
    const val HEX_RGB_LENGTH = 6
    const val HEX_ARGB_LENGTH = 8
    const val WHITE_ARGB = 0xFFFFFFFF.toInt()
    const val BLACK_ARGB = 0xFF000000.toInt()
    const val YELLOW_ARGB = 0xFFFFFF00.toInt()
    const val GRAYSCALE_CHROMA = 24
    const val MIN_CONTRAST = 4.5
    const val BACKGROUND_LUMINANCE_MID = 0.5
    const val COLOR_ADJUST_STEPS = 8
    const val COLOR_ADJUST_AMOUNT = 0.22f

    const val SRGB_CHANNEL_THRESHOLD = 0.03928
    const val SRGB_LOW_DIVISOR = 12.92
    const val SRGB_OFFSET = 0.055
    const val SRGB_SCALE = 1.055
    const val SRGB_GAMMA = 2.4
    const val LUMA_RED = 0.2126
    const val LUMA_GREEN = 0.7152
    const val LUMA_BLUE = 0.0722
    const val CONTRAST_LIFT = 0.05

    const val SHOWINFO_SCHEME = "showinfo:"
    const val HTTP_SCHEME = "http://"
    const val HTTPS_SCHEME = "https://"
    const val EVE_BROWSER_SCHEME = "evebrowser:"
    const val SHOWINFO_SEPARATOR = "//"
    const val CORPORATION_TYPE_ID = 2
    const val ALLIANCE_TYPE_ID = 16159
    const val CHARACTER_TYPE_ID_FIRST = 1373
    const val CHARACTER_TYPE_ID_LAST = 1386
    const val CHARACTER_TYPE_ID_AIR = 34574

    const val TAG_B = "b"
    const val TAG_I = "i"
    const val TAG_U = "u"
    const val TAG_BR = "br"
    const val TAG_FONT = "font"
    const val TAG_FONT_SIZE = "fontsize"
    const val TAG_COLOR = "color"
    const val TAG_A = "a"
    const val TAG_URL = "url"
    const val ATTR_HREF = "href"
    const val ATTR_SIZE = "size"
    const val ATTR_COLOR = "color"

    const val COLOR_HASH_PREFIX = "#"
    const val COLOR_HEX_PREFIX = "0x"
    const val PYTHON_LONG_SUFFIX = "L"
    const val NAMED_WHITE = "white"
    const val NAMED_YELLOW = "yellow"
    const val ENTITY_AMP = "amp"
    const val ENTITY_LT = "lt"
    const val ENTITY_GT = "gt"
    const val ENTITY_NBSP = "nbsp"

    val CONSUMED_TAGS: Set<String> = setOf(
        TAG_B,
        TAG_I,
        TAG_U,
        TAG_BR,
        TAG_FONT,
        TAG_FONT_SIZE,
        TAG_COLOR,
        TAG_A,
        TAG_URL,
        "loc",
        "localized",
        "hint",
        "letterspace",
        "left",
        "center",
        "right",
    )
}

private data class OpenMark(
    val tag: String,
    val start: Int,
    val colorArgb: Int? = null,
    val fontScale: Float? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val link: EveDescriptionTarget? = null,
)

private data class MarkupTag(
    val name: String,
    val closing: Boolean,
    val selfClosing: Boolean,
    val attribValue: String?,
    val attrs: Map<String, String>,
    val endIndex: Int,
)

private fun unwrapPythonUnicode(raw: String): String {
    var current = raw
    repeat(EveEntityDescriptionConfig.MAX_PYTHON_UNWRAPS) {
        val inner = pythonLiteralInner(current.trim()) ?: return current
        current = unescapePythonLiteral(inner)
    }
    return current
}

private fun pythonLiteralInner(text: String): String? {
    if (text.length < 3) return null
    if (text[0] != 'u' && text[0] != 'U') return null
    val quoted = text.substring(1)
    return when {
        quoted.startsWith("'''") && quoted.endsWith("'''") && quoted.length >= 6 ->
            quoted.substring(3, quoted.length - 3)
        quoted.startsWith("\"\"\"") && quoted.endsWith("\"\"\"") && quoted.length >= 6 ->
            quoted.substring(3, quoted.length - 3)
        quoted.length >= 2 &&
            (quoted[0] == '\'' || quoted[0] == '"') &&
            quoted.last() == quoted[0] ->
            quoted.substring(1, quoted.length - 1)
        else -> null
    }
}

private fun unescapePythonLiteral(literal: String): String {
    val out = StringBuilder(literal.length)
    var index = 0
    while (index < literal.length) {
        val current = literal[index]
        if (current != '\\' || index + 1 >= literal.length) {
            out.append(current)
            index++
            continue
        }
        when (val next = literal[index + 1]) {
            '\\', '\'', '"' -> {
                out.append(next)
                index += 2
            }
            'n' -> {
                out.append('\n')
                index += 2
            }
            'r' -> {
                out.append('\r')
                index += 2
            }
            't' -> {
                out.append('\t')
                index += 2
            }
            'u' -> index = appendHexEscape(
                literal,
                index,
                EveEntityDescriptionConfig.UNICODE_ESCAPE_DIGITS,
                out,
            )
            'U' -> index = appendHexEscape(
                literal,
                index,
                EveEntityDescriptionConfig.UNICODE_WIDE_ESCAPE_DIGITS,
                out,
            )
            'x' -> index = appendHexEscape(
                literal,
                index,
                EveEntityDescriptionConfig.HEX_ESCAPE_DIGITS,
                out,
            )
            '\n' -> index += 2
            else -> {
                out.append('\\')
                index++
            }
        }
    }
    return out.toString()
}

private fun appendHexEscape(
    literal: String,
    backslashIndex: Int,
    digits: Int,
    out: StringBuilder,
): Int {
    val start = backslashIndex + 2
    val end = start + digits
    if (end > literal.length || !literal.substring(start, end).isHex()) {
        out.append('\\')
        return backslashIndex + 1
    }
    val codePoint = literal.substring(start, end).toInt(16)
    if (codePoint in 0..Character.MAX_CODE_POINT && codePoint !in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code) {
        out.append(String(Character.toChars(codePoint)))
    } else {
        out.append(EveEntityDescriptionConfig.REPLACEMENT_CHAR)
    }
    return end
}

private fun String.isHex(): Boolean = all { it.isHexDigit() }

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun parseEveHtml(source: String): EveDescriptionDocument {
    val text = StringBuilder(source.length)
    val styles = mutableListOf<EveDescriptionStyle>()
    val links = mutableListOf<EveDescriptionLink>()
    val stack = ArrayDeque<OpenMark>()
    var index = 0
    while (index < source.length) {
        if (source[index] != '<') {
            val next = source.indexOf('<', index).let { if (it < 0) source.length else it }
            appendDecodedText(text, source, index, next)
            index = next
            continue
        }
        val tag = readTag(source, index)
        if (tag == null || tag.name !in EveEntityDescriptionConfig.CONSUMED_TAGS) {
            text.append('<')
            index++
            continue
        }
        index = tag.endIndex
        when {
            tag.name == EveEntityDescriptionConfig.TAG_BR -> text.append('\n')
            tag.closing -> closeTag(tag.name, text.length, stack, styles, links)
            !tag.selfClosing -> openTag(tag, text.length, stack)
        }
    }
    while (stack.isNotEmpty()) {
        emitMark(stack.removeLast(), text.length, styles, links)
    }
    return EveDescriptionDocument(text.toString(), styles, links)
}

private fun openTag(tag: MarkupTag, start: Int, stack: ArrayDeque<OpenMark>) {
    val mark = when (tag.name) {
        EveEntityDescriptionConfig.TAG_B -> OpenMark(tag.name, start, bold = true)
        EveEntityDescriptionConfig.TAG_I -> OpenMark(tag.name, start, italic = true)
        EveEntityDescriptionConfig.TAG_U -> OpenMark(tag.name, start, underline = true)
        EveEntityDescriptionConfig.TAG_FONT -> OpenMark(
            tag = tag.name,
            start = start,
            colorArgb = tag.attrs[EveEntityDescriptionConfig.ATTR_COLOR]?.let(::parseEveColor),
            fontScale = tag.attrs[EveEntityDescriptionConfig.ATTR_SIZE]?.toIntOrNull()?.let(::eveFontScale),
        )
        EveEntityDescriptionConfig.TAG_COLOR -> OpenMark(
            tag = tag.name,
            start = start,
            colorArgb = (tag.attribValue ?: tag.attrs[EveEntityDescriptionConfig.ATTR_COLOR])
                ?.let(::parseEveColor),
        )
        EveEntityDescriptionConfig.TAG_FONT_SIZE -> OpenMark(
            tag = tag.name,
            start = start,
            fontScale = (tag.attribValue ?: tag.attrs[EveEntityDescriptionConfig.ATTR_SIZE])
                ?.toIntOrNull()
                ?.let(::eveFontScale),
        )
        EveEntityDescriptionConfig.TAG_A,
        EveEntityDescriptionConfig.TAG_URL,
        -> {
            val href = tag.attribValue ?: tag.attrs[EveEntityDescriptionConfig.ATTR_HREF]
            OpenMark(tag.name, start, link = href?.let(::parseLinkTarget))
        }
        else -> return
    }
    stack.addLast(mark)
}

private fun closeTag(
    name: String,
    end: Int,
    stack: ArrayDeque<OpenMark>,
    styles: MutableList<EveDescriptionStyle>,
    links: MutableList<EveDescriptionLink>,
) {
    val index = stack.indexOfLast { it.tag == name }
    if (index < 0) return
    emitMark(stack.removeAt(index), end, styles, links)
}

private fun emitMark(
    mark: OpenMark,
    end: Int,
    styles: MutableList<EveDescriptionStyle>,
    links: MutableList<EveDescriptionLink>,
) {
    if (end <= mark.start) return
    if (mark.colorArgb != null || mark.fontScale != null || mark.bold || mark.italic || mark.underline) {
        styles += EveDescriptionStyle(
            start = mark.start,
            end = end,
            colorArgb = mark.colorArgb,
            fontScale = mark.fontScale,
            bold = mark.bold,
            italic = mark.italic,
            underline = mark.underline,
        )
    }
    val link = mark.link
    if (link != null) {
        links += EveDescriptionLink(mark.start, end, link)
    }
}

private fun eveFontScale(sizePx: Int): Float? {
    if (sizePx <= 0) return null
    val scale = sizePx / EveEntityDescriptionConfig.REFERENCE_FONT_SIZE_PX
    return scale.coerceIn(
        EveEntityDescriptionConfig.MIN_FONT_SCALE,
        EveEntityDescriptionConfig.MAX_FONT_SCALE,
    )
}

private fun parseEveColor(raw: String): Int? {
    var value = raw.trim()
    if (value.endsWith(EveEntityDescriptionConfig.PYTHON_LONG_SUFFIX)) {
        value = value.dropLast(EveEntityDescriptionConfig.PYTHON_LONG_SUFFIX.length)
    }
    when {
        value.equals(EveEntityDescriptionConfig.NAMED_WHITE, ignoreCase = true) ->
            return EveEntityDescriptionConfig.WHITE_ARGB
        value.equals(EveEntityDescriptionConfig.NAMED_YELLOW, ignoreCase = true) ->
            return EveEntityDescriptionConfig.YELLOW_ARGB
        value.startsWith(EveEntityDescriptionConfig.COLOR_HASH_PREFIX) ->
            value = value.substring(EveEntityDescriptionConfig.COLOR_HASH_PREFIX.length)
        value.startsWith(EveEntityDescriptionConfig.COLOR_HEX_PREFIX, ignoreCase = true) ->
            value = value.substring(EveEntityDescriptionConfig.COLOR_HEX_PREFIX.length)
        else -> return null
    }
    if (!value.isHex()) return null
    return when (value.length) {
        EveEntityDescriptionConfig.HEX_RGB_LENGTH ->
            (EveEntityDescriptionConfig.OPAQUE_ALPHA shl EveEntityDescriptionConfig.SHIFT_ALPHA) or
                value.toInt(16)
        EveEntityDescriptionConfig.HEX_ARGB_LENGTH -> value.toLong(16).toInt()
        else -> null
    }
}

private fun parseLinkTarget(rawHref: String): EveDescriptionTarget? {
    var href = decodeEntities(rawHref.trim())
    if (href.startsWith(EveEntityDescriptionConfig.EVE_BROWSER_SCHEME, ignoreCase = true)) {
        href = href.substring(EveEntityDescriptionConfig.EVE_BROWSER_SCHEME.length).trim()
    }
    if (href.startsWith(EveEntityDescriptionConfig.HTTPS_SCHEME, ignoreCase = true) ||
        href.startsWith(EveEntityDescriptionConfig.HTTP_SCHEME, ignoreCase = true)
    ) {
        return EveDescriptionTarget.Web(href)
    }
    if (!href.startsWith(EveEntityDescriptionConfig.SHOWINFO_SCHEME, ignoreCase = true)) return null
    return parseShowInfo(href.substring(EveEntityDescriptionConfig.SHOWINFO_SCHEME.length))
}

private fun parseShowInfo(payload: String): EveDescriptionTarget? {
    val parts = payload.split(EveEntityDescriptionConfig.SHOWINFO_SEPARATOR)
    val typeId = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val itemId = parts.getOrNull(1)?.toLongOrNull() ?: return null
    if (itemId <= 0L) return null
    val kind = when {
        typeId == EveEntityDescriptionConfig.CORPORATION_TYPE_ID -> UniverseEntityKind.CORPORATION
        typeId == EveEntityDescriptionConfig.ALLIANCE_TYPE_ID -> UniverseEntityKind.ALLIANCE
        typeId == EveEntityDescriptionConfig.CHARACTER_TYPE_ID_AIR ||
            typeId in EveEntityDescriptionConfig.CHARACTER_TYPE_ID_FIRST..EveEntityDescriptionConfig.CHARACTER_TYPE_ID_LAST ->
            UniverseEntityKind.CHARACTER
        else -> return null
    }
    return EveDescriptionTarget.Entity(UniverseEntityRef(kind, itemId))
}

private fun readTag(source: String, start: Int): MarkupTag? {
    if (start >= source.length || source[start] != '<') return null
    var index = start + 1
    if (index >= source.length) return null
    val closing = source[index] == '/'
    if (closing) index++
    if (index >= source.length || !source[index].isLetter()) return null
    val nameStart = index
    while (index < source.length && source[index].isLetter()) index++
    val name = source.substring(nameStart, index).lowercase(Locale.US)
    var attribValue: String? = null
    val attrs = linkedMapOf<String, String>()
    var selfClosing = false
    if (!closing) {
        index = skipSpace(source, index)
        if (index < source.length && source[index] == '=') {
            index++
            val (value, next) = readAttributeValue(source, index)
            attribValue = value
            index = next
        }
        while (index < source.length && source[index] != '>') {
            if (source[index] == '/') {
                selfClosing = true
                index++
                continue
            }
            if (source[index].isWhitespace()) {
                index++
                continue
            }
            if (!source[index].isLetter()) {
                val end = source.indexOf('>', index)
                if (end < 0) return null
                return MarkupTag(name, closing, selfClosing, attribValue, attrs, end + 1)
            }
            val attrStart = index
            while (index < source.length && (source[index].isLetter() || source[index] == '-')) index++
            val attrName = source.substring(attrStart, index).lowercase(Locale.US)
            index = skipSpace(source, index)
            if (index >= source.length || source[index] != '=') {
                attrs[attrName] = ""
                continue
            }
            index++
            index = skipSpace(source, index)
            val (value, next) = readAttributeValue(source, index)
            attrs[attrName] = value
            index = next
        }
    } else {
        index = skipSpace(source, index)
    }
    if (index >= source.length || source[index] != '>') return null
    return MarkupTag(name, closing, selfClosing, attribValue, attrs, index + 1)
}

private fun readAttributeValue(source: String, start: Int): Pair<String, Int> {
    if (start >= source.length) return "" to start
    val quote = source[start]
    if (quote == '"' || quote == '\'') {
        val end = source.indexOf(quote, start + 1)
        if (end < 0) return decodeEntities(source.substring(start + 1)) to source.length
        return decodeEntities(source.substring(start + 1, end)) to end + 1
    }
    var index = start
    while (index < source.length && !source[index].isWhitespace() && source[index] != '>') index++
    return decodeEntities(source.substring(start, index)) to index
}

private fun skipSpace(source: String, start: Int): Int {
    var index = start
    while (index < source.length && source[index].isWhitespace()) index++
    return index
}

private fun decodeEntities(value: String): String {
    val out = StringBuilder(value.length)
    appendDecodedText(out, value, 0, value.length)
    return out.toString()
}

private fun appendDecodedText(out: StringBuilder, source: String, start: Int, end: Int) {
    var index = start
    while (index < end) {
        val current = source[index]
        if (current == '&') {
            val semi = source.indexOf(';', index + 1)
            val withinLimit = semi > index &&
                semi < end &&
                semi - index <= EveEntityDescriptionConfig.MAX_ENTITY_LENGTH
            if (withinLimit) {
                val decoded = decodeEntity(source.substring(index + 1, semi))
                if (decoded != null) {
                    out.append(decoded)
                    index = semi + 1
                    continue
                }
            }
        }
        if (current == '\r') {
            if (index + 1 < end && source[index + 1] == '\n') {
                out.append('\n')
                index += 2
            } else {
                out.append('\r')
                index++
            }
            continue
        }
        if (current == '\t') out.append(' ') else out.append(current)
        index++
    }
}

private fun decodeEntity(body: String): Char? = when (body.lowercase(Locale.US)) {
    EveEntityDescriptionConfig.ENTITY_AMP -> '&'
    EveEntityDescriptionConfig.ENTITY_LT -> '<'
    EveEntityDescriptionConfig.ENTITY_GT -> '>'
    EveEntityDescriptionConfig.ENTITY_NBSP -> ' '
    else -> null
}

private fun compositeOver(foreground: Int, background: Int): Int {
    val alpha = channel(foreground, EveEntityDescriptionConfig.SHIFT_ALPHA) / EveEntityDescriptionConfig.BYTE_MAX
    if (alpha >= 1.0) return packOpaque(foreground)
    fun mix(shift: Int): Int {
        val fg = channel(foreground, shift)
        val bg = channel(background, shift)
        return (fg * alpha + bg * (1.0 - alpha)).toInt().coerceIn(0, EveEntityDescriptionConfig.BYTE_MASK)
    }
    return packArgb(
        mix(EveEntityDescriptionConfig.SHIFT_RED),
        mix(EveEntityDescriptionConfig.SHIFT_GREEN),
        mix(EveEntityDescriptionConfig.SHIFT_BLUE),
    )
}

private fun mixToward(argb: Int, toward: Int): Int {
    fun mix(shift: Int): Int {
        val from = channel(argb, shift)
        val to = channel(toward, shift)
        return (from + (to - from) * EveEntityDescriptionConfig.COLOR_ADJUST_AMOUNT)
            .toInt()
            .coerceIn(0, EveEntityDescriptionConfig.BYTE_MASK)
    }
    return packArgb(
        mix(EveEntityDescriptionConfig.SHIFT_RED),
        mix(EveEntityDescriptionConfig.SHIFT_GREEN),
        mix(EveEntityDescriptionConfig.SHIFT_BLUE),
    )
}

private fun chroma(argb: Int): Int {
    val red = channel(argb, EveEntityDescriptionConfig.SHIFT_RED)
    val green = channel(argb, EveEntityDescriptionConfig.SHIFT_GREEN)
    val blue = channel(argb, EveEntityDescriptionConfig.SHIFT_BLUE)
    return maxOf(red, green, blue) - minOf(red, green, blue)
}

private fun contrastRatio(first: Int, second: Int): Double {
    val lighter = maxOf(relativeLuminance(first), relativeLuminance(second))
    val darker = minOf(relativeLuminance(first), relativeLuminance(second))
    return (lighter + EveEntityDescriptionConfig.CONTRAST_LIFT) /
        (darker + EveEntityDescriptionConfig.CONTRAST_LIFT)
}

private fun relativeLuminance(argb: Int): Double {
    val red = linearize(channel(argb, EveEntityDescriptionConfig.SHIFT_RED) / EveEntityDescriptionConfig.BYTE_MAX)
    val green = linearize(channel(argb, EveEntityDescriptionConfig.SHIFT_GREEN) / EveEntityDescriptionConfig.BYTE_MAX)
    val blue = linearize(channel(argb, EveEntityDescriptionConfig.SHIFT_BLUE) / EveEntityDescriptionConfig.BYTE_MAX)
    return EveEntityDescriptionConfig.LUMA_RED * red +
        EveEntityDescriptionConfig.LUMA_GREEN * green +
        EveEntityDescriptionConfig.LUMA_BLUE * blue
}

private fun linearize(channel: Double): Double {
    return if (channel <= EveEntityDescriptionConfig.SRGB_CHANNEL_THRESHOLD) {
        channel / EveEntityDescriptionConfig.SRGB_LOW_DIVISOR
    } else {
        ((channel + EveEntityDescriptionConfig.SRGB_OFFSET) / EveEntityDescriptionConfig.SRGB_SCALE)
            .pow(EveEntityDescriptionConfig.SRGB_GAMMA)
    }
}

private fun channel(argb: Int, shift: Int): Int =
    (argb ushr shift) and EveEntityDescriptionConfig.BYTE_MASK

private fun packOpaque(argb: Int): Int = packArgb(
    channel(argb, EveEntityDescriptionConfig.SHIFT_RED),
    channel(argb, EveEntityDescriptionConfig.SHIFT_GREEN),
    channel(argb, EveEntityDescriptionConfig.SHIFT_BLUE),
)

private fun packArgb(red: Int, green: Int, blue: Int): Int =
    (EveEntityDescriptionConfig.OPAQUE_ALPHA shl EveEntityDescriptionConfig.SHIFT_ALPHA) or
        (red shl EveEntityDescriptionConfig.SHIFT_RED) or
        (green shl EveEntityDescriptionConfig.SHIFT_GREEN) or
        blue
