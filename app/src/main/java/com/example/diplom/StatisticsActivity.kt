package com.example.diplom

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.diplom.adapters.StatsPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var selectedCalendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }

        tabLayout = findViewById(R.id.statsTabLayout)
        viewPager = findViewById(R.id.statsViewPager)

        viewPager.adapter = StatsPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Неделя"
                1 -> "Месяц"
                else -> "Год"
            }
        }.attach()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("selected_date", selectedCalendar.timeInMillis)
                    })
                    true
                }
                R.id.nav_fav_activity -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_scan -> {
                    // TODO: добавить сканер штрихкодов
                    true
                }
                R.id.nav_fav_food -> {
                    startActivity(Intent(this, FavoriteFoodActivity::class.java).apply {
                        putExtra("selected_date", selectedCalendar.timeInMillis)
                    })
                    true
                }
                R.id.nav_stats -> true
                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.nav_stats
    }
}