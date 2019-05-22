package ch.meineKanti.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    String tag = "Status: ";
    String url;
    EditText inputurl;
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    boolean urlisvalid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputurl = findViewById(R.id.mobilelink);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("schulnetz-noten", "Schulnetz Noten", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Benachrichtigungen über neue Noten.");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationChannel channel1 = new NotificationChannel("schulnetz-agenda", "Schulnetz Nächste Lektion", NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setSound(null, null);
            channel1.setDescription("Benachrichtigung über nächste Lektion.");
            channel1.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager1 = getSystemService(NotificationManager.class);
            notificationManager1.createNotificationChannel(channel1);

            NotificationChannel channel2 = new NotificationChannel("schulnetz-absenzen", "Schulnetz Absenzen", NotificationManager.IMPORTANCE_DEFAULT);
            channel2.setDescription("Benachrichtigungen über neue Absenzen");
            NotificationManager notificationManager2 = getSystemService(NotificationManager.class);
            notificationManager2.createNotificationChannel(channel2);

            NotificationChannel channel3 = new NotificationChannel("schulnetz-ausfälle", "Schulnetz Ausfallende Lektionen", NotificationManager.IMPORTANCE_DEFAULT);
            channel2.setDescription("Benachrichtigungen über ausfallende Lektionen");
            NotificationManager notificationManager3 = getSystemService(NotificationManager.class);
            notificationManager3.createNotificationChannel(channel3);
        }

        sharedPrefs = getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);
        editor = sharedPrefs.edit();

        if (sharedPrefs.getBoolean("firsttimer", true)) {
            editor.putBoolean("firsttimer", false);

            //Executed only on first run

        }

        if (sharedPrefs.contains("schulnetzURL")) {
            Intent goingon = new Intent("ch.meineKanti.mobile.secondActivity");
            startActivity(goingon);
            finish();
        }
    }
    public void onClickEvent(View view) {
        refresh();
        if (urlisvalid) {
            writeURL();
            Intent goingon = new Intent("ch.meineKanti.mobile.secondActivity");
            startActivity(goingon);
            editor.putBoolean("benachrichtigungen-noten", true);
            editor.putBoolean("benachrichtigungen-agenda", true);
            editor.putBoolean("benachrichtigungen-absenzen", true);
            editor.putBoolean("benachrichtigungen-ausfälle", true);
            editor.commit();
            finish();
        }

    }


    public void writeURL() {
            url = inputurl.getText().toString();
            editor.putString("schulnetzURL", url);
            editor.commit();
    }

    public void refresh() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (inputurl.getText().toString().contains("www.schul-netz.com/baden/mindex.php")) {
                    try {
                        Document page = Jsoup.connect(inputurl.getText().toString()).get();
                        urlisvalid = true;
                        Log.e("DEDBUG", "conn fine");
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Dein SchulNetz-Link funktioniert nicht.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Das ist kein Schulnetz.mobile Link.", Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
