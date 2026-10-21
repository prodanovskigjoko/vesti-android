package mk.vesti.core

import org.kxml2.io.KXmlParser
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RssParserTest {
    private fun parse(xml: String, source: FeedSource = FeedSource("Тест", "https://example.mk/feed")): List<Article> {
        val parser = KXmlParser().apply { setInput(StringReader(xml)) }
        return RssParser().parse(parser, source)
    }

    @Test
    fun parsesWordPressRss() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"
                 xmlns:media="http://search.yahoo.com/mrss/">
            <channel><title>Тест</title><link>https://example.mk</link>
              <item>
                <title>Владата усвои нов &#8222;буџет&#8220;</title>
                <link>https://example.mk/budzet</link>
                <pubDate>Tue, 21 Oct 2026 14:05:00 +0200</pubDate>
                <category><![CDATA[Економија]]></category>
                <description><![CDATA[<p>Седница на <b>Владата</b>&nbsp;денес.</p>]]></description>
                <content:encoded><![CDATA[<img src="https://example.mk/a.jpg"/> текст]]></content:encoded>
                <media:content url="https://example.mk/m.jpg" medium="image"><media:title>Слика</media:title></media:content>
              </item>
            </channel></rss>"""
        val a = parse(xml).single()
        assertEquals("Владата усвои нов „буџет“", a.title)
        assertEquals("https://example.mk/budzet", a.link)
        assertEquals("Седница на Владата денес.", a.summary)
        assertEquals("https://example.mk/a.jpg", a.imageUrl)
        assertEquals(Category.EKONOMIJA, a.category)
        assertEquals(1792584300000L, a.publishedAt)
    }

    @Test
    fun parsesAtom() {
        val xml = """<feed xmlns="http://www.w3.org/2005/Atom"><title>Т</title>
            <entry><title>Наслов</title><link rel="alternate" href="https://example.mk/x"/>
            <updated>2026-10-21T10:00:00Z</updated><summary>Кратко</summary></entry></feed>"""
        val a = parse(xml, FeedSource("С", "u", Category.SPORT)).single()
        assertEquals("https://example.mk/x", a.link)
        assertEquals(Category.SPORT, a.category)
        assertNotNull(a.publishedAt.takeIf { it > 0 })
    }
}
