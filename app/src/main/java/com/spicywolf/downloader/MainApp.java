package com.spicywolf.downloader;

import java.util.List;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.spicywolf.downloader.event.BtHeadsetBatteryLevelChangedEvent;
import com.spicywolf.downloader.event.BtHeadsetConnectionStatusEvent;
import com.spicywolf.downloader.event.RegisterBtHeadsetReceiverEvent;
import com.spicywolf.downloader.event.UnregisterBtHeadsetReceiverEvent;
import com.squareup.otto.Subscribe;

public class MainApp extends Application {

    private final static String TAG = "MainApp";

    private int mCountOfSubscriber = 0;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: " + action);

            if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT.equals(action)) {
                Object values[] =
                        (Object[]) intent.getExtras().get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
                if ((values.length == 2) && (values[0]).equals("FOO")) {
                    BusWrapper.getBus().post(new BtHeadsetBatteryLevelChangedEvent(context.getApplicationContext(),
                                                                                   ((Integer) values[1]) * 10));
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BusWrapper.getBus().post(new BtHeadsetConnectionStatusEvent(context.getApplicationContext(), true));
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BusWrapper.getBus().post(new BtHeadsetConnectionStatusEvent(context.getApplicationContext(), false));
            }
        }
    };

    public MainApp() {
        super();
        Log.i(TAG, "MainApp instance: " + this);
        BusWrapper.getBus().register(this);
    }

    @Subscribe
    public void registerBtHeadsetReceiver(RegisterBtHeadsetReceiverEvent event) {

        Log.i(TAG, "registerBtHeadsetReceiver: " + mCountOfSubscriber);
        if (mCountOfSubscriber == 0) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." +
                                             BluetoothAssignedNumbers.PLANTRONICS);
            intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            getApplicationContext().registerReceiver(mReceiver, intentFilter);
        }
        searchConnectedBtHeadset();
        mCountOfSubscriber++;
    }

    @Subscribe
    public void unregisterBtHeadsetReceiver(UnregisterBtHeadsetReceiverEvent event) {

        Log.i(TAG, "unregisterBtHeadsetReceiver: " + mCountOfSubscriber);

        if (mCountOfSubscriber > 0) {
            getApplicationContext().unregisterReceiver(mReceiver);
            mCountOfSubscriber--;
        }
    }


    private void searchConnectedBtHeadset() {

        BluetoothAdapter.getDefaultAdapter()
                .getProfileProxy(getApplicationContext(), new BluetoothProfile.ServiceListener() {
                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        Log.i(TAG, "onServiceConnected");

                        if (profile == BluetoothProfile.HEADSET) {
                            BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) proxy;
                            List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();

                            Log.i(TAG, "onServiceConnected: " + devices.size());
                            if (devices.size() > 0) {
                                Log.i(TAG, "onServiceConnected: headset is connected");
                                BusWrapper.getBus()
                                        .post(new BtHeadsetConnectionStatusEvent(getApplicationContext(), true));
                            }

                            BluetoothAdapter.getDefaultAdapter()
                                    .closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
                        }
                    }

                    @Override
                    public void onServiceDisconnected(int profile) {
                        Log.i(TAG, "onServiceDisconnected");

                    }
                }, BluetoothProfile.HEADSET);

    }

}
