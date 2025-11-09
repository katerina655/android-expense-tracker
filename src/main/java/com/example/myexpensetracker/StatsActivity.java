package com.example.myexpensetracker;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class StatsActivity extends AppCompatActivity {

    private TextView summaryTextView;
    private ListView listViewDays;
    private LinearLayout listCard;
    private static final String PREF_NAME = "expense_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        summaryTextView = findViewById(R.id.textViewSummary);
        listViewDays = findViewById(R.id.listViewDays);
        listCard = findViewById(R.id.listCard);
        Button backButton = findViewById(R.id.backButton);
        Button buttonDay = findViewById(R.id.buttonDayStats);
        Button buttonWeek = findViewById(R.id.buttonWeekStats);

        backButton.setOnClickListener(v -> finish());

        // επιλογή: Ανά ημέρα
        loadStatistics("day");
        listCard.setVisibility(View.VISIBLE);

        // εναλλαγή λειτουργιών
        buttonDay.setOnClickListener(v -> {
            loadStatistics("day");
            listCard.setVisibility(View.VISIBLE);
        });

        buttonWeek.setOnClickListener(v -> {
            loadStatistics("week");
            listCard.setVisibility(View.GONE);
        });
    }

    private void loadStatistics(String mode) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String jsonString = prefs.getString("expenses", "{}");

        try {
            JSONObject allData = new JSONObject(jsonString);
            if (allData.length() == 0) {
                summaryTextView.setText("Δεν έχεις καταχωρήσει έξοδα ακόμα.");
                return;
            }

            List<String> allDates = new ArrayList<>();
            Map<String, JSONObject> dailyData = new HashMap<>();
            Map<String, Double> dailyTotals = new HashMap<>();
            Map<String, Double> weeklyTotals = new HashMap<>();
            Map<String, String[]> weekDateRanges = new HashMap<>();

            double grandTotal = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            for (Iterator<String> it = allData.keys(); it.hasNext();) {
                String day = it.next();
                JSONObject dayObj = allData.getJSONObject(day);
                allDates.add(day);
                dailyData.put(day, dayObj);

                double totalForDay = 0;
                Iterator<String> cats = dayObj.keys();
                while (cats.hasNext()) {
                    String cat = cats.next();
                    totalForDay += dayObj.getDouble(cat);
                }

                // υπολογισμός εβδομάδας
                Date d = sdf.parse(day);
                cal.setTime(d);
                int week = cal.get(Calendar.WEEK_OF_YEAR);
                int year = cal.get(Calendar.YEAR);
                String weekKey = year + "-W" + week;
                weeklyTotals.put(weekKey, weeklyTotals.getOrDefault(weekKey, 0.0) + totalForDay);

                if (!weekDateRanges.containsKey(weekKey)) {
                    weekDateRanges.put(weekKey, new String[]{day, day});
                } else {
                    String[] range = weekDateRanges.get(weekKey);
                    if (day.compareTo(range[0]) < 0) range[0] = day;
                    if (day.compareTo(range[1]) > 0) range[1] = day;
                }

                dailyTotals.put(day, totalForDay);
                grandTotal += totalForDay;
            }

            Collections.sort(allDates);
            double avgPerDay = grandTotal / allDates.size();
            double avgPerWeek = grandTotal / weeklyTotals.size();

            //  max/min ημέρας
            String maxDay = "", minDay = "";
            double maxDayAmount = Double.MIN_VALUE, minDayAmount = Double.MAX_VALUE;
            for (Map.Entry<String, Double> entry : dailyTotals.entrySet()) {
                if (entry.getValue() > maxDayAmount) {
                    maxDayAmount = entry.getValue();
                    maxDay = entry.getKey();
                }
                if (entry.getValue() < minDayAmount) {
                    minDayAmount = entry.getValue();
                    minDay = entry.getKey();
                }
            }

            //  max/min εβδομάδας
            String maxWeek = "", minWeek = "";
            double maxWeekAmount = Double.MIN_VALUE, minWeekAmount = Double.MAX_VALUE;
            for (Map.Entry<String, Double> entry : weeklyTotals.entrySet()) {
                if (entry.getValue() > maxWeekAmount) {
                    maxWeekAmount = entry.getValue();
                    maxWeek = entry.getKey();
                }
                if (entry.getValue() < minWeekAmount) {
                    minWeekAmount = entry.getValue();
                    minWeek = entry.getKey();
                }
            }

            StringBuilder summary = new StringBuilder();
            summary.append(" <b>Συνολικό ποσό εξόδων:</b> ")
                    .append(String.format(Locale.getDefault(), "%.2f€", grandTotal))
                    .append("<br><br>")
                    .append(" <b>Μέσος όρος ανά ημέρα:</b> ")
                    .append(String.format(Locale.getDefault(), "%.2f€", avgPerDay))
                    .append("<br>")
                    .append(" <b>Μέσος όρος ανά εβδομάδα:</b> ")
                    .append(String.format(Locale.getDefault(), "%.2f€", avgPerWeek))
                    .append("<br><br>");

            // Ανά Ημέρα
            if (mode.equals("day")) {
                summary.append(" <b>Περισσότερα έξοδα:</b> ")
                        .append(maxDay).append(" (").append(String.format("%.2f€", maxDayAmount)).append(")<br>")
                        .append("<b>Λιγότερα έξοδα:</b> ")
                        .append(minDay).append(" (").append(String.format("%.2f€", minDayAmount)).append(")");
            }

            // ➤ Ανά Εβδομάδα
            if (mode.equals("week")) {
                summary.append("🗓 <b>Εβδομάδα με περισσότερα έξοδα:</b> ")
                        .append(formatWeekRange(weekDateRanges.get(maxWeek)))
                        .append(" (").append(String.format("%.2f€", maxWeekAmount)).append(")<br>")
                        .append(" <b>Εβδομάδα με λιγότερα έξοδα:</b> ")
                        .append(formatWeekRange(weekDateRanges.get(minWeek)))
                        .append(" (").append(String.format("%.2f€", minWeekAmount)).append(")");
            }

            summaryTextView.setText(android.text.Html.fromHtml(summary.toString()));

            // εμφανίζουμε τη λίστα ημερών μόνο στη λειτουργία day
            if (mode.equals("day")) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allDates);
                listViewDays.setAdapter(adapter);

                listViewDays.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedDate = allDates.get(position);
                    showDayDetails(selectedDate, dailyData.get(selectedDate));
                });
            } else {
                listViewDays.setAdapter(null); // καθαρίζουμε τη λίστα
            }

        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            summaryTextView.setText("Σφάλμα ανάγνωσης δεδομένων.");
        }
    }

    private String formatWeekRange(String[] range) {
        if (range == null || range.length < 2) return "Άγνωστη εβδομάδα";
        return range[0] + " → " + range[1];
    }

    private void showDayDetails(String date, JSONObject dayData) {
        StringBuilder details = new StringBuilder();
        double total = 0;

        try {
            Iterator<String> cats = dayData.keys();
            while (cats.hasNext()) {
                String cat = cats.next();
                double value = dayData.getDouble(cat);

                String icon;
                switch (cat) {
                    case "Φαγητό": icon = "🍔"; break;
                    case "Διασκέδαση": icon = "🎉"; break;
                    case "Μετακινήσεις": icon = "🚗"; break;
                    case "Ρούχα": icon = "👕"; break;
                    default: icon = "💡"; break;
                }

                details.append(icon).append(" ").append(cat)
                        .append(": ").append(String.format(Locale.getDefault(), "%.2f€", value))
                        .append("\n");

                total += value;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        details.append("\n<b>Σύνολο ημέρας:</b> ").append(String.format(Locale.getDefault(), "%.2f€", total));

        new AlertDialog.Builder(this)
                .setTitle(" Έξοδα για " + date)
                .setMessage(android.text.Html.fromHtml(details.toString()))
                .setPositiveButton("OK", null)
                .show();
    }
}
