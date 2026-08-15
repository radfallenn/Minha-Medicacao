package com.radfallenn.minhamedicacao;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public final class Scheduler {
    private static final String PREFS = "medication_native";
    private static final String KEY_JSON = "meds_json";

    private Scheduler() {}

    public static void scheduleAll(Context context, String json) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_JSON, json).apply();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject med = arr.getJSONObject(i);
                scheduleOne(context, med);
            }
        } catch (Exception ignored) { }
    }

    public static void rescheduleStored(Context context) {
        String json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_JSON, "[]");
        scheduleAll(context, json);
    }

    private static void scheduleOne(Context context, JSONObject med) {
        try {
            String id = med.optString("id", "");
            String name = med.optString("name", "Medicamento");
            String dose = med.optString("dose", "");
            String note = med.optString("note", "");
            String time = med.optString("time", "08:00");
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar when = Calendar.getInstance();
            when.set(Calendar.HOUR_OF_DAY, hour);
            when.set(Calendar.MINUTE, minute);
            when.set(Calendar.SECOND, 0);
            when.set(Calendar.MILLISECOND, 0);
            if (when.getTimeInMillis() <= System.currentTimeMillis()) when.add(Calendar.DAY_OF_YEAR, 1);

            Intent intent = new Intent(context, MedicationAlarmReceiver.class);
            intent.putExtra("id", id);
            intent.putExtra("name", name);
            intent.putExtra("dose", dose);
            intent.putExtra("note", note);
            intent.putExtra("time", time);

            int requestCode = id.isEmpty() ? (name + time).hashCode() : id.hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            long trigger = when.getTimeInMillis();
            if (Build.VERSION.SDK_INT >= 31) {
                if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } catch (Exception ignored) { }
    }
}
