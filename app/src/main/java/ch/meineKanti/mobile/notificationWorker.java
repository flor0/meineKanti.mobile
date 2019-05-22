package ch.meineKanti.mobile;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class notificationWorker extends Worker {
    public notificationWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("NOTIFICATION WORKER", "RUN");
        Intent intent = new Intent(getApplicationContext(), showgrades.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        String message = getInputData().getString("notification_message");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "schulnetz-agenda")
                .setSmallIcon(R.drawable.ic_meinekanti_icon_grey)
                .setContentTitle("Nächste Lektion")
                .setContentText("")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(1000*60*10);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationManagerCompat.notify(8880, builder.build());
        Log.e("NOTIFICATION WORKER", "DONE");

        return Result.success();
    }
}
