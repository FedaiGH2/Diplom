package com.example.diplom

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.adapters.AdminPagerAdapter
import com.example.diplom.databinding.ActivityAdminBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var selectedCalendar: Calendar

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            if (barcode != null) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SCAN_RESULT_FROM_STATS", barcode)
                    putExtra("selected_date", selectedCalendar.timeInMillis)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 календарь (чтобы не ломать навигацию)
        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }

        // ViewPager + Tabs
        binding.adminViewPager.adapter = AdminPagerAdapter(this)

        TabLayoutMediator(
            binding.adminTabLayout,
            binding.adminViewPager
        ) { tab, pos ->
            tab.text = when (pos) {
                0 -> "Добавить еду"
                1 -> "Пользователи"
                2 -> "Продукты"
                else -> ""
            }
        }.attach()

        // 🔥 НИЖНЕЕ МЕНЮ
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = binding.bottomNavigation

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("selected_date", selectedCalendar.timeInMillis)
                    })
                    true
                }

                R.id.nav_fav_activity -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        putExtra("selected_date", selectedCalendar.timeInMillis)
                    })
                    true
                }

                R.id.nav_scan -> {
                    val intent = Intent(this, ScannerActivity::class.java)
                    scanLauncher.launch(intent)
                    true
                }

                R.id.nav_fav_food -> {
                    startActivity(Intent(this, FavoriteFoodActivity::class.java).apply {
                        putExtra("selected_date", selectedCalendar.timeInMillis)
                    })
                    true
                }

                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java).apply {
                        putExtra("selected_date", selectedCalendar.timeInMillis)
                    })
                    true
                }

                else -> false
            }
        }
    }
}