package com.example.it3c_grp10_navarrosa;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;

public class TrackingActivity extends AppCompatActivity {

    private BarChart barChart;
    private TableLayout salesTable;
    private List<CatchRecord> salesList = new ArrayList<>();
    private int editingSaleIndex = -1;
    private Spinner salesTypeSelector;
    private EditText salesSearchBar;
    private List<CatchRecord> filteredSalesList = new ArrayList<>();
    private String currentSalesFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        barChart = findViewById(R.id.sales_bar_chart);
        setupChart();

        salesTable = findViewById(R.id.sales_table);
        salesTypeSelector = findViewById(R.id.sales_type_selector);
        salesSearchBar = findViewById(R.id.sales_search_bar);

        setupSalesTypeSelector();
        setupSalesSearchBar();

        loadAndProcessData();
        loadSales();
        filterAndSearchSales();
        renderSalesTable();
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
    }

    private String getUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "default";
    }

    private void loadAndProcessData() {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String json = prefs.getString("records_" + getUserUid(), "[]");
        Type type = new TypeToken<ArrayList<CatchRecord>>(){}.getType();
        List<CatchRecord> records = new Gson().fromJson(json, type);

        Map<String, Float> salesByFishType = new HashMap<>();
        for (CatchRecord record : records) {
            if (record.type == CatchRecord.EntryType.SALE && record.fishType != null && !record.fishType.isEmpty()) {
                float currentSales = salesByFishType.getOrDefault(record.fishType, 0f);
                salesByFishType.put(record.fishType, currentSales + (float) record.amountSold);
            }
        }

        if (salesByFishType.isEmpty()) {
            barChart.setNoDataText("No sales data available.");
            barChart.invalidate();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Float> entry : salesByFishType.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        BarDataSet dataSet = new BarDataSet(entries, "Total Sales by Fish Type");
        dataSet.setColor(Color.parseColor("#80DEEA")); // aqua_blue
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.invalidate(); // refresh
    }

    private void loadSales() {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String json = prefs.getString("records_" + getUserUid(), "[]");
        Type type = new TypeToken<ArrayList<CatchRecord>>(){}.getType();
        List<CatchRecord> allRecords = new Gson().fromJson(json, type);
        salesList.clear();
        for (CatchRecord record : allRecords) {
            if (record.type == CatchRecord.EntryType.SALE) {
                salesList.add(record);
            }
        }
    }

    private void saveSalesToPrefs() {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String json = prefs.getString("records_" + getUserUid(), "[]");
        Type type = new TypeToken<ArrayList<CatchRecord>>(){}.getType();
        List<CatchRecord> allRecords = new Gson().fromJson(json, type);
        allRecords.add(0, salesList.get(0));
        prefs.edit().putString("records_" + getUserUid(), new Gson().toJson(allRecords)).apply();
    }

    private void setupSalesTypeSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getFishTypeOptions());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        salesTypeSelector.setAdapter(adapter);
        salesTypeSelector.setSelection(0);
        salesTypeSelector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSalesFilter = (String) parent.getItemAtPosition(position);
                filterAndSearchSales();
                renderSalesTable();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private List<String> getFishTypeOptions() {
        List<String> options = new ArrayList<>();
        options.add("All");
        for (CatchRecord sale : salesList) {
            if (sale.fishType != null && !sale.fishType.isEmpty() && !options.contains(sale.fishType)) {
                options.add(sale.fishType);
            }
        }
        return options;
    }

    private void setupSalesSearchBar() {
        salesSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndSearchSales();
                renderSalesTable();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterAndSearchSales() {
        String query = salesSearchBar.getText().toString().toLowerCase();
        filteredSalesList = new ArrayList<>();
        for (CatchRecord sale : salesList) {
            boolean matchesType = currentSalesFilter.equals("All") || (sale.fishType != null && sale.fishType.equalsIgnoreCase(currentSalesFilter));
            boolean matchesQuery = sale.title.toLowerCase().contains(query) || sale.description.toLowerCase().contains(query);
            if (matchesType && matchesQuery) {
                filteredSalesList.add(sale);
            }
        }
    }

    private void renderSalesTable() {
        salesTable.removeAllViews();
        TableRow header = new TableRow(this);
        String[] headers = {"Date", "Fish Type", "Qty", "Price", "Total", "Edit", "Delete"};
        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setPadding(8, 8, 8, 8);
            header.addView(tv);
        }
        salesTable.addView(header);
        for (int i = 0; i < filteredSalesList.size(); i++) {
            CatchRecord sale = filteredSalesList.get(i);
            TableRow row = new TableRow(this);
            row.addView(makeCell(sale.date));
            row.addView(makeCell(sale.fishType));
            row.addView(makeCell(String.valueOf(sale.quantity)));
            row.addView(makeCell(String.valueOf(sale.amountSold / sale.quantity)));
            row.addView(makeCell(String.valueOf(sale.amountSold)));
            final int index = salesList.indexOf(sale);
            // Edit icon
            ImageButton btnEdit = new ImageButton(this);
            btnEdit.setImageResource(android.R.drawable.ic_menu_edit);
            btnEdit.setBackgroundResource(android.R.color.transparent);
            btnEdit.setColorFilter(getResources().getColor(R.color.text_secondary));
            btnEdit.setLayoutParams(new TableRow.LayoutParams(48, 48));
            btnEdit.setOnClickListener(v -> showEditSaleDialog(index));
            row.addView(btnEdit);
            // Delete icon
            ImageButton btnDelete = new ImageButton(this);
            btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
            btnDelete.setBackgroundResource(android.R.color.transparent);
            btnDelete.setColorFilter(getResources().getColor(R.color.text_secondary));
            btnDelete.setLayoutParams(new TableRow.LayoutParams(48, 48));
            btnDelete.setOnClickListener(v -> confirmDeleteSale(index));
            row.addView(btnDelete);
            salesTable.addView(row);
        }
    }

    private TextView makeCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }

    private void showEditSaleDialog(int index) {
        CatchRecord sale = salesList.get(index);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_inventory, null);
        EditText editFishType = dialogView.findViewById(R.id.edit_fish_type);
        EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);
        editFishType.setText(sale.fishType);
        editFishType.setEnabled(false);
        editQuantity.setText(String.valueOf(sale.quantity));
        new AlertDialog.Builder(this)
                .setTitle("Edit Sale Quantity")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    int oldQty = sale.quantity;
                    int newQty = Integer.parseInt(editQuantity.getText().toString());
                    sale.quantity = newQty;
                    sale.amountSold = (sale.amountSold / oldQty) * newQty;
                    updateSaleInPrefs(index, sale);
                    updateInventoryOnEdit(sale.fishType, oldQty, newQty);
                    renderSalesTable();
                    loadAndProcessData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteSale(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Sale")
                .setMessage("Are you sure you want to delete this sale?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    CatchRecord sale = salesList.get(index);
                    updateInventoryOnDelete(sale.fishType, sale.quantity);
                    deleteSaleFromPrefs(index);
                    salesList.remove(index);
                    renderSalesTable();
                    loadAndProcessData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSaleInPrefs(int index, CatchRecord updatedSale) {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String json = prefs.getString("records_" + getUserUid(), "[]");
        Type type = new TypeToken<ArrayList<CatchRecord>>(){}.getType();
        List<CatchRecord> allRecords = new Gson().fromJson(json, type);
        int saleCount = 0;
        for (int i = 0; i < allRecords.size(); i++) {
            if (allRecords.get(i).type == CatchRecord.EntryType.SALE) {
                if (saleCount == index) {
                    allRecords.set(i, updatedSale);
                    break;
                }
                saleCount++;
            }
        }
        prefs.edit().putString("records_" + getUserUid(), new Gson().toJson(allRecords)).apply();
    }

    private void deleteSaleFromPrefs(int index) {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String json = prefs.getString("records_" + getUserUid(), "[]");
        Type type = new TypeToken<ArrayList<CatchRecord>>(){}.getType();
        List<CatchRecord> allRecords = new Gson().fromJson(json, type);
        int saleCount = 0;
        for (int i = 0; i < allRecords.size(); i++) {
            if (allRecords.get(i).type == CatchRecord.EntryType.SALE) {
                if (saleCount == index) {
                    allRecords.remove(i);
                    break;
                }
                saleCount++;
            }
        }
        prefs.edit().putString("records_" + getUserUid(), new Gson().toJson(allRecords)).apply();
    }

    private void updateInventoryOnEdit(String fishType, int oldQty, int newQty) {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
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
        current += oldQty; // revert old sale
        current -= newQty; // apply new sale
        if (current < 0) current = 0;
        inventoryMap.put(fishType, current);
        prefs.edit().putString(key, gson.toJson(inventoryMap)).apply();
    }

    private void updateInventoryOnDelete(String fishType, int qty) {
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
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
        current += qty; // revert sale
        inventoryMap.put(fishType, current);
        prefs.edit().putString(key, gson.toJson(inventoryMap)).apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 