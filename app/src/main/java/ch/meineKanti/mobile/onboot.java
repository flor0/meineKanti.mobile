package ch.meineKanti.mobile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class onboot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {


        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
        .build();

        PeriodicWorkRequest worker = new PeriodicWorkRequest.Builder(backgroundWorker.class, 16, TimeUnit.MINUTES, 30, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance()
                .enqueueUniquePeriodicWork("stundenplaner", ExistingPeriodicWorkPolicy.KEEP, worker);

    }

}
