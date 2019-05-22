package ch.meineKanti.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.concurrent.TimeUnit;

public class secondActivity extends AppCompatActivity {
    String schulNetzURI; //= "https://www.schul-netz.com/baden/mindex.php?longurl=eH8rVDghh3eFxew3nnb0iLrDMGP2LhFEnzCy0BWJX4d23TQwXNVwagrZ9976m7Qd";
    String schulnetzfetched;
    EditText pintext;
    String pin;
    Intent intent = new Intent("ch.meineKanti.mobile.showgrades");
    Toolbar toolbar;
    SharedPreferences prefs;
    boolean pinisvalid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Login");

        prefs = getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);
        schulNetzURI = prefs.getString("schulnetzURL", "");

        // If PIN is being remembered
        if (prefs.getBoolean("rememberme-status", false)) {
            startActivity(new Intent("ch.meineKanti.mobile.showgrades"));
            Log.e("STATUS", "ACTIVITY SHOWGRADES STARTED / BOX WAS CHECKED");

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest worker = new PeriodicWorkRequest.Builder(backgroundWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();
            WorkManager.getInstance()
                    .enqueueUniquePeriodicWork("stundenplaner", ExistingPeriodicWorkPolicy.KEEP, worker);

            finish();
        }

        // If you enter the PIN manually
        Button loginButton = findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e("status", "clickedlogin");
                pintext = findViewById(R.id.mobilePin);
                pin = pintext.getText().toString();

                refresh();

                if (pinisvalid) {
                    SharedPreferences.Editor editor;
                    editor = prefs.edit();
                    editor.putString("rememberme-pin", pin);
                    editor.commit();

                    CheckBox rememberme = findViewById(R.id.rememberPIN);
                    if (rememberme.isChecked()) {
                        editor.putBoolean("rememberme-status", true);
                        editor.commit();
                        Log.e("STATUS", "PIN CHECKED");
                    }
                    else {
                        editor.putBoolean("rememberme-status", false);
                        editor.commit();
                        Log.e("STATUS", "PIN UNCHECKED");
                    }
                    startActivity(new Intent("ch.meineKanti.mobile.showgrades"));
                    Log.e("STATUS", "ACTIVITY SHOWGRADES STARTED / YOU MANUALLY LOGGED IN");

                    Constraints constraints = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();
                    PeriodicWorkRequest worker = new PeriodicWorkRequest.Builder(backgroundWorker.class, 15, TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .build();
                    WorkManager.getInstance()
                            .enqueueUniquePeriodicWork("stundenplaner", ExistingPeriodicWorkPolicy.KEEP, worker);

                    finish();
                }



            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawermenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                // Do whatever you want to do on logout click.
                logout();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void logout() {
        SharedPreferences shpf = getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);
        SharedPreferences.Editor edit = shpf.edit();
        edit.clear();
        edit.commit();
        finish();

    }

    public void refresh() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences prefs = getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);
                    String schulnetzURL = prefs.getString("schulnetzURL", "");
                    Document page = Jsoup.connect(schulnetzURL)
                            .data("pin", pin, "pc", "1")
                            .post();;
                    String temp = page.text();
                    if (temp.contains("falsch")) {
                        pinisvalid = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Dein SchulNetz-PIN funktioniert nicht.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    else {
                        pinisvalid = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Dein SchulNetz-Link funktioniert nicht.", Toast.LENGTH_LONG).show();
                        }
                    });
                    Document page = Jsoup.parse("<html></html>");
                }
            }
        });
        t.start();
        try {
            t.join();
            Log.e("DEGUB", "JONED");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
