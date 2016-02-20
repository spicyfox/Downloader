package com.spicywolf.downloader

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

import android.util.Log
import android.util.Xml

/**
 * This class parses XML feeds from stackoverflow.com.
 * Given an InputStream representation of a feed, it returns a List of entries,
 * where each list element represents a single entry (post) in the XML feed.
 */
class XmlParser {
    // We don't use namespaces

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(`in`: InputStream): List<Item> {

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()
        xpp.setInput(`in`, null)
        val entries = ArrayList<Item>()

        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {

            when (eventType) {
                XmlPullParser.START_TAG -> //Log.i(TAG, "START TAG" + xpp.getName());
                    if (xpp.name == "item") {
                        entries.add(readEntry(xpp))
                    }
                XmlPullParser.END_TAG -> {
                }
                XmlPullParser.TEXT -> {
                }
                else -> {
                }
            }//Log.i(TAG, "END_TAG" + xpp.getName());
            //Log.i(TAG, "TEXT" + xpp.getText());
            eventType = xpp.next()
        }

        Log.i(TAG, "parse " + entries.size)

        return entries
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse2(`in`: InputStream): List<Item> {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(`in`, null)
            parser.nextTag()
            return readFeed(parser)
        } finally {
            `in`.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<Item> {
        val entries = ArrayList<Item>()

        //parser.require(XmlPullParser.START_TAG, ns, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            // Starts by looking for the entry tag
            if (name == "channel") {
                entries.add(readEntry(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): Item {
        parser.require(XmlPullParser.START_TAG, ns, "item")
        var title: String? = null
        var link: String? = null
        var mediaUrl: String? = null
        var mediaLength: Long = -1
        var pubDate: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == "title") {
                title = readTitle(parser)
            } else if (name == "link") {
                link = readLink(parser)
            } else if (name == "enclosure") {
                mediaUrl = readMediaUrl(parser)
                mediaLength = readMediaLength(parser)
            } else if (name == "pubDate") {
                pubDate = readPubDate(parser)
            } else {
                skip(parser)
            }
        }
        return Item(title, link, mediaUrl, pubDate, mediaLength)
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "title")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "title")
        return title
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLink(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "link")
        val link = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "link")
        return link
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPubDate(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "pubDate")
        val pubDate = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "pubDate")
        return pubDate
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readMediaUrl(parser: XmlPullParser): String {
        //parser.require(XmlPullParser.START_TAG, ns, "enclosure");
        return parser.getAttributeValue(null, "url")
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readMediaLength(parser: XmlPullParser): Long {

        val mediaLength = parser.getAttributeValue(null, "length")
        //parser.nextTag();
        if (mediaLength == null || mediaLength.trim { it <= ' ' }.length == 0) {
            return -1
        }

        return java.lang.Long.parseLong(mediaLength)
    }

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    companion object {
        private val ns: String? = null

        private val TAG = "XmlParser"
    }

}