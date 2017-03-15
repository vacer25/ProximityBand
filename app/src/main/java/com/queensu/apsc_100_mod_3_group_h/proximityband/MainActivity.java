package com.queensu.apsc_100_mod_3_group_h.proximityband;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.queensu.apsc_100_mod_3_group_h.ble.BluetoothLeService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    // -------------------- UI --------------------

    SeekBar sensitivitySeekBar;
    TextView sensitivityValueTextView;
    TextView valueTextView;
    TextView connectionStatusTextView;

    Button connectionButton;
    Button setButtonActionButton;

    Spinner bluetoothDeviceSelectionSpinner;
    Spinner buttonActionSelectionSpinner;

    // -------------------- BLE OBJECTS --------------------

    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler bleHandler;
    private Handler pingHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 15000;

    // -------------------- BLE DEVICES VARIABLES --------------------

    boolean isConnected = false;

    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    String currentSelectedBluetoothAddress;
    String currentSelectedBluetoothName;

    // -------------------- RSSI ALGORITHM --------------------

    int rssiThreshold = -100;

    // -------------------- ACTIVITY LIFECYCLE --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bleHandler = new Handler();
        pingHandler = new Handler();

        checkBLECompatibility();
        getActivityViewReferences();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        HelperFunctions.hideActionBarAndStatusBar(this);
        enableBluetooth();

        bluetoothDevices.clear();
        scanLeDevice(true);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        //registerReceiver(mGattUpdateReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

    }

    @Override
    protected void onPause() {
        super.onPause();

        bluetoothDevices.clear();
        //scanLeDevice(false);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        HelperFunctions.hideActionBarAndStatusBar(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // -------------------- GET REFERENCES TO UI --------------------

    void getActivityViewReferences() {

        sensitivitySeekBar = (SeekBar)findViewById(R.id.sensitivitySeekBar);

        sensitivityValueTextView = (TextView)findViewById(R.id.sensitivityValueTextView);
        valueTextView = (TextView)findViewById(R.id.valueTextView);
        connectionStatusTextView = (TextView)findViewById(R.id.connectionStatusTextView);

        bluetoothDeviceSelectionSpinner = (Spinner)findViewById(R.id.bluetoothDeviceSelectionSpinner);
        buttonActionSelectionSpinner = (Spinner)findViewById(R.id.buttonActionSelectionSpinner);

        connectionButton = (Button)findViewById(R.id.connectionButton);
        setButtonActionButton = (Button)findViewById(R.id.setButtonActionButton);

        // Bluetooth device selection
        bluetoothDeviceSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(bluetoothDevices.size() > position) {
                    BluetoothDevice currentBluetoothDevice = bluetoothDevices.get(position);
                    if (currentBluetoothDevice != null) {
                        currentSelectedBluetoothAddress = currentBluetoothDevice.getAddress();
                        currentSelectedBluetoothName = currentBluetoothDevice.getName();
                        //Log.v("BLE DEVICE SELECTION", "Selected device id: " + id);
                        //Log.v("BLE DEVICE SELECTION", "Selected device index: " + position);
                        //Log.v("BLE DEVICE SELECTION", "Selected device address: " + currentSelectedBluetoothAddress);
                        //Log.v("BLE DEVICE SELECTION", "Selected device name: " + currentBluetoothDevice.getName());
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Seekbar test
        sensitivitySeekBar.setOnSeekBarChangeListener(this);
        setSensitivityValueText(0);

    }

    // -------------------- USER INTERACTION --------------------

    void onSetButtonActionClicked(View v) {
        //HelperFunctions.vibrate(this, 50);

        long selection = buttonActionSelectionSpinner.getSelectedItemId();

        if(selection == 0) {
            sendBluetoothData("1");
        }
        else if(selection == 1) {
            sendBluetoothData("2");
        }
        else if(selection == 2) {
            sendBluetoothData("3");
        }
        else if(selection == 3) {
            sendBluetoothData("R");
        }
        else if(selection == 4) {
            sendBluetoothData("G");
        }
        else if(selection == 5) {
            sendBluetoothData("B");
        }
        else if(selection == 6) {
            sendBluetoothData("r");
        }
        else if(selection == 7) {
            sendBluetoothData("g");
        }
        else if(selection == 8) {
            sendBluetoothData("b");
        }
        else if(selection == 9) {
            sendBluetoothData("I");
        }
        else if(selection == 10) {
            sendBluetoothData("J");
        }
        else if(selection == 11) {
            sendBluetoothData("K");
        }
        else if(selection == 12) {
            sendBluetoothData("i");
        }
        else if(selection == 13) {
            sendBluetoothData("j");
        }
        else if(selection == 14) {
            sendBluetoothData("k");
        }
        else if(selection == 15) {
            sendBluetoothData("RGB");
        }
        else if(selection == 16) {
            sendBluetoothData("rgb");
        }
        else if(selection == 17) {
            sendBluetoothData("X");
        }

    }

    void onSetBluetoothDeviceClicked(View v) {
        //HelperFunctions.vibrate(this, 50);
        if(mBluetoothLeService != null) {
            if(!isConnected) {
                Log.v("ACTION", "Attempting to connect...");
                final boolean result = mBluetoothLeService.connect(currentSelectedBluetoothAddress);
                Log.d("BLE CONNECTION", "Connect request result: " + result);
            }
            else {
                Log.v("ACTION", "Attempting to disconnect...");
                mBluetoothLeService.disconnect();
            }
        }

    }

    // -------------------- SENSITIVITY SEEK BAR --------------------

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setSensitivityValueText(progress);
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //HelperFunctions.vibrate(this, 25);
        rssiThreshold = -1 * sensitivitySeekBar.getProgress();
    }

    void setSensitivityValueText(int valueToSet) {
        if(sensitivityValueTextView != null) {
            sensitivityValueTextView.setText(String.valueOf(valueToSet));
        }
    }

    // -------------------- RSSI VALUE --------------------

    void setRSSIValue(String rssiToSet) {

        String[] rssiToSetSplit = rssiToSet.split("\n", 2);

        if(valueTextView != null) {
            valueTextView.setText(rssiToSetSplit[0]);
        }

        try {
            int rssi = Integer.parseInt(rssiToSetSplit[0]);
            if(rssi < rssiThreshold) {
                sendBluetoothData("X");
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

    }

    void setRSSIValue(int rssiToSet) {
        setRSSIValue(String.valueOf(rssiToSet));
    }

    // -------------------- BLUETOOTH CONNECTION --------------------

    Runnable sendRepeatingPing = new Runnable() {
        @Override
        public void run() {
            try {
                sendBluetoothData("A");
            } finally {
                pingHandler.postDelayed(sendRepeatingPing, 500);
            }
        }
    };

    void sendBluetoothData(String dataToSend) {
        if(mBluetoothLeService != null) {
            for (int currentCharIndex = 0; currentCharIndex < dataToSend.length(); currentCharIndex++) {
                mBluetoothLeService.writeCustomCharacteristic((int)(dataToSend.charAt(currentCharIndex)));
                if(dataToSend.length() > 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    // Code to manage Service lifecycle
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.v("BLE CONNECTION", "OnServiceConnected...");

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("BLE CONNECTION", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if(currentSelectedBluetoothAddress != null) {
                mBluetoothLeService.connect(currentSelectedBluetoothAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.v("BLE CONNECTION", "OnServiceDisconnected...");
            mBluetoothLeService = null;
        }
    };

    // -------------------- BLUETOOTH SCANNING --------------------

    void enableBluetooth() {

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

    }

    void checkBLECompatibility() {

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            HelperFunctions.displayToastMessage(this, "BLE not supported!");
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            HelperFunctions.displayToastMessage(this, "Bluetooth not supported!");
            finish();
            return;
        }

    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            bleHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    void addBLEDeviceToList(BluetoothDevice bluetoothDevice) {

        if(!bluetoothDevices.contains(bluetoothDevice) && bluetoothDevice.getName() != null) {
            bluetoothDevices.add(bluetoothDevice);

            ArrayList<String> bluetoothDevicesNames = new ArrayList<String>();
            for(int currentBluetoothDeviceIndex = 0; currentBluetoothDeviceIndex < bluetoothDevices.size(); currentBluetoothDeviceIndex++) {
                String currentBluetoothDeviceName = bluetoothDevices.get(currentBluetoothDeviceIndex).getName();
                if(currentBluetoothDeviceName != null) {
                    bluetoothDevicesNames.add(currentBluetoothDeviceName);
                }
            }

            if(bluetoothDevicesNames.size() > 0) {
                ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothDevicesNames);
                bluetoothDeviceSelectionSpinner.setAdapter(adapter);
            }

        }

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(device.getAddress().equals(currentSelectedBluetoothAddress)) {
                                setRSSIValue(rssi);
                                Log.v("BLE SCAN", "Found new device! Address: " + device.getAddress() + " Name: " + device.getName());
                            }
                            addBLEDeviceToList(device);
                        }
                    });
                }
            };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_READ_REMOTE_RSSI);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.v("BLE CALLBACK", "onReceive... Action: " + action + " Extra: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                isConnected = true;
                sendRepeatingPing.run();
                connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.connected) + " " + currentSelectedBluetoothName);
                connectionButton.setText(R.string.disconnect);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnected = false;
                pingHandler.removeCallbacks(sendRepeatingPing);
                connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.disconnected));
                connectionButton.setText(R.string.connect);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.v("BLE DATA", "Data available: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_READ_REMOTE_RSSI.equals(action)) {
                setRSSIValue(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

}
