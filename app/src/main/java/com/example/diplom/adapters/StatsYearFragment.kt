package com.example.diplom.adapters

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

        val months = mutableListOf<String>()
        val calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }

        for (i in 0..11) {
            val c = calendar.clone() as Calendar
            c.set(Calendar.MONTH, i)
            months.add(SimpleDateFormat("MMM", Locale.getDefault()).format(c.time))
        }

        val caloriesEntries = mutableListOf<BarEntry>()
        val proteinEntries = mutableListOf<BarEntry>()
        val fatEntries = mutableListOf<BarEntry>()
        val carbEntries = mutableListOf<BarEntry>()
        val fiberEntries = mutableListOf<BarEntry>()

        for (monthIndex in 0..11) {
            val start = Calendar.getInstance().apply {
                set(Calendar.MONTH, monthIndex)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val daysInMonth = start.getActualMaximum(Calendar.DAY_OF_MONTH)

            val totalCalories = floatArrayOf(0f)
            val totalProtein = floatArrayOf(0f)
            val totalFat = floatArrayOf(0f)
            val totalCarb = floatArrayOf(0f)
            val totalFiber = floatArrayOf(0f)

            for (dayOfMonth in 1..daysInMonth) {
                val dayCal = start.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.time)

                db.collection("dailyMeals")
                    .document(userId)
                    .collection(dayStr)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (doc: QueryDocumentSnapshot in querySnapshot) {
                            totalCalories[0] += doc.getDouble("calories")?.toFloat() ?: 0f
                            totalProtein[0] += doc.getDouble("protein")?.toFloat() ?: 0f
                            totalFat[0] += doc.getDouble("fat")?.toFloat() ?: 0f
                            totalCarb[0] += doc.getDouble("carb")?.toFloat() ?: 0f
                            totalFiber[0] += doc.getDouble("fiber")?.toFloat() ?: 0f
                        }

                        // Добавляем данные месяца только после последнего дня
                        if (dayOfMonth == daysInMonth) {
                            caloriesEntries.add(BarEntry(monthIndex.toFloat(), totalCalories[0]))
                            proteinEntries.add(BarEntry(monthIndex.toFloat(), totalProtein[0]))
                            fatEntries.add(BarEntry(monthIndex.toFloat(), totalFat[0]))
                            carbEntries.add(BarEntry(monthIndex.toFloat(), totalCarb[0]))
                            fiberEntries.add(BarEntry(monthIndex.toFloat(), totalFiber[0]))

                            if (caloriesEntries.size == 12) {
                                setupChart(chartCalories, caloriesEntries, "Калории", months)
                                setupChart(chartProtein, proteinEntries, "Белки", months)
                                setupChart(chartFat, fatEntries, "Жиры", months)
                                setupChart(chartCarb, carbEntries, "Углеводы", months)
                                setupChart(chartFiber, fiberEntries, "Клетчатка", months)
                            }
                        }
                    }
            }
        }
    }

    private fun setupChart(chart: BarChart, entries: List<BarEntry>, label: String, labels: List<String>) {
        val dataSet = BarDataSet(entries, label).apply {
            color = resources.getColor(R.color.blue)
        }

        val data = BarData(dataSet).apply { barWidth = 0.5f }

        chart.data = data
        chart.description.isEnabled = false

        val xAxis: XAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        val left: YAxis = chart.axisLeft
        val right: YAxis = chart.axisRight
        right.isEnabled = false
        left.granularity = 1f

        chart.invalidate()
    }
}