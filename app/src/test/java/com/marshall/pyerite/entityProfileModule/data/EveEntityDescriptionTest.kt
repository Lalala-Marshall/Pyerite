package com.marshall.pyerite.entityProfileModule.data

import com.marshall.pyerite.ui.golbalComponents.UniverseEntityKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EveEntityDescriptionTest {
    @Test
    fun pythonUnicodeLiteral_decodesChineseAndDropsFontTags() {
        val raw = "u'<font size=\"18\" color=\"#ffff0000\"><b>Peoples Liberation Army</font> - PLA\\u519b\\u56e2'"
        val document = parseEveEntityDescription(raw)

        assertEquals("Peoples Liberation Army - PLA军团", document.text)
        val colored = document.styles.single { it.colorArgb != null }
        assertEquals("Peoples Liberation Army", document.text.substring(colored.start, colored.end))
        assertEquals(0xFFFF0000.toInt(), colored.colorArgb)
        assertEquals(EveEntityDescriptionConfig.MAX_FONT_SCALE, colored.fontScale!!, 0.001f)
        assertTrue(document.styles.any { it.bold && it.end == document.text.length })
        assertTrue(document.links.isEmpty())
    }

    @Test
    fun escapedBackslash_doesNotDecodeTheFollowingUnicodeEscape() {
        val document = parseEveEntityDescription("u'a\\\\u519b'")
        assertEquals("a\\u519b", document.text)
    }

    @Test
    fun plainText_isLeftUntouched() {
        val raw = "under 10\ncode \\u519b  a  b"
        assertEquals(raw, parseEveEntityDescription(raw).text)
    }

    @Test
    fun eveHtml_rendersBreaksBoldAndEntities() {
        val document = parseEveEntityDescription(
            "<font size=\"14\" color=\"#ff00ff00\"><b>Hello</b></font><br><br>a &amp; b",
        )
        assertEquals("Hello\n\na & b", document.text)
        assertTrue(document.styles.any { it.bold && document.text.substring(it.start, it.end) == "Hello" })
    }

    @Test
    fun links_openWebPagesAndOwnerShowInfo() {
        val document = parseEveEntityDescription(
            "<a href=\"https://auth.pla-eve.com/\">auth</a> " +
                "<a href=showinfo:1376//1664583033>Silvitni</a> " +
                "<url=showinfo:2//98190062>PLA</url> " +
                "<a href=\"showinfo:16159//1354830081\">GSF</a> " +
                "<a href=\"joinChannel:player_1\">Public</a>",
        )
        assertEquals("auth Silvitni PLA GSF Public", document.text)
        assertEquals(
            listOf(
                "https://auth.pla-eve.com/",
                "character:1664583033",
                "corporation:98190062",
                "alliance:1354830081",
            ),
            document.links.map { link ->
                when (val target = link.target) {
                    is EveDescriptionTarget.Web -> target.url
                    is EveDescriptionTarget.Entity -> "${target.ref.kind.name.lowercase()}:${target.ref.id}"
                }
            },
        )
        assertEquals(UniverseEntityKind.CHARACTER, (document.links[1].target as EveDescriptionTarget.Entity).ref.kind)
    }

    @Test
    fun strayAngleBracket_staysVisible() {
        assertEquals("skill <3", parseEveEntityDescription("skill <3").text)
        assertEquals("Vexor", parseEveEntityDescription("<loc>Vexor</loc>").text)
    }

    @Test
    fun attribTagsAndPythonQuote_areDecoded() {
        val colored = parseEveEntityDescription("<fontsize=18><color=#ffff0000>Hi</color></fontsize>")
        assertEquals("Hi", colored.text)
        assertEquals(0xFFFF0000.toInt(), colored.styles.first { it.colorArgb != null }.colorArgb)
        assertEquals("it's", parseEveEntityDescription("u'it\\'s'").text)
    }

    @Test
    fun readableColor_keepsAuthorHueOnDarkSurfacesAndBodyTextOnWhite() {
        assertEquals(
            EveEntityDescriptionConfig.YELLOW_ARGB,
            readableEveDescriptionColor(
                EveEntityDescriptionConfig.YELLOW_ARGB,
                0xFF2A2A2A.toInt(),
                0xFFF3F3F3.toInt(),
            ),
        )
        assertEquals(
            0xFF141A42.toInt(),
            readableEveDescriptionColor(
                EveEntityDescriptionConfig.WHITE_ARGB,
                EveEntityDescriptionConfig.WHITE_ARGB,
                0xFF141A42.toInt(),
            ),
        )
        val darkenedYellow = readableEveDescriptionColor(
            EveEntityDescriptionConfig.YELLOW_ARGB,
            EveEntityDescriptionConfig.WHITE_ARGB,
            0xFF141A42.toInt(),
        )
        assertTrue(darkenedYellow != EveEntityDescriptionConfig.YELLOW_ARGB)
        assertEquals(0, darkenedYellow and EveEntityDescriptionConfig.BYTE_MASK)
    }
}
