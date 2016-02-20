package com.spicywolf.downloader;


public class Item {
    public final String title;
    public final String link;
    public final String mediaUrl;
    public final String pubDate;
    public final long mediaLength;

    public Item(String title, String link, String mediaUrl, String pubDate, long mediaLength) {
        this.title = title;
        this.link = link;
        this.mediaUrl = mediaUrl;
        this.pubDate = pubDate;
        this.mediaLength = mediaLength;
    }

    @Override
    public String toString() {
        return String.format("title[%s] / link[%s] / mediaUrl[%s] / mediaLength[%d] / pubDate[%s]", title, link,
                             mediaUrl, mediaLength, pubDate);
    }
}
