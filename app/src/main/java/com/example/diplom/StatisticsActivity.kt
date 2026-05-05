package com.example.diplom

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // 1. Регистрируем лаунчер для получения результата сканирования
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            if (barcode != null) {
                // 2. Передаем штрих-код в MainActivity, чтобы она открыла нужный диалог
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SCAN_RESULT_FROM_STATS", barcode)
                    putExtra("selected_date", selectedCalendar.timeInMillis)
                    // Очищаем стек, чтобы не плодить копии MainActivity
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }

        // ===== TAB LAYOUT =====
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

        // ===== BOTTOM BAR =====
        val home = findViewById<ImageView>(R.id.home)
        val fav = findViewById<ImageView>(R.id.fav)
        val scan = findViewById<ImageView>(R.id.scan)
        val food = findViewById<ImageView>(R.id.food)
        val stats = findViewById<ImageView>(R.id.stats)

        home.setOnClickListener {
            setActiveTab(home)
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        fav.setOnClickListener {
            setActiveTab(fav)
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        scan.setOnClickListener {
            setActiveTab(scan)
            val intent = Intent(this, ScannerActivity::class.java)
            scanLauncher.launch(intent)
        }

        food.setOnClickListener {
            setActiveTab(food)
            startActivity(Intent(this, FavoriteFoodActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        stats.setOnClickListener {
            setActiveTab(stats)
            // уже тут → ничего не делаем
        }

        // ===== АКТИВНАЯ ВКЛАДКА =====
        setActiveTab(stats)
    }

    private fun setActiveTab(active: ImageView) {

        val tabs = listOf(
            findViewById<ImageView>(R.id.home),
            findViewById<ImageView>(R.id.fav),
            findViewById<ImageView>(R.id.scan),
            findViewById<ImageView>(R.id.food),
            findViewById<ImageView>(R.id.stats)
        )

        tabs.forEach {
            it.background = null
            it.clearColorFilter()
            it.imageTintList = null
        }

        active.background = ContextCompat.getDrawable(this, R.drawable.bg_blue_circle)
    }
}