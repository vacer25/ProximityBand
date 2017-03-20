package com.queensu.apsc_100_mod_3_group_h.proximityband;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.queensu.apsc_100_mod_3_group_h.ble.BluetoothLeService;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import android.os.Vibrator;
//Can you see this push
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    // -------------------- UI --------------------

    SeekBar thresholdSeekBar;
    SeekBar filteringSeekBar;
    TextView thresholdValueTextView;
    TextView filteringValueTextView;
    TextView rssiValueTextView;
    TextView connectionStatusTextView;
    TextView connectionTimeTextView;

    Button connectionButton;
    Button setButtonActionButton;

    Spinner bluetoothDeviceSelectionSpinner;
    Spinner buttonActionSelectionSpinner;

    GraphView rssiValueGraph;

    // -------------------- BLE OBJECTS --------------------

    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isScanning;
    private Handler pingHandler;
    private Handler connectionTimeHandler;
    private Handler rssiHandler;
    private Handler rssiFilteringHandler;

    //final BluetoothGattCharacteristic bluetoothRXCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(BluetoothLeService.UUID_RX), 0, 16);

    // Get instance of Vibrator from current Context
    Vibrator vibrator;

    // -------------------- BLE DEVICES VARIABLES --------------------

    boolean isConnected = false;

    int currentConnectionTime = 0;

    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    String currentSelectedBluetoothAddress = "";
    String currentSelectedBluetoothName = "";

    // -------------------- RSSI ALGORITHM --------------------

    int currentRSSI = 0;
    float filteredRSSI = 0;
    double rssiFilteringSmoothness = 0.001f;

    int rssiThreshold = -100;
    int numberOfRSSIReadings = 0;

    boolean canActivateAlarmNow = false;

    LineGraphSeries<DataPoint> currentRSSIGraphSeries;
    LineGraphSeries<DataPoint> filteredRSSIGraphSeries;
    LineGraphSeries<DataPoint> thresholdRSSIGraphSeries;

    KalmanFilter rssiFilter;

    // -------------------- CONSTANTS --------------------

    private static final long SCAN_PERIOD = 30000; // ms
    private static final long PING_INTERVAL = 250; // ms
    public static int RSSI_GRAPH_UPDATE_INTERVAL = 250;
    public static int RSSI_SCAN_INTERVAL = 1000;
    public static int RSSI_UPDATE_FILTERING_INTERVAL = 50;
    private static final int REQUEST_ENABLE_BT = 1;

    private static final int RSSI_GRAPH_NUMBER_OF_DATA_POINTS = 250;

    private static final double MAX_FILTERING = 0.01;
    private static final double MIN_FILTERING = 0.0001;

    private static final int MAX_RSSI = -30;
    private static final int MIN_RSSI = -110;

    // Vibrate for 400 milliseconds
    long[] ALARM_VIBRATE_PATTERN = {0,200,50,200};

    // -------------------- PREFERENCES --------------------

    SharedPreferences prefs;

    String PREF_BLE_ADDRESS = "Bluetooth_Address";
    String PREF_BLE_NAME = "Bluetooth_Name";

    // -------------------- ACTIVITY LIFECYCLE --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pingHandler = new Handler();
        connectionTimeHandler = new Handler();
        rssiHandler = new Handler();
        rssiFilteringHandler = new Handler();

        rssiFilter = new KalmanFilter();

        getPreferences();
        checkBLECompatibility();
        getActivityViewReferences();

        updateRSSIFilter.run();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        HelperFunctions.hideActionBarAndStatusBar(this);
        enableBluetooth();

        if(!isConnected) {
            bluetoothDevices.clear();
            scanLeDevice(true);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        //registerReceiver(mGattUpdateReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

    }

    @Override
    protected void onPause() {
        super.onPause();

        bluetoothDevices.clear();
        //scanLeDevice(false);
        //unregisterReceiver(mGattUpdateReceiver);
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

    // -------------------- PREFERENCES --------------------

    void getPreferences() {

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentSelectedBluetoothAddress = prefs.getString(PREF_BLE_ADDRESS, "");
        currentSelectedBluetoothName = prefs.getString(PREF_BLE_NAME, "");

    }

    // -------------------- UI AND GRAPHING --------------------

    void getActivityViewReferences() {

        thresholdSeekBar = (SeekBar)findViewById(R.id.thresholdSeekBar);
        filteringSeekBar = (SeekBar)findViewById(R.id.filteringSeekBar);

        thresholdValueTextView = (TextView)findViewById(R.id.thresholdValueTextView);
        filteringValueTextView = (TextView)findViewById(R.id.filteringValueTextView);

        rssiValueTextView = (TextView)findViewById(R.id.rssiValueTextView);
        connectionStatusTextView = (TextView)findViewById(R.id.connectionStatusTextView);

        connectionTimeTextView = (TextView)findViewById(R.id.connectionTimeTextView);
        setConnectionTime(0);

        bluetoothDeviceSelectionSpinner = (Spinner)findViewById(R.id.bluetoothDeviceSelectionSpinner);
        buttonActionSelectionSpinner = (Spinner)findViewById(R.id.buttonActionSelectionSpinner);

        connectionButton = (Button)findViewById(R.id.connectionButton);
        setButtonActionButton = (Button)findViewById(R.id.setButtonActionButton);

        rssiValueGraph = (GraphView)findViewById(R.id.rssiValueGraph);
        resetGraph();

        rssiValueGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time");
        //rssiValueGraph.getGridLabelRenderer().setVerticalAxisTitle("Signal Strength");
        rssiValueGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        rssiValueGraph.getViewport().setXAxisBoundsManual(true);
        rssiValueGraph.getViewport().setMinX(0);
        rssiValueGraph.getViewport().setMaxX(RSSI_GRAPH_NUMBER_OF_DATA_POINTS);
        rssiValueGraph.getViewport().setYAxisBoundsManual(true);
        rssiValueGraph.getViewport().setMinY(MIN_RSSI);
        rssiValueGraph.getViewport().setMaxY(MAX_RSSI);

        // Bluetooth device selection
        bluetoothDeviceSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(bluetoothDevices.size() > position) {
                    BluetoothDevice currentBluetoothDevice = bluetoothDevices.get(position);
                    if (currentBluetoothDevice != null) {
                        if(!currentBluetoothDevice.getAddress().equals(currentSelectedBluetoothAddress)) {
                            resetGraph();
                            setConnectionTime(0);
                        }
                        currentSelectedBluetoothAddress = currentBluetoothDevice.getAddress();
                        currentSelectedBluetoothName = currentBluetoothDevice.getName();
                        prefs.edit().putString(PREF_BLE_ADDRESS, currentSelectedBluetoothAddress).apply();
                        prefs.edit().putString(PREF_BLE_NAME, currentSelectedBluetoothName).apply();
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Threshold seekbar
        thresholdSeekBar.setOnSeekBarChangeListener(this);
        thresholdSeekBar.setProgress((int)(HelperFunctions.map(rssiThreshold, MAX_RSSI, MIN_RSSI, 0, 100)));
        setThresholdValue(rssiThreshold);

        // Filtering seekbar
        filteringSeekBar.setOnSeekBarChangeListener(this);
        filteringSeekBar.setProgress((int)(HelperFunctions.map((float)rssiFilteringSmoothness, (float)MIN_FILTERING, (float)MAX_FILTERING, 0f, 100f)));
        rssiFilter.R = rssiFilteringSmoothness;
        setFilteringValueTextView(rssiFilter.R);

        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    void setGraphSeries() {
        if(rssiValueGraph != null) {

            currentRSSIGraphSeries = new LineGraphSeries<>();
            currentRSSIGraphSeries.setColor(Color.BLUE);
            currentRSSIGraphSeries.setThickness(3);
            rssiValueGraph.addSeries(currentRSSIGraphSeries);

            filteredRSSIGraphSeries = new LineGraphSeries<>();
            filteredRSSIGraphSeries.setColor(Color.GREEN);
            filteredRSSIGraphSeries.setThickness(7);
            rssiValueGraph.addSeries(filteredRSSIGraphSeries);

            thresholdRSSIGraphSeries = new LineGraphSeries<>();
            thresholdRSSIGraphSeries.setColor(Color.RED);
            thresholdRSSIGraphSeries.setThickness(3);
            rssiValueGraph.addSeries(thresholdRSSIGraphSeries);

        }
    }

    void resetGraph() {
        rssiValueGraph.removeAllSeries();
        numberOfRSSIReadings = 0;
        setGraphSeries();
    }

    // -------------------- USER INTERACTION --------------------

    public void onSetButtonActionClicked(View v) {
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
            sendBluetoothData("rgbijk");
        }
        else if(selection == 17) {
            sendBluetoothData("X");
        }
        else if(selection == 18) {
            sendBluetoothData("x");
        }

    }

    public void onSetBluetoothDeviceClicked(View v) {
        //HelperFunctions.vibrate(this, 50);
        if(mBluetoothLeService != null) {
            if(!isConnected) {
                if(currentSelectedBluetoothAddress.contains(":")){
                    //Log.vibrator("ACTION", "Attempting to connect...");
                    connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.connecting) + " " + currentSelectedBluetoothName + "...");
                    mBluetoothLeService.connect(currentSelectedBluetoothAddress);
                    //Log.d("BLE CONNECTION", "Connect request result: " + result);
                }
            }
            else {
                //Log.vibrator("ACTION", "Attempting to disconnect...");
                connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.disconnecting) + " " + currentSelectedBluetoothName + "...") ;
                mBluetoothLeService.disconnect();
            }
        }

    }

    // -------------------- THRESHOLD SEEK BAR --------------------

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar == thresholdSeekBar) {
            rssiThreshold = (int)(HelperFunctions.map(progress, 0, 100, MAX_RSSI, MIN_RSSI));
            setThresholdValue(rssiThreshold);
        }
        else if(seekBar == filteringSeekBar) {
            rssiFilter.R = HelperFunctions.map(filteringSeekBar.getProgress(), 0f, 100f, (float)MIN_FILTERING, (float)MAX_FILTERING);
            rssiFilteringSmoothness = rssiFilter.R;
            setFilteringValueTextView(rssiFilter.R);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
            if(seekBar == thresholdSeekBar) {
                canActivateAlarmNow = false;
            }
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(seekBar == thresholdSeekBar) {
            canActivateAlarmNow = true;
        }
    }

    void setThresholdValue(int valueToSet){
        DataPoint thresholdDataPoints[] = {
                new DataPoint(0, (float)(valueToSet)),
                new DataPoint(numberOfRSSIReadings, (float)(valueToSet))
        };
        thresholdRSSIGraphSeries.resetData(thresholdDataPoints);
        setThresholdValueText(valueToSet);
    }

    void setThresholdValueText(int valueToSet) {
        if(thresholdValueTextView != null) {
            thresholdValueTextView.setText(String.valueOf(valueToSet));
        }
    }

    void setFilteringValueTextView(double valueToSet) {
        if(thresholdValueTextView != null) {
            filteringValueTextView.setText(String.format(Locale.ENGLISH, "%.5f", valueToSet));
        }
    }

    // -------------------- RSSI VALUE --------------------

    void setRSSIValue(String rssiToSet, boolean updateGraph) {

        try {
            String[] rssiToSetSplit = rssiToSet.split("\n", 2);
            int rssi = Integer.parseInt(rssiToSetSplit[0]);
            setRSSIValue(rssi, updateGraph);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

    }

    void setRSSIValue(int rssiValue, boolean updateGraph) {
        if(rssiValueTextView != null) {
            String currentRSSIText = String.format(Locale.ENGLISH, "%.1f", filteredRSSI) + getResources().getString(R.string.db);
            rssiValueTextView.setText(currentRSSIText);
        }
        if(currentRSSIGraphSeries != null && filteredRSSIGraphSeries != null && updateGraph) {
            currentRSSIGraphSeries.appendData(new DataPoint(numberOfRSSIReadings, rssiValue), true, RSSI_GRAPH_NUMBER_OF_DATA_POINTS);
            filteredRSSIGraphSeries.appendData(new DataPoint(numberOfRSSIReadings, filteredRSSI), true, RSSI_GRAPH_NUMBER_OF_DATA_POINTS);
            setThresholdValue(rssiThreshold);
            numberOfRSSIReadings++;
        }

        if(filteredRSSI < rssiThreshold) {
            if(canActivateAlarmNow) {
                sendBluetoothData("X"); // Alarm on
            }
            vibrator.vibrate(ALARM_VIBRATE_PATTERN, 0);
            rssiValueTextView.setTextColor(Color.RED);
        }
        else {
            sendBluetoothData("x"); // Alarm off
            rssiValueTextView.setTextColor(Color.BLACK);
            vibrator.cancel();
        }
    }

    // -------------------- CONNECTION TIME --------------------

    void setConnectionTime(int timeToSet) {

        currentConnectionTime = timeToSet;

        if(connectionTimeTextView != null) {

            int connectionHours = timeToSet / 3600;
            int connectionMinutes = timeToSet / 60 - connectionHours * 60;
            int connectionSeconds = timeToSet - connectionHours * 3600 - connectionMinutes * 60;

            String connectionMinutesString = "";
            if(connectionMinutes < 10) {
                connectionMinutesString = "0";
            }
            connectionMinutesString += String.valueOf(connectionMinutes);

            String connectionSecondsString = "";
            if(connectionSeconds < 10) {
                connectionSecondsString = "0";
            }
            connectionSecondsString += String.valueOf(connectionSeconds);

            connectionTimeTextView.setText(connectionHours + ":" + connectionMinutesString + ":" + connectionSecondsString);

        }
    }

    // -------------------- TIMED ACTIONS --------------------

    Runnable sendRepeatingPing = new Runnable() {
        @Override
        public void run() {
            try {
                sendBluetoothData("A");
                //mBluetoothLeService.readCharacteristic(bluetoothRXCharacteristic);
                //mBluetoothLeService.readCustomCharacteristic();
            } finally {
                pingHandler.postDelayed(sendRepeatingPing, PING_INTERVAL);
            }
        }
    };

    Runnable updateRSSIValue = new Runnable() {
        @Override
        public void run() {
            try {
                if(currentRSSI != 0) {
                    setRSSIValue(currentRSSI, true);
                }
            } finally {
                rssiHandler.postDelayed(updateRSSIValue, RSSI_GRAPH_UPDATE_INTERVAL);
            }
        }
    };

    Runnable updateRSSIFilter = new Runnable() {
        @Override
        public void run() {
            //Log.vibrator("FILTER", "Trying to update filter value with: " + currentRSSI);
            try {
                //Log.vibrator("FILTER", "Updating filter value with: " + currentRSSI);
                if(filteredRSSI > -1) {
                    filteredRSSI = currentRSSI;
                }
                else {
                    filteredRSSI = rssiFilter.update((float) currentRSSI);
                }
                //Log.vibrator("FILTER", "New filter value is: " + filteredRSSI);
            } finally {
                rssiFilteringHandler.postDelayed(updateRSSIFilter, RSSI_UPDATE_FILTERING_INTERVAL);
            }
        }
    };

    Runnable updateConnectionTime = new Runnable() {
        @Override
        public void run() {
            try {
                setConnectionTime(currentConnectionTime + 1);
            } finally {
                connectionTimeHandler.postDelayed(updateConnectionTime, 1000);
            }
        }
    };

    // -------------------- BLUETOOTH CONNECTION --------------------

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
            if(currentSelectedBluetoothAddress.contains(":")) {
                mBluetoothLeService.connect(currentSelectedBluetoothAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.v("BLE CONNECTION", "OnServiceDisconnected...");
            mBluetoothLeService = null;
        }
    };

    void setUpConnectToBluetooth() {

        isConnected = true;
        sendRepeatingPing.run();
        connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.connected) + " " + currentSelectedBluetoothName);
        connectionButton.setText(R.string.disconnect);

        //mBluetoothLeService.setCharacteristicNotification(bluetoothRXCharacteristic, true);

        ArrayList<String> bluetoothDevicesNames = new ArrayList<>();
        bluetoothDevicesNames.add(currentSelectedBluetoothName);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothDevicesNames);
        bluetoothDeviceSelectionSpinner.setAdapter(adapter);

        bluetoothDeviceSelectionSpinner.setEnabled(false);

        setConnectionTime(-1);
        updateConnectionTime.run();
        scanLeDevice(false);

    }

    void setUpDisconnectFromBluetooth() {

        currentRSSI = 0;

        isConnected = false;
        pingHandler.removeCallbacks(sendRepeatingPing);
        connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.disconnected));
        connectionButton.setText(R.string.connect);

        ArrayList<String> bluetoothDevicesNames = new ArrayList<>();
        bluetoothDevicesNames.add(currentSelectedBluetoothName);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothDevicesNames);
        bluetoothDeviceSelectionSpinner.setAdapter(adapter);

        bluetoothDeviceSelectionSpinner.setEnabled(true);

        connectionTimeHandler.removeCallbacks(updateConnectionTime);
        rssiValueTextView.setText("---");
        scanLeDevice(true);

    }

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
        }

    }

    //private void scanLeDevice(final boolean enable) {
    //    scanLeDevice(enable, true);
    //}

    private void scanLeDevice(final boolean enable /*, final boolean disableRunable */) {
        if (enable /* && !isScanning */) {
            /*
            // Stops scanning after a pre-defined scan period.
            bleHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            */
            isScanning = true;
            connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.scanning));
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            updateRSSIValue.run();
        } else {
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    void addBLEDeviceToList(BluetoothDevice bluetoothDevice) {

        if(!bluetoothDevices.contains(bluetoothDevice) && bluetoothDevice.getName() != null) {
            bluetoothDevices.add(bluetoothDevice);

            ArrayList<String> bluetoothDevicesNames = new ArrayList<>();
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
                                currentRSSI = rssi;
                                setRSSIValue(rssi, false);
                                //Log.vibrator("BLE SCAN", "Found new device! Address: " + device.getAddress() + " Name: " + device.getName());
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

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Log.vibrator("BLE CALLBACK", "onReceive... Action: " + action + " Extra: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                setUpConnectToBluetooth();
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setUpDisconnectFromBluetooth();
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.v("BLE DATA", "Data available: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_READ_REMOTE_RSSI.equals(action)) {
                Log.d("RSSI", String.format("Got RSSI: %d", currentRSSI));
                try {
                    String[] rssiToSetSplit = (intent.getStringExtra(BluetoothLeService.EXTRA_DATA)).split("\n", 2);
                    currentRSSI = Integer.parseInt(rssiToSetSplit[0]);
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                setRSSIValue(currentRSSI, false);
            }
        }
    };

}
