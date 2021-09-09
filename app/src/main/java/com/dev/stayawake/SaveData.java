package com.dev.stayawake;

import android.content.Context;
import android.content.SharedPreferences;

public class SaveData {

    public static final String __SaveData_Key = "pref";
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;

    private static SaveData savedata;

    public static SaveData getInstance(Context context) {
        if (pref == null) {
            pref = context.getSharedPreferences(__SaveData_Key, Context.MODE_PRIVATE);
            editor = pref.edit();
            savedata = new SaveData();
        }
        return savedata;
    }

    static void setAlarmPoint(int val) {
        editor.putInt("alarmPoint", val);
        editor.commit();
    }

    static int getAlarmPoint() {
        return pref.getInt("alarmPoint", 20);
    }

    public static void setAppVersion(String appVersion) {
        editor.putString("appVersion", appVersion);
        editor.commit();
    }
}
