package ch.meineKanti.mobile;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.Result;

import static android.content.Context.MODE_PRIVATE;

public class backgroundWorker extends Worker {



    String schulnetzURL;
    String pin;
    ArrayList<String> array_noten;
    ArrayList<String> array_noten_alt;
    ArrayList<String> array_agenda;
    ArrayList<String> array_starttimes;
    ArrayList<String> array_absenzen;
    ArrayList<String> array_absenzen_alt;
    ArrayList<String> array_absenzen_neu;
    ArrayList<String> array_noten_neu;
    String[] colors;
    HashMap<String,String> timeends;
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("schulnetz-cachefile", MODE_PRIVATE);


    Document page;
    public backgroundWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("BACKGROUNDWORKER", "WORK STARTED");

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

        schulnetzURL = prefs.getString("schulnetzURL", "");
        pin = prefs.getString("rememberme-pin", "");
        if (!refresh()) {
            Log.e("FAILURE", "BACKGROUNDWORKER NO CONNECTION");
            return Result.failure();
        }

        //Noten

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileInputStream fis;
                try {
                    fis = getApplicationContext().openFileInput("schulnetz-cache-noten.txt");
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    array_noten_alt = (ArrayList<String>) ois.readObject();
                    ois.close();

                    getNoten();
                    array_noten_neu = neueNoten(array_noten_alt, array_noten);
                    Intent intent = new Intent(getApplicationContext(), showgrades.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                    if (array_noten_neu.size() == 1) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "schulnetz-noten")
                                .setSmallIcon(R.drawable.ic_meinekanti_icon_grey)
                                .setContentTitle("Neue Note")
                                .setContentText(array_noten_neu.get(0).split("\n")[1]+" | "+array_noten_neu.get(0).split("\n")[3])
                                .setStyle(new NotificationCompat.BigTextStyle())
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);
                        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                        notificationManagerCompat.notify(8881, builder.build());

                    }
                    else if (array_noten_neu.size() > 1) {
                        int anzneuenoten = array_noten_neu.size();
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "schulnetz-noten")
                                .setSmallIcon(R.drawable.ic_meinekanti_icon_grey)
                                .setContentTitle("Neue Noten")
                                .setContentText("Du hast "+anzneuenoten+" neue Noten")
                                .setStyle(new NotificationCompat.BigTextStyle())
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);
                        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                        notificationManagerCompat.notify(8881, builder.build());
                    }

                    try {
                        FileOutputStream fos = getApplicationContext().openFileOutput("schulnetz-cache-noten.txt", MODE_PRIVATE);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(array_noten);
                        oos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        //Agenda
        new Thread(new Runnable() {
            @Override
            public void run() {
                getStundenplan();
                //TODO:FINISH THIS AUSFALLE
                //Ausfallende Lektionen

                int currentdayofweek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                int lastdayofweek = prefs.getInt("lastdayofweek", currentdayofweek);
                ArrayList<String> array_agenda_ausfallend_file;

                // Read from file
                try {
                    FileInputStream fis = getApplicationContext().openFileInput("schulnetz-cache-ausfaelle.txt");
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    array_agenda_ausfallend_file = (ArrayList<String>) ois.readObject();
                }
                catch (Exception e) {
                    //Create file if it doesn't exist
                    e.printStackTrace();
                    array_agenda_ausfallend_file = new ArrayList<>();
                    FileOutputStream fos = null;
                    try {
                        fos = getApplicationContext().openFileOutput("schulnetz-cache-ausfaelle.txt", MODE_PRIVATE);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(new ArrayList<String>());
                        oos.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                }

                if (currentdayofweek != lastdayofweek) {
                    //Reset file if day has changed and update last day
                    try {
                        FileOutputStream fos = getApplicationContext().openFileOutput("schulnetz-cache-ausfaelle.txt", MODE_PRIVATE);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(new ArrayList<String>());
                        oos.close();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("lastdayofweek", currentdayofweek);
                        editor.apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


                ArrayList<String> array_agenda_ausfallend = new ArrayList<>();
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i].equals("fb8c60")) {
                        array_agenda_ausfallend.add(array_agenda.get(i));
                        Log.e("AUSFALLE", array_agenda_ausfallend.size()+"");
                    }
                }

                for (String ausfall : array_agenda_ausfallend) {
                    if (!array_agenda_ausfallend_file.contains(ausfall)) {
                        try {
                            FileOutputStream fos = getApplicationContext().openFileOutput("schulnetz-cache-ausfaelle.txt", MODE_PRIVATE);
                            ObjectOutputStream oos = new ObjectOutputStream(fos);
                            oos.writeObject(array_agenda_ausfallend);
                            oos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent(getApplicationContext(), showgrades.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "schulnetz-ausfälle")
                                .setSmallIcon(R.drawable.ic_meinekanti_icon_grey)
                                .setContentTitle("Ausfallende Lektionen")
                                .setContentText(ausfall.split("\n")[0]+" | "+ausfall.split("\n")[1])
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);
                        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                        notificationManagerCompat.notify(8883, builder.build());//TODO:8883 or what?
                        break;
                    }
                }



                //Aktuelle Lektion
                if (array_agenda.size() > 0) {
                    //TODO: Stunden, die nicht stattfinden sollen natürlich nicht benachrichtigt werden!
                    if (!array_agenda.get(0).equals("Du hast heute keine Stunden.")) {
                        String notifictext_agenda = "";
                        long next_lesson_seconds = 0;
                        boolean nextlessonexists = false;
                        Calendar c = Calendar.getInstance();
                        long now = c.getTimeInMillis();
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        long passed = now - c.getTimeInMillis();
                        long current_time_seconds = passed / 1000;
                        for (String stundei : array_agenda) {
                            if (colors[array_agenda.indexOf(stundei)] != "fb8c60") {
                                String temp = stundei.split("\n")[0];
                                //TODO:See if this works, because of lessons without an ending time
                                String stunde;
                                try {
                                    stunde = temp.substring(0, temp.indexOf(" "));
                                } catch (StringIndexOutOfBoundsException e) {
                                    stunde = temp;
                                }
                                Calendar cal = Calendar.getInstance();
                                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(stunde.split(":")[0]));
                                cal.set(Calendar.MINUTE, Integer.parseInt(stunde.split(":")[1]));
                                cal.set(Calendar.MILLISECOND, 0);
                                cal.set(Calendar.SECOND, 0);
                                long agenda_time_seconds = (cal.getTimeInMillis() - c.getTimeInMillis()) / 1000;
                                if (current_time_seconds < agenda_time_seconds) {
                                    Log.e("NÄCHSTE LEKTION", stunde);
                                    String tmp = stundei.split("\n")[0];
                                    notifictext_agenda = stunde + " | " + stundei.split("\n")[1] + " | " + stundei.split("\n")[2];
                                    next_lesson_seconds = agenda_time_seconds;
                                    long time_until_notification = next_lesson_seconds - current_time_seconds - 600;
                                    long time_until_lektion = next_lesson_seconds - current_time_seconds;
                                    Log.e("STATUS", "LESSON STARTS IN " + time_until_lektion / 60 + " min");
                                    Log.e("STATUS", "NOTIFICATION SHOWN IN " + time_until_notification / 60 + " MIN");
                                    if (time_until_notification < 15 * 60) {
                                        if (time_until_notification < 0) {
                                            Intent intent = new Intent(getApplicationContext(), showgrades.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

                                            Log.e("STATUS", "NOTIFICATION SHOWN INSTANTLY: " + notifictext_agenda);
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "schulnetz-agenda")
                                                    .setSmallIcon(R.drawable.ic_meinekanti_icon_grey)
                                                    .setContentTitle("Nächste Lektion")
                                                    .setContentText(notifictext_agenda)
                                                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notifictext_agenda))
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                    .setContentIntent(pendingIntent)
                                                    .setAutoCancel(true)
                                                    .setTimeoutAfter(time_until_lektion * 1000);
                                            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                                            notificationManagerCompat.notify(8880, builder.build());
                                            Log.e("STATUS", "INSTANT NOTIFICATION SHOWN, ENDS IN " + time_until_lektion / 60 + " MIN");


                                        } else {
                                            Log.e("WERDEN BENACHR.", notifictext_agenda);
                                            Log.e("In MIN", time_until_notification / 60 + "");

                                            Data inputData = new Data.Builder()
                                                    .putString("notification_message", notifictext_agenda)
                                                    .build();
                                            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(notificationWorker.class)
                                                    .setInitialDelay(time_until_notification, TimeUnit.SECONDS)
                                                    .setInputData(inputData)
                                                    .build();
                                            WorkManager.getInstance().enqueue(workRequest);
                                        }
                                    }
                                    break;
                                }
                            }

                            }

                    }
                }
            }
        }).start();


        //Absenzen
        new Thread(new Runnable() {
            @Override
            public void run() {
                getAbsenzen();
                try {
                    FileInputStream fis = getApplicationContext().openFileInput("schulnetz-cache-absenzen.txt");
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    array_absenzen_alt = (ArrayList<String>) ois.readObject();
                    ois.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    array_absenzen_alt = array_absenzen;
                }
                array_absenzen_neu = makeArray_absenzen_neu(array_absenzen_alt, array_absenzen);
                if (array_absenzen_neu.size() > 0) {
                    if (!array_absenzen_neu.contains("Du hast keine neuen Absenzen.")) {
                        //Notify
                        Intent intent = new Intent(getApplicationContext(), showgrades.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "schulnetz-absenzen")
                                .setSmallIcon(R.drawable.ic_meinekanti_icon_grey)
                                .setContentTitle("Neue Absenz")
                                .setContentText("Du hast neue Absenzen.")
                                .setStyle(new NotificationCompat.BigTextStyle())
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);
                        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                        notificationManagerCompat.notify(8882, builder.build());
                    }
                }
                try {
                    FileOutputStream fos = getApplicationContext().openFileOutput("schulnetz-cache-absenzen.txt", MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(array_absenzen);
                    oos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Log.e("BACKGROUNDWORKERR", "WORK DONEE");
        return Result.success();
    }

    public boolean refresh() {
        try {
            page = Jsoup.connect(schulnetzURL)
                    .data("pin", pin, "pc", "1")
                    .post();
            Log.w("STATUS", "CONNECTION SUCCESSFUL");
            //page = Jsoup.parse(pagetemp);
            return true;
        }
        catch (IOException e) {
            Log.e("ERROR", "CONNECTION ERROR");
            return false;
            //Toast.makeText(getApplicationContext(), "Überprüfe deine Internetverbindung.", Toast.LENGTH_LONG).show();
        }
    }
    public void getNoten() {
        Element lists = page.select("ul").get(0);
        Elements noten = lists.select("li");
        Elements daten = noten.select("li > span");
        Elements namen = noten.select("li > p");

        String[] notenresult = namen.html().split("\n");
        String[] datenS = daten.html().split("\n");

        array_noten = new ArrayList<String>();
        for (int i = 0; i<notenresult.length; i++) {
            //System.out.println(notenresult[i]);
            String temp = datenS[i]+"\n"+notenresult[i].replace("<br>", "\n");
            temp = temp.substring(0, temp.length()-1);
            array_noten.add(temp);        }
    }

    public ArrayList<String> neueNoten(ArrayList<String> alteNoten, ArrayList<String> neueNoten) {
        ArrayList<String> out = new ArrayList<>();
        for (int i = 0; i < neueNoten.size(); i++) {
            if (!alteNoten.contains(neueNoten.get(i))) {
                out.add(neueNoten.get(i));
            }
        }
        return out;
    }

    public void getStundenplan() {
        Elements agendaUl = page.select("li[style]");
        // Deal with colors / Missing lessons or abnormalities
        colors = new String[agendaUl.size()];
        int n = 0;
        for (Element elem : agendaUl) {
            colors[n] = elem.attr("style").substring(19, elem.attr("style").indexOf(";"));
            n++;
        }
        String[] agenda = new String[agendaUl.size()];
        String[] rooms = new String[agendaUl.size()];
        String[] times = new String[agendaUl.size()];
        n = 0;
        String[] templ;
        array_starttimes = new ArrayList<>();
        for (Element elem : agendaUl) {
            templ = elem.html().replace("&nbsp", "").substring(1).split(";;;");
            agenda[n] = templ[2];
            times[n] = templ[0];
            array_starttimes.add(templ[0]);
            rooms[n] = templ[1];
            n++;
        }
        array_agenda = new ArrayList<>();
        for (int i = 0; i < agenda.length; i++) {
            if (timeends.get(times[i]) != null) {
                array_agenda.add(times[i]+" - "+timeends.get(times[i])+"\n"+agenda[i]+"\n"+rooms[i]);
            }
            else {
                array_agenda.add(times[i]+"\n"+agenda[i]+"\n"+rooms[i]);
            }
        }
        if (array_agenda.size() == 0) {
            array_agenda.add("Du hast heute keine Stunden.");
        }
    }

    public void getAbsenzen() {
        //Element absenzenMeldungenListe = page.select("ul").get(2);
        Element absenzenListe = page.select("ul").get(1);
        Elements absenzenLis = absenzenListe.select("li");
        array_absenzen = new ArrayList<>();

        if (!absenzenLis.get(0).text().equals("Sie haben keine offenen Absenzen.")) {
            for (Element li : absenzenLis)  {
                array_absenzen.add(li.select("span").text()+"\n"+li.select("p").text());
            }
        }
        else {
            array_absenzen.add("Du hast keine neuen Absenzen.");
        }
        //AbsenzenMeldungen
        //BRAUCHE HTML!
    }

    public ArrayList<String> makeArray_absenzen_neu(ArrayList<String> alt, ArrayList<String> neu) {
        ArrayList<String> out = new ArrayList<>();
        for (String i : neu) {
            if (!alt.contains(i)) {
                out.add(i);
            }
        }
        return out;
    }
}
