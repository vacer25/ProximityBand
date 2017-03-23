package com.queensu.apsc_100_mod_3_group_h.proximityband;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferenceSettings {

    public static void setNotificationSettings() {

        SharedPreferences.Editor editor = MainActivity.prefs.edit();

        Set redNotificationGroupSet = new HashSet<>();
        for(String currentNotification : MainActivity.redNotificationGroup) {
            redNotificationGroupSet.add(String.valueOf(currentNotification));
        }
        editor.putStringSet(MainActivity.PREF_RED_NOTIFICATIONS, redNotificationGroupSet).apply();

        Set greenNotificationGroupSet = new HashSet<>();
        for(String currentNotification : MainActivity.greenNotificationGroup) {
            greenNotificationGroupSet.add(String.valueOf(currentNotification));
        }
        editor.putStringSet(MainActivity.PREF_GREEN_NOTIFICATIONS, greenNotificationGroupSet).apply();

        Set blueNotificationGroupSet = new HashSet<>();
        for(String currentNotification : MainActivity.blueNotificationGroup) {
            blueNotificationGroupSet.add(String.valueOf(currentNotification));
        }
        editor.putStringSet(MainActivity.PREF_BLUE_NOTIFICATIONS, blueNotificationGroupSet).apply();

    }

    public static void getNotificationSettings() {

        SharedPreferences preferences = MainActivity.prefs;

        Set redNotificationGroupSet = new HashSet<>();
        redNotificationGroupSet = preferences.getStringSet(MainActivity.PREF_RED_NOTIFICATIONS, redNotificationGroupSet);
        MainActivity.redNotificationGroup.clear();
        for(Object currentNotificationInSet : redNotificationGroupSet) {
            MainActivity.redNotificationGroup.add(String.valueOf(currentNotificationInSet));
        }

        Set greenNotificationGroupSet = new HashSet<>();
        greenNotificationGroupSet = preferences.getStringSet(MainActivity.PREF_GREEN_NOTIFICATIONS, greenNotificationGroupSet);
        MainActivity.greenNotificationGroup.clear();
        for(Object currentNotificationInSet : greenNotificationGroupSet) {
            MainActivity.greenNotificationGroup.add(String.valueOf(currentNotificationInSet));
        }

        Set blueNotificationGroupSet = new HashSet<>();
        blueNotificationGroupSet = preferences.getStringSet(MainActivity.PREF_BLUE_NOTIFICATIONS, blueNotificationGroupSet);
        MainActivity.blueNotificationGroup.clear();
        for(Object currentNotificationInSet : blueNotificationGroupSet) {
            MainActivity.blueNotificationGroup.add(String.valueOf(currentNotificationInSet));
        }

    }

}