package com.example.it3c_grp10_navarrosa;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.core.view.MenuItemCompat;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.app.Dialog;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.WindowManager;
import android.util.DisplayMetrics;

public class MainPageActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MenuItem notificationMenuItem;
    private boolean hasUpcomingFeeding = false;
    private List<FeedingSchedule> upcomingFeedings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String userName = prefs.getString("user_name", "User");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hello, " + userName + "!");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        CardView logbookCard = findViewById(R.id.logbook_card);
        CardView salesMonitoringCard = findViewById(R.id.sales_monitoring_card);
        CardView inventoryCard = findViewById(R.id.inventory_card);

        logbookCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainPageActivity.this, LogCatchActivity.class);
            startActivity(intent);
        });

        salesMonitoringCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainPageActivity.this, TrackingActivity.class);
            startActivity(intent);
        });

        inventoryCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainPageActivity.this, InventoryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logbook) {
            startActivity(new Intent(this, LogCatchActivity.class));
        } else if (id == R.id.nav_tracking) {
            startActivity(new Intent(this, TrackingActivity.class));
        } else if (id == R.id.nav_inventory) {
            startActivity(new Intent(this, InventoryActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_page_menu, menu);
        notificationMenuItem = menu.findItem(R.id.action_notifications);
        updateNotificationBadge();
        return true;
    }

    private void updateNotificationBadge() {
        View actionView = MenuItemCompat.getActionView(notificationMenuItem);
        if (actionView == null) {
            notificationMenuItem.setActionView(R.layout.notification_badge);
            actionView = MenuItemCompat.getActionView(notificationMenuItem);
        }
        View badge = actionView.findViewById(R.id.badge_dot);
        if (badge != null) {
            badge.setVisibility(hasUpcomingFeeding ? View.VISIBLE : View.GONE);
        }
        actionView.setOnClickListener(v -> onOptionsItemSelected(notificationMenuItem));
    }

    private String getUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "default";
    }

    private void checkUpcomingFeedings() {
        hasUpcomingFeeding = false;
        upcomingFeedings.clear();
        SharedPreferences prefs = getSharedPreferences("CatchRecords", MODE_PRIVATE);
        String json = prefs.getString("feeding_schedules_" + getUserUid(), null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<FeedingSchedule>>(){}.getType();
            List<FeedingSchedule> allFeedings = new Gson().fromJson(json, type);
            long now = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            for (FeedingSchedule fs : allFeedings) {
                try {
                    Date feedingDate = sdf.parse(fs.date + " " + fs.time);
                    if (feedingDate != null && feedingDate.getTime() > now) {
                        hasUpcomingFeeding = true;
                        upcomingFeedings.add(fs);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            if (hasUpcomingFeeding && !upcomingFeedings.isEmpty()) {
                LayoutInflater inflater = LayoutInflater.from(this);
                ScrollView dialogView = (ScrollView) inflater.inflate(R.layout.dialog_feeding_schedules, null);
                LinearLayout listLayout = dialogView.findViewById(R.id.dialog_feeding_list);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                long now = System.currentTimeMillis();
                for (FeedingSchedule fs : upcomingFeedings) {
                    View card = inflater.inflate(R.layout.item_dialog_feeding_schedule, listLayout, false);
                    ((TextView) card.findViewById(R.id.dialog_feeding_time)).setText(fs.time);
                    ((TextView) card.findViewById(R.id.dialog_feeding_date)).setText(fs.date);
                    try {
                        Date feedingDate = sdf.parse(fs.date + " " + fs.time);
                        long diff = feedingDate.getTime() - now;
                        long mins = diff / (60 * 1000);
                        long hours = mins / 60;
                        mins = mins % 60;
                        String timeLeft = (hours > 0 ? hours + "h " : "") + mins + "m left";
                        ((TextView) card.findViewById(R.id.dialog_time_left)).setText(timeLeft);
                    } catch (Exception e) {
                        ((TextView) card.findViewById(R.id.dialog_time_left)).setText("");
                    }
                    if (fs.notes != null && !fs.notes.isEmpty()) {
                        ((TextView) card.findViewById(R.id.dialog_feeding_notes)).setText("Notes: " + fs.notes);
                        ((TextView) card.findViewById(R.id.dialog_feeding_notes)).setVisibility(View.VISIBLE);
                    } else {
                        ((TextView) card.findViewById(R.id.dialog_feeding_notes)).setVisibility(View.GONE);
                    }
                    listLayout.addView(card);
                }
                Dialog dialog = new Dialog(this);
                dialog.setContentView(dialogView);
                dialog.setTitle("Upcoming Feeding Schedules");
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = (int) (metrics.widthPixels * 0.9);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
                }
                dialog.show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("No Upcoming Feeding")
                        .setMessage("No future feeding scheduled.")
                        .setPositiveButton("OK", null)
                        .show();
            }
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainPageActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUpcomingFeedings();
        if (notificationMenuItem != null) {
            updateNotificationBadge();
        }
    }

    private void showFeedFishNotification() {
        String channelId = "feed_fish_channel";
        String channelName = "Feed Fish Reminder";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainPageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Fish Feeding Reminder")
                .setContentText("It's time to feed the fish!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManager.notify(1, builder.build());
    }
} 