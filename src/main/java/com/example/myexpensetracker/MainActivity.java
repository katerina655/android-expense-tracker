package com.example.myexpensetracker;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.text.InputType;

import org.json.JSONObject;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText expenseInput;
    private Spinner categorySpinner;
    private TextView todayTotalText;
    private SharedPreferences prefs;
    private static final String PREF_NAME = "expense_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expenseInput = findViewById(R.id.expenseInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        todayTotalText = findViewById(R.id.todayTotalText);
        Button saveButton = findViewById(R.id.saveButton);
        Button statsButton = findViewById(R.id.buttonStats);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // εδώ φοτρώνουμε τις κατηγοριες από το strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.categories, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        //  μπορούμε να προσθέσουμε και νέα κατηγορία
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                if (selected.equals("+ Προσθήκη νέας...")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Προσθήκη νέας κατηγορίας");

                    final EditText input = new EditText(MainActivity.this);
                    input.setHint("Π.χ. Καφές, Δώρα...");
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    builder.setPositiveButton("Προσθήκη", (dialog, which) -> {
                        String newCategory = input.getText().toString().trim();
                        if (!newCategory.isEmpty()) {
                            // προσθήκη στη λίστα του Spinner
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) categorySpinner.getAdapter();
                            adapter.insert(newCategory, adapter.getCount() - 1);
                            adapter.notifyDataSetChanged();
                            categorySpinner.setSelection(adapter.getPosition(newCategory));
                            Toast.makeText(MainActivity.this, "Η κατηγορία προστέθηκε!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Δεν έδωσες όνομα κατηγορίας", Toast.LENGTH_SHORT).show();
                            categorySpinner.setSelection(0);
                        }
                    });

                    builder.setNegativeButton("Άκυρο", (dialog, which) -> {
                        dialog.dismiss();
                        categorySpinner.setSelection(0); // Επιστροφή στην αρχική επιλογή
                    });

                    builder.show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateTodayTotal();

        // Κουμπί αποθήκευσης
        saveButton.setOnClickListener(v -> saveExpense());

        // Κουμπί για να πάμε στη σελίδα στατιστικών
        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });
    }

    private void saveExpense() {
        String inputText = expenseInput.getText().toString().trim();
        if (inputText.isEmpty()) {
            Toast.makeText(this, "Βάλε ποσό πρώτα!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(inputText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Μη έγκυρο ποσό", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = categorySpinner.getSelectedItem().toString();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        try {
            String jsonString = prefs.getString("expenses", "{}");
            JSONObject allData = new JSONObject(jsonString);

            JSONObject dayData = allData.optJSONObject(today);
            if (dayData == null) dayData = new JSONObject();

            double current = dayData.optDouble(category, 0);
            dayData.put(category, current + amount);

            allData.put(today, dayData);
            prefs.edit().putString("expenses", allData.toString()).apply();

            expenseInput.setText("");
            Toast.makeText(this, "Αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
            updateTodayTotal();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Σφάλμα αποθήκευσης", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTodayTotal() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String jsonString = prefs.getString("expenses", "{}");

        try {
            JSONObject allData = new JSONObject(jsonString);
            JSONObject dayData = allData.optJSONObject(today);
            double total = 0;

            if (dayData != null) {
                Iterator<String> keys = dayData.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    total += dayData.optDouble(k, 0);
                }
            }
            todayTotalText.setText("Σύνολο σημερινών εξόδων: " + total + "€");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
