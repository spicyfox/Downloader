package com.spicywolf.downloader.event;


import android.content.Context;

public class BtHeadsetConnectionStatusEvent {
    public Context context;
    public boolean isConnected;

    public BtHeadsetConnectionStatusEvent(Context context, boolean isConnected) {
        this.context = context;
        this.isConnected = isConnected;
    }
}
