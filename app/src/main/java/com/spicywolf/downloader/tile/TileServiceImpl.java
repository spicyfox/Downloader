package com.spicywolf.downloader.tile;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class TileServiceImpl extends TileService {
    private final static String TAG = "TileServiceImpl";

    @Override
    public void onTileAdded() {
        Log.d(TAG, "onTileAdded");
    }

    @Override
    public void onStopListening() {
        Log.d(TAG, "onStopListening");
    }

    @Override
    public void onClick() {
        Log.d(TAG, "onClick");

        //Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS, Uri.parse("package:" + getPackageName()));
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityAndCollapse(intent);
    }

    @Override
    public void onStartListening() {
        Log.d(TAG, "onStartListening");

        Tile tile = getQsTile();
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        int curState = tile.getState();
        int state = Tile.STATE_UNAVAILABLE;
        if (connMgr.isActiveNetworkMetered()) {
            switch (connMgr.getRestrictBackgroundStatus()) {
                case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED:
                    // Background data usage is blocked for this app. Wherever possible,
                    // the app should also use less data in the foreground.
                    state = Tile.STATE_ACTIVE;
                    break;

                case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED:
                case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED:
                    state = Tile.STATE_INACTIVE;
                    break;
            }
        }

        if (curState != state) {
            tile.setState(state);
            tile.updateTile();
        }

    }

    @Override
    public void onTileRemoved() {
        Log.d(TAG, "onTileRemoved");
    }
}
