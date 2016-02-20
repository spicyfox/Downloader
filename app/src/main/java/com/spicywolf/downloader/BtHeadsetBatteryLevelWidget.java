package com.spicywolf.downloader;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import com.spicywolf.downloader.event.BtHeadsetBatteryLevelChangedEvent;
import com.spicywolf.downloader.event.BtHeadsetConnectionStatusEvent;
import com.spicywolf.downloader.event.RegisterBtHeadsetReceiverEvent;
import com.spicywolf.downloader.event.UnregisterBtHeadsetReceiverEvent;
import com.squareup.otto.Subscribe;

public class BtHeadsetBatteryLevelWidget extends AppWidgetProvider {

    private final static String TAG = "BatteryLevelWidget";
    private final static int ON_BATTERY_IMAGES
            [] = { R.drawable.bluetooth_0, R.drawable.bluetooth_1, R.drawable.bluetooth_2, R.drawable.bluetooth_3,
            R.drawable.bluetooth_4 };
    private final static int OFF_BATTERY_IMAGES[] =
            { R.drawable.bluetooth_off_0, R.drawable.bluetooth_off_1, R.drawable.bluetooth_off_2,
                    R.drawable.bluetooth_off_3, R.drawable.bluetooth_off_4 };

    private boolean mConnected = false;

    private static int sBatteryLevel = 0;
    private static int sInstanceCount;
    private int mInstanceNo;

    public BtHeadsetBatteryLevelWidget() {
        super();
        mInstanceNo = sInstanceCount;
        appendLog("BtHeadsetBatteryLevelWidget instance: " + mInstanceNo);

        sInstanceCount++;

        BusWrapper.getBus().register(this);

    }

    private void appendLog(String msg, Object... args) {
        if (args.length > 0) {
            msg = String.format(msg, args);
        }

        Log.i(TAG, String.format("[%03d] %s", mInstanceNo, msg));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        appendLog("onDeleted");
    }

    @Override
    public void onEnabled(Context context) {
        appendLog("onEnabled");

        BusWrapper.getBus().post(new RegisterBtHeadsetReceiverEvent());

    }

    @Override
    public void onDisabled(Context context) {
        appendLog("onDisabled");

        BusWrapper.getBus().post(new UnregisterBtHeadsetReceiverEvent());

    }

    @Subscribe
    public void onBtHeadsetBatteryLevelChangedEvent(BtHeadsetBatteryLevelChangedEvent event) {
        sBatteryLevel = event.batteryLevel;
        appendLog("onBtHeadsetBatteryLevelChangedEvent: " + sBatteryLevel);


        updateView(event.context, AppWidgetManager.getInstance(event.context), null);

    }

    @Subscribe
    public void onBtHeadsetConnectionStatusEvent(BtHeadsetConnectionStatusEvent event) {

        if (mConnected && event.isConnected) {
            appendLog("onBtHeadsetConnectionStatusEvent: already connected");
            return;
        }

        mConnected = event.isConnected;
        appendLog("onBtHeadsetConnectionStatusEvent: " + (mConnected ? "connected" : "disconnected"));

        updateView(event.context, AppWidgetManager.getInstance(event.context), null);

    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        appendLog("onUpdate " + appWidgetIds.length);

        updateView(context, appWidgetManager, appWidgetIds);
    }

    private void updateView(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        int batteryImages[] = mConnected ? ON_BATTERY_IMAGES : OFF_BATTERY_IMAGES;

        appendLog("updateView size: " + appWidgetIds.length);

        for(int id: appWidgetIds) {
            appendLog("updateView " + id);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.battery_widget);

            int imageIndex = 0;
            if (sBatteryLevel >= 90) {
                imageIndex = 4;
            } else if (sBatteryLevel >= 80) {
                imageIndex = 3;
            } else if (sBatteryLevel >= 60) {
                imageIndex = 2;
            } else if (sBatteryLevel >= 40) {
                imageIndex = 1;
            }
            views.setImageViewResource(R.id.Battery_imageView, batteryImages[imageIndex]);
            appWidgetManager.updateAppWidget(id, views);
        }

    }

}
