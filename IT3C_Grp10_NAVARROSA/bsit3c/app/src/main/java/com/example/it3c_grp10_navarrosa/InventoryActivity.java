package com.example.it3c_grp10_navarrosa;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.ImageButton;
import androidx.annotation.NonNull;

public class InventoryActivity extends AppCompatActivity {
    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter inventoryAdapter;
    private List<Map.Entry<String, Integer>> inventoryEntries = new ArrayList<>();
    private SharedPreferences prefs;
    private Gson gson;
    private Map<String, Integer> inventoryMap = new HashMap<>();
    private static final int LOW_STOCK_THRESHOLD = 5;
    private String editingFishType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        inventoryRecyclerView = findViewById(R.id.inventory_recycler_view);
        inventoryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        gson = new Gson();

        loadInventory();
        inventoryAdapter = new InventoryAdapter();
        inventoryRecyclerView.setAdapter(inventoryAdapter);
        Button btnAdjust = findViewById(R.id.btn_adjust_inventory);
        btnAdjust.setOnClickListener(v -> showAdjustDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventory();
        inventoryAdapter.notifyDataSetChanged();
    }

    private String getUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "default";
    }

    private void loadInventory() {
        String json = prefs.getString("inventory_" + getUserUid(), null);
        Type type = new TypeToken<HashMap<String, Integer>>(){}.getType();
        if (json != null) {
            inventoryMap = gson.fromJson(json, type);
        } else {
            inventoryMap = new HashMap<>();
        }
        inventoryEntries = new ArrayList<>(inventoryMap.entrySet());
        if (inventoryAdapter != null) inventoryAdapter.notifyDataSetChanged();
    }

    private void saveInventory() {
        String json = gson.toJson(inventoryMap);
        prefs.edit().putString("inventory_" + getUserUid(), json).apply();
    }

    private void showAdjustDialog() {
        showAdjustDialog(null, 0);
    }

    private void showAdjustDialog(String fishTypePrefill, int qtyPrefill) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_adjust_inventory, null);
        EditText editFishType = dialogView.findViewById(R.id.edit_fish_type);
        EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);
        if (fishTypePrefill != null) {
            editFishType.setText(fishTypePrefill);
            editFishType.setEnabled(false);
        }
        if (qtyPrefill > 0) {
            editQuantity.setText(String.valueOf(qtyPrefill));
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(fishTypePrefill == null ? "Adjust Inventory" : "Edit Inventory")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String fishType = editFishType.getText().toString().trim();
                    String qtyStr = editQuantity.getText().toString().trim();
                    if (!fishType.isEmpty() && !qtyStr.isEmpty()) {
                        int qty = Integer.parseInt(qtyStr);
                        inventoryMap.put(fishType, qty);
                        saveInventory();
                        loadInventory();
                        inventoryAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Inventory updated!", Toast.LENGTH_SHORT).show();
                    }
                    editingFishType = null;
                })
                .setNegativeButton("Cancel", (dialog, which) -> editingFishType = null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
        @NonNull
        @Override
        public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
            return new InventoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
            Map.Entry<String, Integer> entry = inventoryEntries.get(position);
            holder.fishType.setText(entry.getKey());
            holder.stock.setText(String.valueOf(entry.getValue()));
            // Set stock bar color
            View stockBar = holder.itemView.findViewById(R.id.stock_bar);
            int color;
            if (entry.getValue() >= 100 && entry.getValue() <= 1000) {
                color = getResources().getColor(android.R.color.holo_green_dark);
            } else if (entry.getValue() >= 11 && entry.getValue() <= 99) {
                color = getResources().getColor(android.R.color.holo_orange_dark);
            } else {
                color = getResources().getColor(R.color.reminder_red);
            }
            stockBar.setBackgroundColor(color);
            // Edit button
            holder.btnEdit.setOnClickListener(v -> {
                editingFishType = entry.getKey();
                showAdjustDialog(entry.getKey(), entry.getValue());
            });
            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(InventoryActivity.this)
                        .setTitle("Delete Inventory")
                        .setMessage("Delete " + entry.getKey() + " from inventory?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            inventoryMap.remove(entry.getKey());
                            saveInventory();
                            loadInventory();
                            inventoryAdapter.notifyDataSetChanged();
                            Toast.makeText(InventoryActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return inventoryEntries.size();
        }

        class InventoryViewHolder extends RecyclerView.ViewHolder {
            TextView fishType, stock;
            ImageButton btnEdit, btnDelete;
            InventoryViewHolder(@NonNull View itemView) {
                super(itemView);
                fishType = itemView.findViewById(R.id.text_fish_type);
                stock = itemView.findViewById(R.id.text_stock);
                btnEdit = itemView.findViewById(R.id.btn_edit_inventory);
                btnDelete = itemView.findViewById(R.id.btn_delete_inventory);
            }
        }
    }
} 