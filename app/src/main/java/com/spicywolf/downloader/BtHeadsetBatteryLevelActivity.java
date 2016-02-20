package com.spicywolf.downloader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;


public class BtHeadsetBatteryLevelActivity extends Activity {

    private final static String TAG = "BatteryLevel";

    private TextView mStatus;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            appendLog("onReceive - " + action);

            if (BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT.equals(action)) {

                Object values[] = (Object[])intent.getExtras().get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
                if ((values.length  == 2) && (values[0]).equals("FOO")) {
                    appendLog("Battery Level %d", ((Integer)values[1]) * 10);
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_bluetooth_headset);
        mStatus = (TextView) findViewById(R.id.textStatus);
        registerReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private void registerReceiver() {
        appendLog("register receiver");


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." +
                                         BluetoothAssignedNumbers.PLANTRONICS);
        intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        appendLog("unregister receiver");
        unregisterReceiver(mReceiver);
    }

    private void appendLog(String msg, Object... args) {
        if (args.length > 0) {
          msg = String.format(msg, args);
        }
        mStatus.append(String.format("\n[%d] %s", System.currentTimeMillis(), msg));

    }
}
