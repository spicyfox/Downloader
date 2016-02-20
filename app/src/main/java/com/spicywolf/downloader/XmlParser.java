package com.spicywolf.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;
import android.util.Xml;

/**
 * This class parses XML feeds from stackoverflow.com.
 * Given an InputStream representation of a feed, it returns a List of entries,
 * where each list element represents a single entry (post) in the XML feed.
 */
public class XmlParser {
    private static final String ns = null;

    private final static String TAG = "XmlParser";
    // We don't use namespaces

    public List<Item> parse(InputStream in) throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(in, null);

        List<Item> entries = new ArrayList<>();

        int eventType = xpp.getEventType();
        while(eventType != XmlPullParser.END_DOCUMENT) {

            switch (eventType) {
            case XmlPullParser.START_TAG:
                //Log.i(TAG, "START TAG" + xpp.getName());
                if (xpp.getName().equals("item")) {
                    Item entry = readEntry(xpp);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }

                break;
            case XmlPullParser.END_TAG:
                //Log.i(TAG, "END_TAG" + xpp.getName());
                break;
            case XmlPullParser.TEXT:
                //Log.i(TAG, "TEXT" + xpp.getText());
                break;
            default:
                break;
            }
            eventType = xpp.next();
        }

        Log.i(TAG, "parse " + entries.size());

        return entries;
    }

    public List<Item> parse2(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private List<Item> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Item> entries = new ArrayList<>();

        //parser.require(XmlPullParser.START_TAG, ns, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("channel")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private Item readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");
        String title = null;
        String link = null;
        String mediaUrl = null;
        long mediaLength = -1;
        String pubDate = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("link")) {
                link = readLink(parser);
            } else if (name.equals("enclosure")) {
                mediaUrl = readMediaUrl(parser);
                mediaLength = readMediaLength(parser);
            } else if (name.equals("pubDate")) {
                pubDate = readPubDate(parser);
            } else {
                skip(parser);
            }
        }
        return new Item(title, link, mediaUrl, pubDate, mediaLength);
    }

    // Processes title tags in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "title");
        return title;
    }

    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "link");
        String link = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "link");
        return link;
    }

    private String readPubDate(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "pubDate");
        String pubDate = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "pubDate");
        return pubDate;
    }

    private String readMediaUrl(XmlPullParser parser) throws IOException, XmlPullParserException {
        //parser.require(XmlPullParser.START_TAG, ns, "enclosure");
        return parser.getAttributeValue(null, "url");
    }

    private long readMediaLength(XmlPullParser parser) throws IOException, XmlPullParserException {

        String mediaLength = parser.getAttributeValue(null, "length");
        //parser.nextTag();
        if (mediaLength == null || mediaLength.trim().length() == 0) {
            return -1;
        }

        return Long.parseLong(mediaLength);
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
    }

}