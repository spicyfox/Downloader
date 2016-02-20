package com.spicywolf.downloader.event;

import android.content.Context;

public class BtHeadsetBatteryLevelChangedEvent {
    public int batteryLevel;
    public Context context;

    public BtHeadsetBatteryLevelChangedEvent(Context context, int batteryLevel) {
        this.context = context;
        this.batteryLevel = batteryLevel;
    }
}
