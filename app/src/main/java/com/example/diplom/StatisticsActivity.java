package com.example.diplom;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.diplom.logic.StatsPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


import android.content.Intent;
import android.os.Bundle;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.diplom.logic.StatsPagerAdapter;
import com.example.diplom.MainActivity;
import com.example.diplom.ProfileActivity;
import com.example.diplom.FavoriteFoodActivity;

import androidx.viewpager2.widget.ViewPager2;

public class StatisticsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);


        long calendarMillis = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
        selectedCalendar = Calendar.getInstance();
        selectedCalendar.setTimeInMillis(calendarMillis);

        tabLayout = findViewById(R.id.statsTabLayout);
        viewPager = findViewById(R.id.statsViewPager);

        viewPager.setAdapter(new StatsPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) tab.setText("Неделя");
                    else if (position == 1) tab.setText("Месяц");
                    else tab.setText("Год");
                }
        ).attach();


        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(StatisticsActivity.this, MainActivity.class);
                intent.putExtra("selected_date", selectedCalendar.getTimeInMillis());
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_fav_activity) {
                startActivity(new Intent(StatisticsActivity.this, ProfileActivity.class));
                return true;
            }
            if (id == R.id.nav_scan) {
                
                return true;
            }
            if (id == R.id.nav_fav_food) {
                Intent intent = new Intent(StatisticsActivity.this, FavoriteFoodActivity.class);
                intent.putExtra("selected_date", selectedCalendar.getTimeInMillis());
                startActivity(intent);
                return true;
            }
            if (id == R.id.nav_stats) {
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.nav_stats);
    }
}