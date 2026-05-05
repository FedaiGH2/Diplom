package com.example.diplom.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.diplom.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsMonthFragment : Fragment() {

    private lateinit var chartCalories: BarChart
    private lateinit var chartProtein: BarChart
    private lateinit var chartFat: BarChart
    private lateinit var chartCarb: BarChart
    private lateinit var chartFiber: BarChart
    private var currentUser: FirebaseUser? = null
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartCalories = view.findViewById(R.id.chartCalories)
        chartProtein = view.findViewById(R.id.chartProtein)
        chartFat = view.findViewById(R.id.chartFat)
        chartCarb = view.findViewById(R.id.chartCarb)
        chartFiber = view.findViewById(R.id.chartFiber)

        currentUser = FirebaseAuth.getInstance().currentUser
        db = FirebaseFirestore.getInstance()

        loadMonthStats()
    }

    private fun loadMonthStats() {
        val user = currentUser ?: return
        val userId = user.uid

        val calendar = Calendar.getInstance()
        val monthDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val caloriesEntries = mutableListOf<BarEntry>()
        val proteinEntries = mutableListOf<BarEntry>()
        val fatEntries = mutableListOf<BarEntry>()
        val carbEntries = mutableListOf<BarEntry>()
        val fiberEntries = mutableListOf<BarEntry>()

        val labels = mutableListOf<String>()

        var groupIndex = 0
        var completedGroups = 0
        val totalGroups = Math.ceil(monthDays / 4.0).toInt()

        for (startDay in 1..monthDays step 4) {

            val endDay = minOf(startDay + 3, monthDays)
            labels.add("$startDay-$endDay")

            var totalCalories = 0f
            var totalProtein = 0f
            var totalFat = 0f
            var totalCarb = 0f
            var totalFiber = 0f

            var loadedDays = 0
            val daysInGroup = endDay - startDay + 1

            for (day in startDay..endDay) {
                val c = calendar.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, day)

                val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)

                db.collection("dailyMeals")
                    .document(userId)
                    .collection(dayStr)
                    .get()
                    .addOnSuccessListener { querySnapshot ->

                        for (doc in querySnapshot) {
                            totalCalories += doc.getDouble("calories")?.toFloat() ?: 0f
                            totalProtein += doc.getDouble("protein")?.toFloat() ?: 0f
                            totalFat += doc.getDouble("fat")?.toFloat() ?: 0f
                            totalCarb += doc.getDouble("carb")?.toFloat() ?: 0f
                            totalFiber += doc.getDouble("fiber")?.toFloat() ?: 0f
                        }

                        loadedDays++

                        // Когда загрузили все дни в группе
                        if (loadedDays == daysInGroup) {

                            caloriesEntries.add(BarEntry(groupIndex.toFloat(), totalCalories))
                            proteinEntries.add(BarEntry(groupIndex.toFloat(), totalProtein))
                            fatEntries.add(BarEntry(groupIndex.toFloat(), totalFat))
                            carbEntries.add(BarEntry(groupIndex.toFloat(), totalCarb))
                            fiberEntries.add(BarEntry(groupIndex.toFloat(), totalFiber))

                            completedGroups++

                            if (completedGroups == totalGroups) {
                                setupChart(chartCalories, caloriesEntries, "Калории", labels)
                                setupChart(chartProtein, proteinEntries, "Белки", labels)
                                setupChart(chartFat, fatEntries, "Жиры", labels)
                                setupChart(chartCarb, carbEntries, "Углеводы", labels)
                                setupChart(chartFiber, fiberEntries, "Клетчатка", labels)
                            }

                            groupIndex++
                        }
                    }
            }
        }
    }

    private fun setupChart(
        chart: BarChart,
        entries: List<BarEntry>,
        label: String,
        labels: List<String>
    ) {
        val dataSet = BarDataSet(entries, label).apply {
            color = resources.getColor(R.color.blue)

            setDrawValues(true)
            valueTextSize = 14f   // 👀 крупные числа над столбцами
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.5f
            setValueTextSize(14f)
        }

        chart.data = data

        // ===== ОБЩИЕ НАСТРОЙКИ =====
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.setFitBars(true)          // 🔥 важно для X-оси
        chart.setScaleEnabled(false)    // ❌ убираем зум пальцами
        chart.setPinchZoom(false)
        chart.setDragEnabled(true)

        chart.extraBottomOffset = 20f
        chart.extraLeftOffset = 00f
        chart.extraRightOffset = 00f

        // ===== X AXIS =====
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        xAxis.granularity = 1f
        xAxis.labelCount = labels.size   // 🔥 чтобы все подписи были видны
        xAxis.labelRotationAngle = -45f
        chart.setTouchEnabled(false)   // 🔥 ГЛАВНОЕ — отключает ВСЕ касания
        chart.setDragEnabled(false)    // ❌ убираем скролл
        chart.setScaleEnabled(false)   // ❌ зум
        chart.setPinchZoom(false)      // ❌ зум двумя пальцами
        chart.isHighlightPerTapEnabled = false  // ❌ нажатия на столбцы
        chart.isHighlightPerDragEnabled = false // ❌ выделение при свайпе
        xAxis.textSize = 12f
        xAxis.setDrawGridLines(false)

        xAxis.spaceMin = 0.5f
        xAxis.spaceMax = 0.5f

        // ===== Y AXIS =====
        val left = chart.axisLeft
        val right = chart.axisRight

        right.isEnabled = false

        left.textSize = 12f
        left.granularity = 1f
        left.setDrawGridLines(false)

        left.axisMinimum = 0f   // ❌ запрещаем отрицательные значения

        // ===== АНИМАЦИЯ =====
        chart.animateY(800)

        chart.invalidate()
    }
}