package com.queensu.apsc_100_mod_3_group_h.proximityband;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.os.Vibrator;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, MultiSelectionSpinner.OnMultipleItemsSelectedListener {

    // -------------------- UI --------------------

    SeekBar thresholdSeekBar;
    SeekBar filteringSeekBar;
    SeekBar delaySeekBar;

    TextView thresholdValueTextView;
    TextView filteringValueTextView;
    TextView delayValueTextView;

    TextView rssiValueTextView;
    TextView connectionStatusTextView;
    TextView connectionTimeTextView;

    Button connectionButton;
    //Button setButtonActionButton;

    Spinner bluetoothDeviceSelectionSpinner;
    //Spinner buttonActionSelectionSpinner;
    Spinner notificationReminderSelectionSpinner;

    GraphView rssiValueGraph;

    MultiSelectionSpinner redNotificationGroupMultiSelectionSpinner;
    MultiSelectionSpinner greenNotificationGroupMultiSelectionSpinner;
    MultiSelectionSpinner blueNotificationGroupMultiSelectionSpinner;

    WebView aboutWebView;

    // -------------------- BLE OBJECTS --------------------

    private BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    //private boolean isScanning;
    private Handler pingHandler;
    private Handler connectionTimeHandler;
    private Handler rssiHandler;
    private Handler rssiFilteringHandler;
    private Handler vibrationRepeatHandler;
    private Handler alarmDelayHandler;
    private Handler notificationReminderHandler;

    // -------------------- VIBRATION --------------------

    Vibrator vibrator;

    // -------------------- NOTIFICATIONS --------------------

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private boolean isEnabledNLS = false;

    private String packagesListHash = "";

    public ArrayList<String> packageNames = new ArrayList<>();
    public ArrayList<String> packageAppNames = new ArrayList<>();

    private String packageNamesString = "";
    private String packageAppNamesString = "";
    private int previousNumberOfNotifications = 0;

    private int notificationReminderTime = 0; // In ms
    private boolean notificationReminderIsRunning = false;

    public static ArrayList<String> redNotificationGroup = new ArrayList<String>() {{
        add("com.android.mms"); // Messages
        add("com.android.email"); // Email
    }};
    public static ArrayList<String> greenNotificationGroup = new ArrayList<String>() {{
        add("com.queensu.apsc_100_mod_3_group_h.proximityband"); // Proximity Band
        add("com.android.calendar"); // Calendar
        add("com.android.phone"); // Phone
    }};
    public static ArrayList<String> blueNotificationGroup = new ArrayList<String>() {{
        add("com.facebook.orca"); // Messenger
        add("com.skype.raider"); // Skype
    }};

    NotificationManager notificationManager;

    // -------------------- BLE DEVICES VARIABLES --------------------

    boolean isConnected = false;
    boolean isOutOfRange = false;
    boolean isWaitingToAutoConnect = false;
    boolean isWaitingToDisconnect = false;
    boolean isWaitingToSendInitialStatusUpdate = false;

    int currentConnectionTime = 0;

    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    String currentSelectedBluetoothAddress = "";
    String currentSelectedBluetoothName = "";

    boolean buttonIsPressed = false;

    // -------------------- RSSI ALGORITHM --------------------

    int currentRSSI = 0;
    float filteredRSSI = 0;
    float rssiFilteringSmoothness = 0.001f;

    int rssiThreshold = -100;
    int numberOfRSSIReadings = 0;

    float alarmDelay = 1f;

    boolean canActivateAlarmNow = false;

    LineGraphSeries<DataPoint> currentRSSIGraphSeries;
    LineGraphSeries<DataPoint> filteredRSSIGraphSeries;
    LineGraphSeries<DataPoint> thresholdRSSIGraphSeries;

    KalmanFilter rssiFilter;

    AlertDialog alarmDialog;

    // -------------------- CONSTANTS --------------------

    static final String PROXIMITY_BAND_BLE_NAME = "Proximity Band";

    private static final long PING_INTERVAL = 250; // ms
    public static int RSSI_GRAPH_UPDATE_INTERVAL = 250; // ms
    public static int RSSI_SCAN_INTERVAL = 1000; // ms
    public static int RSSI_UPDATE_FILTERING_INTERVAL = 50; // ms
    private static final int REQUEST_ENABLE_BT = 1;

    private static final long VIBRATION_REPEAT_TIME = 500; // ms

    private static final int RSSI_GRAPH_NUMBER_OF_DATA_POINTS = 250;

    private static final double MAX_FILTERING = 0.005;
    private static final double MIN_FILTERING = 0.0001;

    private static final double MAX_ALARM_DELAY = 5.0;
    private static final double MIN_ALARM_DELAY = 0.0;

    private static final int MAX_RSSI = -30; // db
    private static final int MIN_RSSI = -110; // db

    long[] ALARM_VIBRATE_PATTERN = {0,200,50,200}; // ms

    // -------------------- DATA CONSTANTS --------------------

    public final static String COMMAND_BUTTON_PRESSED = "B1";
    public final static String COMMAND_BUTTON_RELEASED = "B0";

    public final static String COMMAND_SWITCH_POSITION_1 = "S1";
    public final static String COMMAND_SWITCH_POSITION_2 = "S2";
    public final static String COMMAND_SWITCH_POSITION_3 = "S3";

    public final static String COMMAND_ACK = "A";

    // -------------------- PREFERENCES --------------------

    public static SharedPreferences prefs;

    String PREF_BLE_ADDRESS = "Bluetooth_Address";
    String PREF_BLE_NAME = "Bluetooth_Name";

    String PREF_FILTERING = "Filtering_Smoothness";
    String PREF_THRESHOLD = "RSSI_Threshold";
    String PREF_DELAY = "Alarm_Delay";

    public static String PREF_RED_NOTIFICATIONS = "Red_Notifications";
    public static String PREF_GREEN_NOTIFICATIONS = "Green_Notifications";
    public static String PREF_BLUE_NOTIFICATIONS = "Blue_Notifications";

    public static String PREF_PACKAGES_LIST_HASH = "Packages_List_Hash";
    public static String PREF_PACKAGES_NAMES = "Packages_Names";
    public static String PREF_PACKAGES_APP_NAMES = "Packages_App_Names";

    public static String PREF_NOTIFICATION_REMINDER_TIME = "Notification_Reminder_Time";

    // -------------------- ACTIVITY LIFECYCLE --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pingHandler = new Handler();
        connectionTimeHandler = new Handler();
        rssiHandler = new Handler();
        rssiFilteringHandler = new Handler();
        vibrationRepeatHandler = new Handler();
        alarmDelayHandler = new Handler();
        notificationReminderHandler = new Handler();

        rssiFilter = new KalmanFilter();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        getPreferences();
        checkBLECompatibility();
        getActivityViewReferences();
        setupNotificationListener();

        updateRSSIFilter.run();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mBLEServiceConnection, BIND_AUTO_CREATE);

        Intent notificationMonitorServiceIntent = new Intent(this, NotificationMonitorService.class);
        bindService(notificationMonitorServiceIntent, mNLServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Fullscreen
        HelperFunctions.hideActionBarAndStatusBar(this);

        // BLE Connection
        enableBluetooth();

        if(!isConnected) {
            bluetoothDevices.clear();
            scanLeDevice(true);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Notification Listener Access
        isEnabledNLS = NLSIsEnabled();
        Log.v("NLS", "isEnabledNLS = " + isEnabledNLS);
        if (!isEnabledNLS) {
            showConfirmDialog();
        }

        HelperFunctions.appHasBeenSentToBackground = true;

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

        unbindService(mBLEServiceConnection);
        stopService(new Intent(this, BluetoothLeService.class));
        unregisterReceiver(mGattUpdateReceiver);

        unbindService(mNLServiceConnection);
        stopService(new Intent(this, NotificationMonitorService.class));
        unregisterReceiver(notificationCallbackReceiver);

        if(vibrationRepeatHandler != null) {
            vibrationRepeatHandler.removeCallbacks(repeatVibration);
        }
        vibrator.cancel();
        mBluetoothLeService = null;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        else {
            if(isWaitingToAutoConnect) {
                attemptConnectionToLastConnectedBluetoothDevice();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // -------------------- PREFERENCES --------------------

    void getPreferences() {

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentSelectedBluetoothAddress = prefs.getString(PREF_BLE_ADDRESS, "");
        currentSelectedBluetoothName = prefs.getString(PREF_BLE_NAME, "");

        packagesListHash = prefs.getString(PREF_PACKAGES_LIST_HASH, packagesListHash);
        packageNamesString = prefs.getString(PREF_PACKAGES_NAMES, packageNamesString);
        packageAppNamesString = prefs.getString(PREF_PACKAGES_APP_NAMES, packageAppNamesString);

        rssiFilteringSmoothness = prefs.getFloat(PREF_FILTERING, rssiFilteringSmoothness);
        rssiThreshold = prefs.getInt(PREF_THRESHOLD, rssiThreshold);
        alarmDelay = prefs.getFloat(PREF_DELAY, alarmDelay);

        notificationReminderTime = prefs.getInt(PREF_NOTIFICATION_REMINDER_TIME, notificationReminderTime);

        rssiFilter.R = rssiFilteringSmoothness;

        //setThresholdValue(rssiThreshold);
        //setFilteringValueTextView(rssiFilteringSmoothness);

        PreferenceSettings.getNotificationSettings();

    }

    // -------------------- UI AND GRAPHING --------------------

    void getActivityViewReferences() {

        thresholdSeekBar = (SeekBar)findViewById(R.id.thresholdSeekBar);
        filteringSeekBar = (SeekBar)findViewById(R.id.filteringSeekBar);
        delaySeekBar = (SeekBar)findViewById(R.id.delaySeekBar);

        thresholdValueTextView = (TextView)findViewById(R.id.thresholdValueTextView);
        filteringValueTextView = (TextView)findViewById(R.id.filteringValueTextView);
        delayValueTextView = (TextView)findViewById(R.id.delayValueTextView);

        rssiValueTextView = (TextView)findViewById(R.id.rssiValueTextView);
        connectionStatusTextView = (TextView)findViewById(R.id.connectionStatusTextView);

        connectionTimeTextView = (TextView)findViewById(R.id.connectionTimeTextView);
        setConnectionTime(0);

        bluetoothDeviceSelectionSpinner = (Spinner)findViewById(R.id.bluetoothDeviceSelectionSpinner);
        //buttonActionSelectionSpinner = (Spinner)findViewById(R.id.buttonActionSelectionSpinner);
        notificationReminderSelectionSpinner = (Spinner)findViewById(R.id.notificationReminderSelectionSpinner);
        notificationReminderSelectionSpinner.setSelection(Arrays.binarySearch(getResources().getIntArray(R.array.reminder_times_values), notificationReminderTime));

        connectionButton = (Button)findViewById(R.id.connectionButton);
        //setButtonActionButton = (Button)findViewById(R.id.setButtonActionButton);

        aboutWebView = (WebView)findViewById(R.id.aboutWebView);
        aboutWebView.setVisibility(View.GONE);

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
                        }
                        currentSelectedBluetoothAddress = currentBluetoothDevice.getAddress();
                        currentSelectedBluetoothName = currentBluetoothDevice.getName();
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Notification reminder
        notificationReminderSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                notificationReminderTime = getResources().getIntArray(R.array.reminder_times_values)[position];
                Log.v("NOTIFICATIONS", "Reminder time set to: " + notificationReminderTime);
                //HelperFunctions.displayToastMessage(MainActivity.this, "Reminder time set to: " + notificationReminderTime);
                notificationReminderHandler.removeCallbacks(remindOfNotifications);
                if(notificationReminderTime == 0) {
                    notificationReminderIsRunning = false;
                    Log.v("NOTIFICATIONS", "Reminders turned off");
                }
                else {
                    notificationReminderHandler.post(remindOfNotifications);
                    notificationReminderIsRunning = true;
                    Log.v("NOTIFICATIONS", "Reminders turned on");
                }
                prefs.edit().putInt(PREF_NOTIFICATION_REMINDER_TIME, notificationReminderTime).apply();
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
        filteringSeekBar.setProgress((int)(HelperFunctions.map(rssiFilteringSmoothness, (float)MIN_FILTERING, (float)MAX_FILTERING, 0f, 100f)));
        rssiFilter.R = rssiFilteringSmoothness;
        setFilteringValueTextView(filteringSeekBar.getProgress());

        // Delay seekbar
        delaySeekBar.setOnSeekBarChangeListener(this);
        delaySeekBar.setProgress((int)(HelperFunctions.map(alarmDelay, (float)MIN_ALARM_DELAY, (float)MAX_ALARM_DELAY, 0f, 100f)));
        setDelayValueTextView(alarmDelay);

        redNotificationGroupMultiSelectionSpinner = (MultiSelectionSpinner)findViewById(R.id.redNotificationGroupMultiSelectionSpinner);
        greenNotificationGroupMultiSelectionSpinner = (MultiSelectionSpinner)findViewById(R.id.greenNotificationGroupMultiSelectionSpinner);
        blueNotificationGroupMultiSelectionSpinner = (MultiSelectionSpinner)findViewById(R.id.blueNotificationGroupMultiSelectionSpinner);

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

    public void onSetBluetoothDeviceClicked(View v) {

        if(mBluetoothLeService != null) {
            if(!isConnected) {
                if(currentSelectedBluetoothAddress.contains(":")){
                    //Log.vibrator("ACTION", "Attempting to connect...");

                    prefs.edit().putString(PREF_BLE_ADDRESS, currentSelectedBluetoothAddress).apply();
                    prefs.edit().putString(PREF_BLE_NAME, currentSelectedBluetoothName).apply();

                    connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.connecting) + " " + currentSelectedBluetoothName + "...");
                    mBluetoothLeService.connect(currentSelectedBluetoothAddress);
                    //Log.d("BLE CONNECTION", "Connect request result: " + result);
                }
            }
            else {
                //Log.vibrator("ACTION", "Attempting to disconnect...");
                boolean didSendSuppressAlarmCommand = false;
                do {
                    didSendSuppressAlarmCommand = sendBluetoothData("Y");
                } while(!didSendSuppressAlarmCommand);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                isWaitingToDisconnect = true;
                connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.disconnecting) + " " + currentSelectedBluetoothName + "...");
                mBluetoothLeService.disconnect();

            }
        }

    }

    @Override
    public void selectedIndices(MultiSelectionSpinner multiSelectionSpinner, List<Integer> indices) {
        if(multiSelectionSpinner == redNotificationGroupMultiSelectionSpinner) {
            redNotificationGroup.clear();
            for(Integer currentIndex : indices) {
                redNotificationGroup.add(packageNames.get(currentIndex));
            }
        }
        else if(multiSelectionSpinner == greenNotificationGroupMultiSelectionSpinner) {
            greenNotificationGroup.clear();
            for(Integer currentIndex : indices) {
                greenNotificationGroup.add(packageNames.get(currentIndex));
            }
        }
        else if(multiSelectionSpinner == blueNotificationGroupMultiSelectionSpinner) {
            blueNotificationGroup.clear();
            for(Integer currentIndex : indices) {
                blueNotificationGroup.add(packageNames.get(currentIndex));
            }
        }

        PreferenceSettings.setNotificationSettings();
        updateNotificationsList(true);
    }

    @Override
    public void selectedStrings(MultiSelectionSpinner multiSelectionSpinner, List<String> strings) {
    }

    public void onAboutButtonClicked (View v) {
        aboutWebView.setVisibility(View.VISIBLE);
        aboutWebView.loadUrl("file:///android_asset/about.html");
        aboutWebView.setScrollY(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if( keyCode == KeyEvent.KEYCODE_BACK ) {
            aboutWebView.setVisibility(View.GONE);
        }
        return true;
    }

    // -------------------- THRESHOLD SEEK BAR --------------------

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser) {
            if (seekBar == thresholdSeekBar) {
                canActivateAlarmNow = false;
                rssiThreshold = (int) (HelperFunctions.map(progress, 0, 100, MAX_RSSI, MIN_RSSI));
                setThresholdValue(rssiThreshold);
            } else if (seekBar == filteringSeekBar) {
                rssiFilteringSmoothness = HelperFunctions.map(filteringSeekBar.getProgress(), 0f, 100f, (float) MIN_FILTERING, (float) MAX_FILTERING);
                rssiFilter.R = rssiFilteringSmoothness;
                setFilteringValueTextView(filteringSeekBar.getProgress());
            } else if (seekBar == delaySeekBar) {
                alarmDelay = HelperFunctions.map(delaySeekBar.getProgress(), 0f, 100f, (float) MIN_ALARM_DELAY, (float) MAX_ALARM_DELAY);
                setDelayValueTextView(alarmDelay);
            }
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
            prefs.edit().putInt(PREF_THRESHOLD, rssiThreshold).apply();
            canActivateAlarmNow = true;
        }
        else if(seekBar == filteringSeekBar) {
            prefs.edit().putFloat(PREF_FILTERING, rssiFilteringSmoothness).apply();
        }
        else if(seekBar == delaySeekBar) {
            prefs.edit().putFloat(PREF_DELAY, alarmDelay).apply();
        }
    }

    void setThresholdValue(int valueToSet){
        DataPoint thresholdDataPoints[] = {
                new DataPoint(0, (float)(valueToSet)),
                new DataPoint(numberOfRSSIReadings, (float)(valueToSet))
        };
        if(thresholdRSSIGraphSeries != null) {
            thresholdRSSIGraphSeries.resetData(thresholdDataPoints);
        }
        setThresholdValueText(valueToSet);
    }

    void setThresholdValueText(int valueToSet) {
        if(thresholdValueTextView != null) {
            thresholdValueTextView.setText(String.valueOf(valueToSet) + getResources().getString(R.string.db));
        }
    }

    void setFilteringValueTextView(double valueToSet) {
        if(thresholdValueTextView != null) {
            filteringValueTextView.setText(/*String.format(Locale.ENGLISH, "%.5f", valueToSet)*/(int)valueToSet + getResources().getString(R.string.percent));
        }
    }

    void setDelayValueTextView(double valueToSet) {
        if(delayValueTextView != null) {
            delayValueTextView.setText(String.format(Locale.ENGLISH, "%.1f", valueToSet) + getResources().getString(R.string.seconds));
        }
    }

    // -------------------- RSSI VALUE --------------------

    @SuppressWarnings("unused")
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
            if(canActivateAlarmNow && isConnected) {
                alarmDelayHandler.postDelayed(activateDelayedAlarm, (long)(alarmDelay * 1000));
            }
            if(rssiValueTextView != null) {
                rssiValueTextView.setTextColor(Color.RED);
            }
        }
        else {
            if(isConnected && !isWaitingToDisconnect) {
                cancelAlarm();
            }
            if(rssiValueTextView != null) {
                rssiValueTextView.setTextColor(Color.BLACK);
            }
        }

    }

    // -------------------- ALARM --------------------

    void activateAlarm() {
        if(!isOutOfRange) {
            sendBluetoothData("X"); // Alarm on
            repeatVibration.run();
            showAlarmDialog();
            isOutOfRange = true;
        }
    }

    void cancelAlarm() {
        cancelAlarm(true);
    }

    void cancelAlarm(boolean doAllowAlarmToContinue) {
        if(isOutOfRange) {
            if(alarmDialog != null) {
                alarmDialog.cancel();
            }
            sendBluetoothData("x"); // Alarm off
            vibrationRepeatHandler.removeCallbacks(repeatVibration);
            vibrator.cancel();
            isOutOfRange = false;
        }
        alarmDelayHandler.removeCallbacks(activateDelayedAlarm);
        canActivateAlarmNow = doAllowAlarmToContinue;
    }

    private void showAlarmDialog() {
        alarmDialog = new AlertDialog.Builder(this)
                .setMessage( isConnected ? getResources().getString(R.string.alarm_dialog_message_out_of_range) : getResources().getString(R.string.alarm_dialog_message_lost_connection) )
                .setTitle(getResources().getString(R.string.alarm_dialog_title))
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                cancelAlarm(false);
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create();
        try {
            alarmDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
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
                if(sendBluetoothData("!") && isWaitingToSendInitialStatusUpdate) {
                    updateNotificationsList(true);
                    sendBluetoothData("2"); // Medium vibration
                    isWaitingToSendInitialStatusUpdate = false;
                }
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

    Runnable activateDelayedAlarm = new Runnable() {
        @Override
        public void run() {
            try {
                activateAlarm();
            } finally { }
        }
    };

    Runnable repeatVibration = new Runnable() {
        @Override
        public void run() {
            try {
                vibrator.vibrate(ALARM_VIBRATE_PATTERN, 0);
            } finally {
                vibrationRepeatHandler.postDelayed(repeatVibration, VIBRATION_REPEAT_TIME);
            }
        }
    };

    Runnable remindOfNotifications = new Runnable() {
        @Override
        public void run() {
            try {
                //Log.v("NOTIFICATIONS", "REMINDING NOW !!!");
                updateNotificationsList(true);
            } finally {
                if(notificationReminderTime > 0) {
                    notificationReminderHandler.postDelayed(remindOfNotifications, notificationReminderTime);
                }
            }
        }
    };

    // -------------------- NOTIFICATIONS --------------------

    // Code to manage Notification Monitor service lifecycle
    private final ServiceConnection mNLServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.v("NOTIFICATION MONITOR", "OnServiceConnected...");
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.v("NOTIFICATION MONITOR", "OnServiceDisconnected...");
        }
    };

    private void setupNotificationListener() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NotificationMonitorService.ACTION_NOTIFICATION_EVENT);
        registerReceiver(notificationCallbackReceiver, intentFilter);

        boolean canUseCachedPackageList = true;
        int numberOfPackages;

        final PackageManager packageManager = getPackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
        //String currentPackagesListHash = HelperFunctions.MD5(packages.toArray().toString());

        //if(packageNamesString.length() == 0) {
        //    canUseCachedPackageList = false;
        //}
        //else {

            ArrayList<String> temp_currentPackageNames = new ArrayList<>();
            ArrayList<String> temp_cachedPackageNames = new ArrayList<>();
            for (PackageInfo packageInfo : packages) {
                if (!HelperFunctions.isSystemPackage(packageInfo)) {
                    String currentPackageName = (String) (packageInfo.applicationInfo != null ? packageManager.getApplicationLabel(packageInfo.applicationInfo) : "");
                    if(!currentPackageName.trim().isEmpty()) {
                        temp_currentPackageNames.add(packageInfo.packageName);
                    }
                }
            }

            numberOfPackages = temp_currentPackageNames.size();
            temp_cachedPackageNames.addAll(Arrays.asList(packageNamesString.split("\n")));

            if(!temp_currentPackageNames.containsAll(temp_cachedPackageNames) || !temp_cachedPackageNames.containsAll(temp_currentPackageNames)) {
                canUseCachedPackageList = false;
            }

        //}

        ArrayList<String> redNotificationGroupNames = new ArrayList<>();
        ArrayList<String> greenNotificationGroupNames = new ArrayList<>();
        ArrayList<String> blueNotificationGroupNames = new ArrayList<>();

        packageNames.clear();
        packageAppNames.clear();

        if(!canUseCachedPackageList){

            //packagesListHash = currentPackagesListHash;
            //prefs.edit().putString(PREF_PACKAGES_LIST_HASH, packagesListHash).apply();
            Collections.sort(packages, new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo o1, PackageInfo o2) {
                    String package1Name = (String)(o1.applicationInfo != null ? packageManager.getApplicationLabel(o1.applicationInfo) : "(unknown)");
                    String package2Name = (String)(o2.applicationInfo != null ? packageManager.getApplicationLabel(o2.applicationInfo) : "(unknown)");
                    return package1Name.toUpperCase().compareTo(package2Name.toUpperCase());
                }
            });

            packageNamesString = "";
            packageAppNamesString = "";

            int currentPackageIndex = 0;
            for (PackageInfo packageInfo : packages) {
                if(!HelperFunctions.isSystemPackage(packageInfo)) {

                    String packageName = packageInfo.packageName;
                    String packageAppName = (String) (packageInfo.applicationInfo != null ? packageManager.getApplicationLabel(packageInfo.applicationInfo) : "(unknown)");
                    if(packageAppName.trim().isEmpty()) {
                        continue;
                    }
                    packageNames.add(packageName);
                    packageAppNames.add(packageAppName);

                    if(currentPackageIndex != numberOfPackages - 1) {
                        packageNamesString += packageName + "\n";
                        packageAppNamesString += packageAppName + "\n";
                    }
                    else {
                        packageNamesString += packageName;
                        packageAppNamesString += packageAppName;
                    }

                    currentPackageIndex++;

                }
            }

            prefs.edit().putString(PREF_PACKAGES_NAMES, packageNamesString).apply();
            prefs.edit().putString(PREF_PACKAGES_APP_NAMES, packageAppNamesString).apply();

        }
        else {
            packageNames.addAll(Arrays.asList(packageNamesString.split("\n")));
            packageAppNames.addAll(Arrays.asList(packageAppNamesString.split("\n")));
        }

        for(int i = 0; i < packageNames.size(); i++) {
            String currentPackageName = packageNames.get(i);
            String currentPackageAppName = packageAppNames.get(i);

            if(redNotificationGroup.contains(currentPackageName)) {
                redNotificationGroupNames.add(currentPackageAppName);
            }
            if(greenNotificationGroup.contains(currentPackageName)) {
                greenNotificationGroupNames.add(currentPackageAppName);
            }
            if(blueNotificationGroup.contains(currentPackageName)) {
                blueNotificationGroupNames.add(currentPackageAppName);
            }
        }

        redNotificationGroupMultiSelectionSpinner.setItems(packageAppNames);
        greenNotificationGroupMultiSelectionSpinner.setItems(packageAppNames);
        blueNotificationGroupMultiSelectionSpinner.setItems(packageAppNames);

        redNotificationGroupMultiSelectionSpinner.setListener(this);
        greenNotificationGroupMultiSelectionSpinner.setListener(this);
        blueNotificationGroupMultiSelectionSpinner.setListener(this);

        redNotificationGroupMultiSelectionSpinner.setSelection(redNotificationGroupNames);
        greenNotificationGroupMultiSelectionSpinner.setSelection(greenNotificationGroupNames);
        blueNotificationGroupMultiSelectionSpinner.setSelection(blueNotificationGroupNames);

        redNotificationGroupMultiSelectionSpinner.setTitle(getResources().getString(R.string.pick_red_notification_group));
        greenNotificationGroupMultiSelectionSpinner.setTitle(getResources().getString(R.string.pick_green_notification_group));
        blueNotificationGroupMultiSelectionSpinner.setTitle(getResources().getString(R.string.pick_blue_notification_group));

    }

    private final BroadcastReceiver notificationCallbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (NotificationMonitorService.ACTION_NOTIFICATION_EVENT.equals(action)) {
                updateNotificationsList();
            }
        }
    };

    private void updateNotificationsList(boolean forceUpdate) {
        if(forceUpdate) {
            NotificationMonitorService.mCurrentNotifications.clear();
        }
        updateNotificationsList();
    }

    @TargetApi(23)
    private void updateNotificationsList() {
        if (isEnabledNLS) {

            //Log.v("NOTIFICATIONS", "Updating notifications...");

            StatusBarNotification[] currentNotifications = NotificationMonitorService.getCurrentNotifications();
            if (currentNotifications == null) {
                sendBluetoothData("ijk"); // Turn off all flashing
                previousNumberOfNotifications = 0;
            }
            else {

                //Log.v("NOTIFICATIONS", "Do not disturb mode: " + (notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_NONE ? "On" : "Off"));

                if (notificationManager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_NONE) {

                    int currentNumberOfNotifications = currentNotifications.length;
                    //Log.v("NOTIFICATIONS", "Current count: " + currentNumberOfNotifications + " Previous count: " + previousNumberOfNotifications);

                    ArrayList<String> currentNotificationsPackageNames = new ArrayList<>();
                    for (StatusBarNotification currentStatusBarNotification : currentNotifications) {
                        currentNotificationsPackageNames.add(currentStatusBarNotification.getPackageName());
                    }

                    // At least one notification from the red group
                    if (!Collections.disjoint(currentNotificationsPackageNames, redNotificationGroup)) {
                        if(currentNumberOfNotifications >= previousNumberOfNotifications) {
                            sendBluetoothData("I1"); // Flash red LED, short motor vibration
                        }
                        else {
                            sendBluetoothData("I"); // Flash red LED, no motor vibration
                        }
                        //if(!notificationReminderIsRunning && notificationReminderTime > 0) {
                        //    notificationReminderHandler.postDelayed(remindOfNotifications, notificationReminderTime);
                        //}
                    } else {
                        sendBluetoothData("i");
                    }

                    // At least one notification from the green group
                    if (!Collections.disjoint(currentNotificationsPackageNames, greenNotificationGroup)) {
                        if(currentNumberOfNotifications >= previousNumberOfNotifications) {
                            sendBluetoothData("J1"); // Flash green LED, short motor vibration
                        }
                        else {
                            sendBluetoothData("J"); // Flash green LED, no motor vibration
                        }
                        //if(!notificationReminderIsRunning && notificationReminderTime > 0) {
                        //    notificationReminderHandler.postDelayed(remindOfNotifications, notificationReminderTime);
                        //}
                    } else {
                        sendBluetoothData("j");
                    }

                    // At least one notification from the blue group
                    if (!Collections.disjoint(currentNotificationsPackageNames, blueNotificationGroup)) {
                        if(currentNumberOfNotifications >= previousNumberOfNotifications) {
                            sendBluetoothData("K1"); // Flash blue LED, short motor vibration
                        }
                        else {
                            sendBluetoothData("K"); // Flash blue LED, no motor vibration
                        }
                        //if(!notificationReminderIsRunning && notificationReminderTime > 0) {
                        //    notificationReminderHandler.postDelayed(remindOfNotifications, notificationReminderTime);
                        //}
                    } else {
                        sendBluetoothData("k");
                    }

                    previousNumberOfNotifications = currentNotifications.length;

                }

            }
        }//else {
            //openNotificationAccess();
            //mTextView.setText("Please Enable Notification Access");
        //}
    }

    private void openNotificationAccess() {
        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Please enable " + getResources().getString(R.string.app_name) + " access")
                .setTitle("Notification Access")
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                openNotificationAccess();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create().show();
    }

    private boolean NLSIsEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String currentName : names) {
                final ComponentName cn = ComponentName.unflattenFromString(currentName);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -------------------- BLUETOOTH CONNECTION --------------------

    boolean sendBluetoothData(String dataToSend) {
        boolean didSendAllData = false;
        if(mBluetoothLeService != null) {
            for (int currentCharIndex = 0; currentCharIndex < dataToSend.length(); currentCharIndex++) {
                didSendAllData = mBluetoothLeService.writeCustomCharacteristic((int)(dataToSend.charAt(currentCharIndex)));
                if(dataToSend.length() > 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return didSendAllData;
    }

    @TargetApi(23)
    void processReceivedBluetoothData(String dataReceived) {

        String[] dataReceivedSplit = dataReceived.split("\n", 2);
        dataReceived = dataReceivedSplit[0].replace("\r", "");

        Log.v("DATA", "Received Data: " + dataReceived);

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if(!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }

        if(dataReceived.equals(COMMAND_SWITCH_POSITION_1)) { // Sound
            if(buttonIsPressed) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                updateNotificationsList(true);
            }
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            sendBluetoothData(COMMAND_ACK);
        }
        else if(dataReceived.equals(COMMAND_SWITCH_POSITION_2)) { // Vibrate
            if(buttonIsPressed) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                updateNotificationsList(true);
            }
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            sendBluetoothData(COMMAND_ACK);
        }
        else if(dataReceived.equals(COMMAND_SWITCH_POSITION_3)) { // Silent
            if(buttonIsPressed) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                sendBluetoothData("ijk");
            }
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            sendBluetoothData(COMMAND_ACK);
        }
        else if(dataReceived.equals(COMMAND_BUTTON_PRESSED)) {
            //vibrator.vibrate(1000000);
            buttonIsPressed = true;
            if(alarmDialog != null) {
                if (alarmDialog.isShowing()) {
                    cancelAlarm(false);
                }
            }
            sendBluetoothData(COMMAND_ACK);
        }
        else if(dataReceived.equals(COMMAND_BUTTON_RELEASED)) {
            buttonIsPressed = false;
            sendBluetoothData(COMMAND_ACK);
        }

    }

    // Code to manage BLE service lifecycle
    private final ServiceConnection mBLEServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.v("BLE CONNECTION", "OnServiceConnected...");

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("BLE CONNECTION", "Unable to initialize Bluetooth");
                finish();
            }
            if(mBluetoothAdapter == null) {
                isWaitingToAutoConnect = true;
            }
            else {
                if (mBluetoothAdapter.isEnabled()) {
                    attemptConnectionToLastConnectedBluetoothDevice();
                } else {
                    isWaitingToAutoConnect = true;
                }
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.v("BLE CONNECTION", "OnServiceDisconnected...");
            mBluetoothLeService = null;
        }
    };

    void attemptConnectionToLastConnectedBluetoothDevice() {
        // Automatically connects to the device upon successful start-up initialization.
        if(currentSelectedBluetoothAddress.contains(":") && currentSelectedBluetoothName.contains(PROXIMITY_BAND_BLE_NAME) && !isConnected) {
            connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.connecting) + " " + currentSelectedBluetoothName + "...");
            mBluetoothLeService.connect(currentSelectedBluetoothAddress);
        }
        isWaitingToAutoConnect = false;
    }

    void setUpConnectToBluetooth() {

        isConnected = true;
        isWaitingToDisconnect = false;
        canActivateAlarmNow = true;
        buttonIsPressed = false;
        sendRepeatingPing.run();
        connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.connected) + " " + currentSelectedBluetoothName);
        connectionButton.setText(R.string.disconnect);

        scanLeDevice(false);
        setConnectionTime(-1);
        updateConnectionTime.run();

        ArrayList<String> bluetoothDevicesNames = new ArrayList<>();
        bluetoothDevicesNames.add(currentSelectedBluetoothName);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothDevicesNames);
        bluetoothDeviceSelectionSpinner.setAdapter(adapter);
        bluetoothDeviceSelectionSpinner.setEnabled(false);

        isWaitingToSendInitialStatusUpdate = true;

    }

    void setUpDisconnectFromBluetooth() {

        isConnected = false;
        pingHandler.removeCallbacks(sendRepeatingPing);
        connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.disconnected));
        connectionButton.setText(R.string.connect);

        if(canActivateAlarmNow && !isWaitingToDisconnect) {
            activateAlarm();
        }
        isWaitingToDisconnect = false;

        connectionTimeHandler.removeCallbacks(updateConnectionTime);
        rssiValueTextView.setText("---");
        scanLeDevice(true);

        bluetoothDevices.clear();
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.none_ble_devices));
        bluetoothDeviceSelectionSpinner.setAdapter(adapter);
        bluetoothDeviceSelectionSpinner.setEnabled(true);

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
            //isScanning = true;
            connectionStatusTextView.setText(getResources().getString(R.string.status) + " " + getResources().getString(R.string.scanning));
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            updateRSSIValue.run();
        } else {
            //isScanning = false;
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
                            if(!isWaitingToAutoConnect && device.getAddress().equals(currentSelectedBluetoothAddress)) {
                                currentRSSI = rssi;
                                setRSSIValue(rssi, false);
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
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                processReceivedBluetoothData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_READ_REMOTE_RSSI.equals(action)) {
                //Log.d("RSSI", String.format("Got RSSI: %d", currentRSSI));
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
