package com.marshall.pyerite.regionMarketModule.viewModel

import com.marshall.pyerite.localization.ContentLanguage
import com.marshall.pyerite.regionMarketModule.model.MarketConfig
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private const val GB2312_CHARSET = "GB2312"
private const val BYTE_MASK = 0xFF
private const val GB2312_BYTES_PER_HAN = 2
private const val GB2312_ZONE_BASE = 160
private const val GB2312_POSITION_SPAN = 100

/**
 * GB2312 level-1 section-position codes where each pinyin initial starts.
 * I, U, and V are omitted because no syllable starts with them. The last
 * value is the exclusive end of Z, not its own letter.
 */
private val PINYIN_INITIAL_BOUNDS: IntArray = intArrayOf(
    1601, 1637, 1833, 2078, 2274, 2302, 2433, 2594, 2787,
    3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027, 4086,
    4390, 4558, 4684, 4925, 5249, 5590,
)

private const val PINYIN_INITIALS = "ABCDEFGHJKLMNOPQRSTWXYZ"

private val gb2312: Charset = Charset.forName(GB2312_CHARSET)

internal fun marketSectionLetter(name: String, language: ContentLanguage): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return MarketConfig.SECTION_OTHER
    val first = trimmed.first()
    if (first in 'A'..'Z' || first in 'a'..'z') return first.uppercaseChar().toString()
    if (language != ContentLanguage.CHINESE) return MarketConfig.SECTION_OTHER
    return hanPinyinInitial(first)?.toString() ?: MarketConfig.SECTION_OTHER
}

private fun hanPinyinInitial(char: Char): Char? {
    val encoded = try {
        gb2312.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(char.toString()))
    } catch (_: CharacterCodingException) {
        return null
    }
    if (encoded.remaining() < GB2312_BYTES_PER_HAN) return null
    val sector = (encoded.get().toInt() and BYTE_MASK) - GB2312_ZONE_BASE
    val position = (encoded.get().toInt() and BYTE_MASK) - GB2312_ZONE_BASE
    val code = sector * GB2312_POSITION_SPAN + position
    val index = when (val found = PINYIN_INITIAL_BOUNDS.binarySearch(code)) {
        in PINYIN_INITIALS.indices -> found
        else -> -found - 2
    }
    if (index !in PINYIN_INITIALS.indices) return null
    if (code >= PINYIN_INITIAL_BOUNDS[index + 1]) return null
    return PINYIN_INITIALS[index]
}
