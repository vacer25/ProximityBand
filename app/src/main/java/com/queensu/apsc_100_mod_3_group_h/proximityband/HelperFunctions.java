package com.queensu.apsc_100_mod_3_group_h.proximityband;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Admin on 3/6/2017.
 * Test
 */

public class HelperFunctions {

    static void vibrate(Activity activity, int time) {
        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(time);
    }

    // ------------------------------ ACTIVITY SETTINGS ------------------------------

    static void hideActionBarAndStatusBar(Activity activity) {

        // Hide the action bar.
        ActionBar actionBar = ((AppCompatActivity)activity).getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Hide the status bar.
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

    }

    // ------------------------------ DISPLAY TOAST MESSAGE ------------------------------

    public static void displayToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}
