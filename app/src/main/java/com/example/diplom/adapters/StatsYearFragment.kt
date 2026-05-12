package com.example.diplom.adapters

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.diplom.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

class StatsYearFragment : Fragment() {

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
    ): View? = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartCalories = view.findViewById(R.id.chartCalories)
        chartProtein = view.findViewById(R.id.chartProtein)
        chartFat = view.findViewById(R.id.chartFat)
        chartCarb = view.findViewById(R.id.chartCarb)
        chartFiber = view.findViewById(R.id.chartFiber)

        currentUser = FirebaseAuth.getInstance().currentUser
        db = FirebaseFirestore.getInstance()

        loadYearStats()
    }

    private fun loadYearStats() {
        val user = currentUser ?: return
        val userId = user.uid

        val calendar = Calendar.getInstance()

        val monthsList = mutableListOf<Calendar>()

        // ===== текущий + 6 предыдущих =====
        for (i in 6 downTo 0) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.MONTH, -i)
            monthsList.add(cal)
        }

        // ===== подписи месяцев =====
        val monthsLabels = monthsList.map {
            SimpleDateFormat("MMM", Locale.getDefault()).format(it.time)
        }

        val caloriesEntries = mutableListOf<BarEntry>()
        val proteinEntries = mutableListOf<BarEntry>()
        val fatEntries = mutableListOf<BarEntry>()
        val carbEntries = mutableListOf<BarEntry>()
        val fiberEntries = mutableListOf<BarEntry>()

        var loadedMonths = 0

        for ((index, monthCal) in monthsList.withIndex()) {

            val start = monthCal.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val daysInMonth = start.getActualMaximum(Calendar.DAY_OF_MONTH)

            var totalCalories = 0f
            var totalProtein = 0f
            var totalFat = 0f
            var totalCarb = 0f
            var totalFiber = 0f

            var daysProcessed = 0

            for (day in 1..daysInMonth) {

                val dayCal = start.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, day)

                val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(dayCal.time)

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

                        daysProcessed++

                        // ===== когда ВСЕ дни месяца обработаны =====
                        if (daysProcessed == daysInMonth) {

                            caloriesEntries.add(BarEntry(index.toFloat(), totalCalories))
                            proteinEntries.add(BarEntry(index.toFloat(), totalProtein))
                            fatEntries.add(BarEntry(index.toFloat(), totalFat))
                            carbEntries.add(BarEntry(index.toFloat(), totalCarb))
                            fiberEntries.add(BarEntry(index.toFloat(), totalFiber))

                            loadedMonths++

                            // ===== когда ВСЕ месяцы готовы =====
                            if (loadedMonths == monthsList.size) {
                                if (loadedMonths == monthsList.size) {

                                    setupChart(chartCalories, caloriesEntries, "Калории", monthsLabels, Color.BLUE)

                                    setupChart(chartProtein, proteinEntries, "Белки", monthsLabels, Color.RED)

                                    setupChart(chartFat, fatEntries, "Жиры", monthsLabels, Color.parseColor("#FFA500"))

                                    setupChart(chartCarb, carbEntries, "Углеводы", monthsLabels, Color.YELLOW)

                                    setupChart(chartFiber, fiberEntries, "Клетчатка", monthsLabels, Color.GREEN)
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun setupChart(
        chart: BarChart,
        entries: List<BarEntry>,
        label: String,
        labels: List<String>,
        color: Int
    ) {
        val dataSet = BarDataSet(entries, label).apply {
            this.color = color
            setDrawValues(true)
            valueTextSize = 14f
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

        chart.setFitBars(true)

        chart.setTouchEnabled(false)
        chart.setDragEnabled(false)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isHighlightPerTapEnabled = false
        chart.isHighlightPerDragEnabled = false

        chart.extraBottomOffset = 20f

        // ===== X AXIS =====
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        xAxis.granularity = 1f
        xAxis.labelCount = labels.size
        xAxis.labelRotationAngle = -45f
        xAxis.textSize = 12f
        xAxis.setDrawGridLines(false)

        xAxis.spaceMin = 0.5f
        xAxis.spaceMax = 0.5f

        // ===== Y AXIS =====
        val left = chart.axisLeft
        chart.axisRight.isEnabled = false

        left.textSize = 12f
        left.granularity = 1f
        left.setDrawGridLines(false)
        left.axisMinimum = 0f

        // ===== АНИМАЦИЯ =====
        chart.animateY(800)
        chart.invalidate()
    }}