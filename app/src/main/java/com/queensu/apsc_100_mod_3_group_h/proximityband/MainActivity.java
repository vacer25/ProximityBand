package com.queensu.apsc_100_mod_3_group_h.proximityband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

// Main Activity Test Change by Vasa
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    // -------------------- OBJECTS VARIABLES --------------------
    // This is a test from Jafer
    SeekBar sensitivitySeekBar;
    TextView sensitivityValueTextView;
    TextView valueTextView;

    private BluetoothAdapter mBluetoothAdapter;

    // -------------------- ACTIVITY LIFECYCLE --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBLECompatibility();
        getActivityViewReferences();

    }

    @Override
    protected void onResume() {
        super.onResume();
        HelperFunctions.hideActionBarAndStatusBar(this);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        HelperFunctions.hideActionBarAndStatusBar(this);
    }

    // -------------------- GET REFERENCES TO UI --------------------

    void getActivityViewReferences() {

        sensitivitySeekBar = (SeekBar)findViewById(R.id.sensitivitySeekBar);
        sensitivityValueTextView = (TextView)findViewById(R.id.sensitivityValueTextView);
        valueTextView = (TextView)findViewById(R.id.valueTextView);

        sensitivitySeekBar.setOnSeekBarChangeListener(this);
        setSensitivityValueText(0);

    }

    // -------------------- USER INTERACTION --------------------

    void onSetButtonActionClicked(View v) {
        HelperFunctions.vibrate(this, 50);
    }

    void onSetBluetoothDeviceClicked(View v) {
        HelperFunctions.vibrate(this, 50);
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
        HelperFunctions.vibrate(this, 25);
    }

    void setSensitivityValueText(int valueToSet) {
        if(sensitivityValueTextView != null) {
            sensitivityValueTextView.setText(String.valueOf(valueToSet));
        }
        if(valueTextView != null) {
            valueTextView.setText(String.valueOf(valueToSet));
        }
    }

    // -------------------- BLUETOOTH --------------------

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

}
