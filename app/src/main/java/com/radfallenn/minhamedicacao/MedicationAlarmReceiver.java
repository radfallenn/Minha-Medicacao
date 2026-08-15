package com.radfallenn.minhamedicacao;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class MedicationAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "medication_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        String dose = intent.getStringExtra("dose");
        String note = intent.getStringExtra("note");
        String id = intent.getStringExtra("id");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes de medicação",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisos no horário dos medicamentos cadastrados");
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                1,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        StringBuilder body = new StringBuilder();
        if (dose != null && !dose.isEmpty()) body.append(dose);
        if (note != null && !note.isEmpty()) {
            if (body.length() > 0) body.append(" · ");
            body.append(note);
        }
        if (body.length() == 0) body.append("Hora de tomar sua medicação.");

        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Hora da medicação: " + (name == null ? "Medicamento" : name))
                .setContentText(body.toString())
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body.toString()))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(android.app.Notification.PRIORITY_HIGH)
                .build();

        int notificationId = id == null || id.isEmpty() ? (name == null ? 1000 : name.hashCode()) : id.hashCode();
        try { nm.notify(notificationId, notification); } catch (SecurityException ignored) { }

        Scheduler.rescheduleStored(context);
    }
}
