package com.example.it3c_grp10_navarrosa;

import android.app.DatePickerDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.TimePickerDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.text.Editable;
import android.text.TextWatcher;

public class LogCatchActivity extends AppCompatActivity {
    private EditText editFishType, editQuantity, editAmountSold, editNotes, editDate;
    private Button btnSave, btnCancel;
    private RecyclerView recyclerView;
    private LogEntryAdapter adapter;
    private ArrayList<CatchRecord> recordsList;
    private SharedPreferences prefs;
    private Gson gson;
    private ArrayList<FeedingSchedule> feedingList = new ArrayList<>();
    private int editingFeedingIndex = -1;
    private LinearLayout feedingListContainer;
    private Spinner typeSelector;
    private EditText searchBar;
    private ArrayList<CatchRecord> filteredList;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.logbook_module);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
            gson = new Gson();

            editFishType = findViewById(R.id.editFishType);
            editQuantity = findViewById(R.id.editQuantity);
            editAmountSold = findViewById(R.id.editAmountSold);
            editNotes = findViewById(R.id.editNotes);
            editDate = findViewById(R.id.editDate);
            btnSave = findViewById(R.id.btnSave);
            btnCancel = findViewById(R.id.btnCancel);
            recyclerView = findViewById(R.id.log_recycler_view);
            feedingListContainer = findViewById(R.id.feeding_list_container);
            typeSelector = findViewById(R.id.type_selector);
            searchBar = findViewById(R.id.search_bar);

            setupTypeSelector();
            setupSearchBar();
            setupRecyclerView();
            loadRecords();
            setupDatePicker();
            loadFeedingSchedules();
            renderFeedingScheduleCards();

            EditText feedingDate = findViewById(R.id.feeding_date);
            EditText feedingTime = findViewById(R.id.feeding_time);
            feedingDate.setOnClickListener(v -> showFeedingDatePicker(feedingDate));
            feedingTime.setOnClickListener(v -> showFeedingTimePicker(feedingTime));

            btnCancel.setOnClickListener(v -> finish());
            btnSave.setOnClickListener(v -> saveRecord());
            findViewById(R.id.btnAddFeeding).setOnClickListener(v -> addFeedingSchedule());
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
        filterAndSearch();
        adapter.notifyDataSetChanged();
        loadFeedingSchedules();
        renderFeedingScheduleCards();
    }

    private void setupTypeSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"All", "Catch", "Sale", "Note"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSelector.setAdapter(adapter);
        typeSelector.setSelection(0);
        typeSelector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentFilter = (String) parent.getItemAtPosition(position);
                filterAndSearch();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupSearchBar() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndSearch();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterAndSearch() {
        String query = searchBar.getText().toString().toLowerCase();
        filteredList = new ArrayList<>();
        if (currentFilter.equals("Sale")) {
            List<CatchRecord> sales = new ArrayList<>();
            for (CatchRecord record : recordsList) {
                if (record.type == CatchRecord.EntryType.SALE) {
                    sales.add(record);
                }
            }
            sales.sort((a, b) -> b.date.compareTo(a.date));
            int count = 0;
            for (CatchRecord sale : sales) {
                if ((sale.title.toLowerCase().contains(query) || sale.description.toLowerCase().contains(query)) && count < 2) {
                    filteredList.add(sale);
                    count++;
                }
            }
        } else {
            for (CatchRecord record : recordsList) {
                boolean matchesType = currentFilter.equals("All") || record.type.toString().equalsIgnoreCase(currentFilter);
                boolean matchesQuery = record.title.toLowerCase().contains(query) || record.description.toLowerCase().contains(query);
                if (matchesType && matchesQuery) {
                    filteredList.add(record);
                }
            }
        }
        adapter.updateList(filteredList);
    }

    private void setupRecyclerView() {
        recordsList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new LogEntryAdapter(filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new LogEntryAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(int position) {
                CatchRecord record = filteredList.get(position);
                if (record.type == CatchRecord.EntryType.SALE) {
                    showEditSaleDialog(position);
                } else {
                    Toast.makeText(LogCatchActivity.this, "Edit not yet implemented for this type.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onDeleteClick(int position) {
                int realPos = recordsList.indexOf(filteredList.get(position));
                recordsList.remove(realPos);
                filterAndSearch();
                saveRecordsToPrefs();
            }
        });
    }

    private void loadRecords() {
        String json = prefs.getString("records_" + getUserUid(), null);
        Type type = new TypeToken<ArrayList<CatchRecord>>() {}.getType();
        if (json != null) {
            recordsList.clear();
            recordsList.addAll(gson.fromJson(json, type));
            filterAndSearch();
        }
    }

    private void setupDatePicker() {
        Calendar calendar = Calendar.getInstance();
        updateDateInView(calendar);

        editDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, month, dayOfMonth);
                        updateDateInView(newDate);
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void updateDateInView(Calendar calendar) {
        editDate.setText(String.format("%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)));
    }
    
    private void saveRecord() {
        String fishType = editFishType.getText().toString();
        String quantityStr = editQuantity.getText().toString();
        String amountStr = editAmountSold.getText().toString();
        String notes = editNotes.getText().toString();
        String date = editDate.getText().toString();
        String selectedType = typeSelector.getSelectedItem().toString();

        CatchRecord.EntryType type;
        String title;
        String description;

        boolean isSale = !amountStr.isEmpty() && Double.parseDouble(amountStr) > 0;

        if (selectedType.equals("Note")) {
            type = CatchRecord.EntryType.NOTE;
            title = "Note";
            description = notes;
        } else if (isSale) {
            type = CatchRecord.EntryType.SALE;
            title = "Sold " + fishType;
            description = "Sold " + quantityStr + " for P" + amountStr;
        } else {
            type = CatchRecord.EntryType.CATCH;
            title = "Caught " + fishType;
            description = "Caught " + quantityStr;
        }

        if(!notes.isEmpty() && !selectedType.equals("Note")){
            description += " (" + notes + ")";
        }

        CatchRecord record = new CatchRecord(type, title, description, date);
        record.fishType = fishType;
        record.quantity = quantityStr.isEmpty() ? 0 : Integer.parseInt(quantityStr);
        record.amountSold = amountStr.isEmpty() ? 0 : Double.parseDouble(amountStr);
        record.notes = notes;

        recordsList.add(0, record);
        filterAndSearch();
        adapter.notifyItemInserted(0);
        recyclerView.scrollToPosition(0);
        saveRecordsToPrefs();
        updateInventory(fishType, record.quantity, isSale);
        clearInputFields();
    }

    private void updateInventory(String fishType, int quantity, boolean isSale) {
        String key = "inventory_" + getUserUid();
        Gson gson = new Gson();
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.HashMap<String, Integer>>(){}.getType();
        String json = prefs.getString(key, null);
        java.util.HashMap<String, Integer> inventoryMap;
        if (json != null) {
            inventoryMap = gson.fromJson(json, type);
        } else {
            inventoryMap = new java.util.HashMap<>();
        }
        int current = inventoryMap.getOrDefault(fishType, 0);
        if (isSale) {
            current -= quantity;
            if (current < 0) current = 0;
        } else {
            current += quantity;
        }
        inventoryMap.put(fishType, current);
        prefs.edit().putString(key, gson.toJson(inventoryMap)).apply();
    }

    private void saveRecordsToPrefs() {
        String json = gson.toJson(recordsList);
        prefs.edit().putString("records_" + getUserUid(), json).apply();
    }

    private void loadFeedingSchedules() {
        String json = prefs.getString("feeding_schedules_" + getUserUid(), null);
        Type type = new TypeToken<ArrayList<FeedingSchedule>>() {}.getType();
        feedingList.clear();
        if (json != null) {
            feedingList.addAll(gson.fromJson(json, type));
        }
    }

    private void renderFeedingScheduleCards() {
        feedingListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < feedingList.size(); i++) {
            FeedingSchedule fs = feedingList.get(i);
            View card = inflater.inflate(R.layout.item_feeding_schedule, feedingListContainer, false);
            ((TextView) card.findViewById(R.id.text_feeding_date)).setText(fs.date);
            ((TextView) card.findViewById(R.id.text_feeding_time)).setText(fs.time);
            ((TextView) card.findViewById(R.id.text_feeding_notes)).setText(fs.notes);
            int index = i;
            card.findViewById(R.id.button_edit_feeding).setOnClickListener(v -> {
                ((EditText) findViewById(R.id.feeding_date)).setText(fs.date);
                ((EditText) findViewById(R.id.feeding_time)).setText(fs.time);
                ((EditText) findViewById(R.id.feeding_notes)).setText(fs.notes);
                editingFeedingIndex = index;
                Toast.makeText(LogCatchActivity.this, "Edit the fields and press 'Add Feeding Schedule' to update.", Toast.LENGTH_SHORT).show();
            });
            card.findViewById(R.id.button_delete_feeding).setOnClickListener(v -> {
                feedingList.remove(index);
                saveFeedingSchedulesToPrefs();
                renderFeedingScheduleCards();
                Toast.makeText(LogCatchActivity.this, "Feeding schedule deleted.", Toast.LENGTH_SHORT).show();
            });
            feedingListContainer.addView(card);
        }
    }

    private void addFeedingSchedule() {
        EditText dateInput = findViewById(R.id.feeding_date);
        EditText timeInput = findViewById(R.id.feeding_time);
        EditText notesInput = findViewById(R.id.feeding_notes);
        String date = dateInput.getText().toString();
        String time = timeInput.getText().toString();
        String notes = notesInput.getText().toString();
        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please enter both date and time.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            FeedingSchedule schedule = new FeedingSchedule(date, time, notes);
            if (editingFeedingIndex >= 0) {
                feedingList.set(editingFeedingIndex, schedule);
                editingFeedingIndex = -1;
                Toast.makeText(this, "Feeding schedule updated!", Toast.LENGTH_SHORT).show();
            } else {
                feedingList.add(0, schedule);
                Toast.makeText(this, "Feeding schedule added!", Toast.LENGTH_SHORT).show();
            }
            saveFeedingSchedulesToPrefs();
            renderFeedingScheduleCards();
            scheduleFeedingNotification(date, time, notes);
            dateInput.setText("");
            timeInput.setText("");
            notesInput.setText("");
        } catch (Exception e) {
            Toast.makeText(this, "Error adding feeding schedule: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleFeedingNotification(String date, String time, String notes) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            long triggerAtMillis = sdf.parse(date + " " + time).getTime();
            if (triggerAtMillis < System.currentTimeMillis()) {
                Toast.makeText(this, "Cannot set a reminder in the past.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FeedingNotificationReceiver.class);
            intent.putExtra("notes", notes);
            intent.putExtra("time", time);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) triggerAtMillis, intent, flags);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Toast.makeText(this, "Feeding reminder set!", Toast.LENGTH_SHORT).show();
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date/time format.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Removed Toast to prevent user-facing error popup
            // Log.e("FeedingNotification", "Error scheduling notification", e);
        }
    }

    private void saveFeedingSchedulesToPrefs() {
        String json = gson.toJson(feedingList);
        prefs.edit().putString("feeding_schedules_" + getUserUid(), json).apply();
    }

    private void clearInputFields() {
        editFishType.setText("");
        editQuantity.setText("");
        editAmountSold.setText("");
        editNotes.setText("");
    }

    private void showFeedingDatePicker(EditText dateField) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    dateField.setText(dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showFeedingTimePicker(EditText timeField) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    timeField.setText(timeStr);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private String getUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "default";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showEditSaleDialog(int position) {
        CatchRecord record = filteredList.get(position);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_inventory, null);
        EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);
        editQuantity.setText(String.valueOf(record.quantity));
        TextView fishTypeView = new TextView(this);
        fishTypeView.setText("Fish Type: " + record.fishType);
        fishTypeView.setPadding(0, 0, 0, 8);
        TextView amountView = new TextView(this);
        amountView.setText("Total Amount: P" + record.amountSold);
        amountView.setPadding(0, 0, 0, 8);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(fishTypeView);
        layout.addView(amountView);
        layout.addView(editQuantity);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Sale Quantity")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String qtyStr = editQuantity.getText().toString();
                        if (qtyStr.isEmpty()) {
                            Toast.makeText(this, "Quantity cannot be empty.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int newQty = Integer.parseInt(qtyStr);
                        int realPos = recordsList.indexOf(record);
                        record.quantity = newQty;
                        record.description = "Sold " + newQty + " for P" + record.amountSold + (record.notes != null && !record.notes.isEmpty() ? " (" + record.notes + ")" : "");
                        recordsList.set(realPos, record);
                        saveRecordsToPrefs();
                        filterAndSearch();
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid input: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
} 