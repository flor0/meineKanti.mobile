package ch.meineKanti.mobile;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.view.MotionEventCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
//TODO:Swipe left or right
//TODO:Collapse toolbar
public class showgrades extends AppCompatActivity {
    SharedPreferences prefs;
    ArrayList<String> finallyshown;
    ArrayAdapter adapter;
    ListView liste;
    SwipeRefreshLayout swipeToRefresh;
    Toolbar toolbar;
    BottomNavigationView bottomnav;
    String[] colors;
    Map<String, String> timeends;
    String schulnetzURL;
    String pin;
    Document page;
    ArrayList<String> finallyshown_agenda;
    ArrayList<String> finallyshown_noten;
    ArrayList<String> finallyshown_absenzen;
    int checker = 0;//0=Agenda, 1=Noten, 2=Absenzen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showgrades);

        prefs = getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);
        schulnetzURL = prefs.getString("schulnetzURL", "");
        pin = prefs.getString("rememberme-pin", "");

        bottomnav = findViewById(R.id.bottomnav);
        bottomnav.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

        bottomnav.setSelectedItemId(R.id.bottomnav_menu_stundenplan);

        //TODO:REMOVE
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(backgroundWorker.class).build();
        WorkManager.getInstance().enqueue(request);

        timeends = new HashMap<>();
        timeends.put("07:30", "08:15");
        timeends.put("08:25", "09:10");
        timeends.put("09:20", "10:05");
        timeends.put("10:25", "11:10");
        timeends.put("11:20", "12:05");
        timeends.put("12:35", "13:20");
        timeends.put("13:30", "14:15");
        timeends.put("14:25", "15:10");
        timeends.put("15:20", "16:05");
        timeends.put("16:15", "17:00");
        timeends.put("17:10", "17:55");
        timeends.put("18:05", "18:50");


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Agenda");
        toolbar.setTitleTextColor(0xFFFFFFFF);
        toolbar.setOverflowIcon(getDrawable(R.drawable.ic_more_vert_black_24dp));

        liste = findViewById(R.id.dynamiclist);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    page = Jsoup.connect(schulnetzURL)
                            .data("pin", pin, "pc", "1")
                            .post();
                    Log.w("STATUS", "CONNECTION SUCCESSFUL");
                    getStundenplan();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!finallyshown_agenda.get(0).equals("Du hast heute keine Stunden.")) {
                                adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, finallyshown_agenda) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        view.setBackgroundResource(R.drawable.customboxes);
                                        if (colors[position].equals("fb8c60")) {
                                            view.setBackgroundResource(R.drawable.customboxes_ausfall);
                                        }
                                        //view.setBackgroundColor(Color.parseColor("#" + colors[position]));
                                        return view;
                                    }
                                };
                                liste.setAdapter(adapter);
                            }
                            else {
                                adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, finallyshown_agenda);
                                liste.setAdapter(adapter);
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, new ArrayList<String>());
                            liste.setAdapter(adapter);
                            Toast.makeText(getApplicationContext(), "Verbindungsfehler", Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        }).start();





        //Refresh handler
        swipeToRefresh = findViewById(R.id.pullToRefresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            page = Jsoup.connect(schulnetzURL)
                                    .data("pin", pin, "pc", "1")
                                    .post();
                            Log.w("STATUS", "CONNECTION SUCCESSFUL");
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Verbindungsfehler", Toast.LENGTH_LONG).show();
                                    swipeToRefresh.setRefreshing(false);
                                }
                            });
                            return;
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                switch (checker) {
                                    case 0:
                                        getStundenplan();
                                        adapter.clear();
                                        adapter.addAll(finallyshown_agenda);
                                        adapter.notifyDataSetChanged();
                                        Log.w("REFRESHED", "STUNDENPLAN");
                                        break;
                                    case 1:
                                        getNoten();
                                        adapter.clear();
                                        adapter.addAll(finallyshown_noten);
                                        adapter.notifyDataSetChanged();
                                        Log.w("REFRESHED", "NOTEN");
                                        break;
                                    case 2:
                                        getAbsenzen();
                                        adapter.clear();
                                        adapter.addAll(finallyshown_absenzen);
                                        adapter.notifyDataSetChanged();
                                        Log.w("REFRESHED", "ABSENZEN");
                                        break;
                                }
                                swipeToRefresh.setRefreshing(false);
                                Log.d("Success", "Reloaded");
                            }
                        });
                    }
                }).start();
            }
        });

        bottomnav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                try {
                    switch (menuItem.getItemId()) {
                        case R.id.bottomnav_menu_stundenplan:

                            checker = 0;
                            getStundenplan();
                            if (!finallyshown_agenda.get(0).equals("Du hast heute keine Stunden.")) {
                                adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, finallyshown_agenda) {
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View view = super.getView(position, convertView, parent);
                                        view.setBackgroundResource(R.drawable.customboxes);
                                        if (colors[position].equals("fb8c60")) {
                                            view.setBackgroundResource(R.drawable.customboxes_ausfall);
                                        }
                                        //view.setBackgroundColor(Color.parseColor("#" + colors[position]));
                                        return view;
                                    }
                                };
                                liste.setAdapter(adapter);
                            }
                            else {
                                adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, finallyshown_agenda);
                                liste.setAdapter(adapter);
                            }


                        /*adapter.clear();
                        adapter.addAll(finallyshown_agenda);
                        adapter.notifyDataSetChanged();*/
                            setTitle("Agenda");
                            Log.w("STATUS", "STUNDENPLAN ERFOLG");
                            return true;
                        case R.id.bottomnav_menu_noten:
                            checker = 1;
                            getNoten();
                            adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, finallyshown_noten);
                            liste.setAdapter(adapter);
                            setTitle("Noten");
                            Log.w("STATUS", "NOTEN ERFOLG");
                            return true;
                        case R.id.bottomnav_menu_absenzen:
                            checker = 2;
                            getAbsenzen();
                            adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.listview_layout, finallyshown_absenzen);
                            liste.setAdapter(adapter);
                            setTitle("Absenzen");
                            Log.w("STATUS", "ABSENZEN ERFOLG");
                            return true;
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Verbindungsfehler", Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        });


    }

    //SWIPE LEFT/RIGHT



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawermenu, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest worker = new PeriodicWorkRequest.Builder(backgroundWorker.class, 16, TimeUnit.MINUTES, 30, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance()
                .enqueueUniquePeriodicWork("stundenplaner", ExistingPeriodicWorkPolicy.KEEP, worker);
        Log.e("STATUS", "PERIODIC BACKGROUNDWORKER STARTED IN SHOWGRADES");

    }

    public void getStundenplan() {
        Elements agendaUl = page.select("li[style]");
        // Deal with colors / Missing lessons or abnormalities
        colors = new String[agendaUl.size()];
        int n = 0;
        for (Element elem : agendaUl) {
            colors[n] = elem.attr("style").substring(elem.attr("style").indexOf("#") + 1, elem.attr("style").indexOf(";"));
            n++;
        }

        String[] agenda = new String[agendaUl.size()];
        String[] rooms = new String[agendaUl.size()];
        String[] times = new String[agendaUl.size()];

        n = 0;
        String[] templ;
        for (Element elem : agendaUl) {
            templ = elem.html().replace("&nbsp", "").substring(1).split(";;;");
            agenda[n] = templ[2];
            times[n] = templ[0];
            rooms[n] = templ[1];
            n++;
        }
        finallyshown_agenda = new ArrayList<>();
        for (int i = 0; i < agenda.length; i++) {
            if (timeends.get(times[i]) != null) {
                finallyshown_agenda.add(times[i]+" - "+timeends.get(times[i])+"\n"+agenda[i]+"\n"+rooms[i]);
            }
            else {
                finallyshown_agenda.add(times[i]+"\n"+agenda[i]+"\n"+rooms[i]);
            }
        }
            /*
            finallyshown_agenda.add("07:30 - 08:15\nFranzösisch\n9113");
            finallyshown_agenda.add("08:25 - 09:10\nFranzösisch\n9113");
            finallyshown_agenda.add("09:20 - 10:05\nBiologie\n7001");
            finallyshown_agenda.add("10:25 - 11:10\nPhysik\n2003");
            finallyshown_agenda.add("11:20 - 12:05\nMathematik\n1307");
            finallyshown_agenda.add("13:30 - 14:15\nMathematik\n1307");
            finallyshown_agenda.add("14:25 - 15:10\nEnglisch\n6203");
            finallyshown_agenda.add("15:20 - 16:05\nGeschichte\n1104");
            finallyshown_agenda.add("16:15 - 17:00\nFranzösisch\n9113");
            */

        if (finallyshown_agenda.size() == 0) {
            finallyshown_agenda.add("Du hast heute keine Stunden.");
        }
    }

    public void getNoten() {
        Element lists = page.select("ul").get(0);
        Elements noten = lists.select("li");
        Elements daten = noten.select("li > span");
        Elements namen = noten.select("li > p");

        String[] notenresult = namen.html().split("\n");
        String[] datenS = daten.html().split("\n");

        finallyshown_noten = new ArrayList<String>();
        for (int i = 0; i<notenresult.length; i++) {
            //System.out.println(notenresult[i]);
            String temp = datenS[i]+"\n"+notenresult[i].replace("<br>", "\n");
            temp = temp.substring(0, temp.length()-1);
            finallyshown_noten.add(temp);
        }
        /*
        finallyshown_noten.add("01.01.2019\nChemie\nSäure-Base-Reaktionen\n6");
        finallyshown_noten.add("01.01.2019\nBiologie\nNeurobiologie\n6");
        finallyshown_noten.add("01.01.2019\nFranzösisch\nGérondif\n2");
        finallyshown_noten.add("01.01.2019\nDeutsch\nKommaprüfung\n6");
        finallyshown_noten.add("01.01.2019\nMathematik\nKomplexe Integrale\n6.5");
        */
        FileOutputStream fos = null;
        try {
            fos = openFileOutput("schulnetz-cache-noten.txt", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(finallyshown_noten);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (noten.get(0).text().equals("Sie haben keine unbestätigten Noten.")) {
            finallyshown_noten.add(0, "Du hast keine unbestätigten Noten");
        }




    }

    public void getAbsenzen() {
        //Element absenzenMeldungenListe = page.select("ul").get(2);
        Element absenzenListe = page.select("ul").get(1);
        Elements absenzenLis = absenzenListe.select("li");
        finallyshown_absenzen = new ArrayList<>();
        Log.e("test", absenzenLis.get(0).text());
        if (!absenzenLis.get(0).text().equals("Sie haben keine offenen Absenzen.")) {
            for (Element li : absenzenLis)  {
                finallyshown_absenzen.add(li.select("span").text()+"\n"+li.select("p").text());
            }
        }
        else {
            finallyshown_absenzen.add("Du hast keine offenen Absenzen.");
        }
        //AbsenzenMeldungen
        //BRAUCHE HTML!
    }



    //THREE DOTS MENU
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                // Do whatever you want to do on logout click.
                AlertDialog.Builder altbuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog));
                altbuilder.setTitle("Wirklich ausloggen?");
                altbuilder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                        finish();
                    }
                });
                altbuilder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = altbuilder.create();
                dialog.show();
                return true;

            case R.id.menu_einstellungen:
                //startActivity(new Intent("ch.meineKanti.mobile.einstellungenActivity"));
                Snackbar snack_einstellungen = Snackbar.make(findViewById(R.id.container), "Einstellungen sind noch nicht verfügbar", Snackbar.LENGTH_LONG);
                snack_einstellungen.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void logout() {
        SharedPreferences shpf = getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);
        SharedPreferences.Editor edit = shpf.edit();
        edit.clear();
        edit.apply();
        finish();
    }

}