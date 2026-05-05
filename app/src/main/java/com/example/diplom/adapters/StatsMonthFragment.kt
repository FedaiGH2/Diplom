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

        val days = (1..monthDays).map { it.toString() }

        val caloriesEntries = mutableListOf<BarEntry>()
        val proteinEntries = mutableListOf<BarEntry>()
        val fatEntries = mutableListOf<BarEntry>()
        val carbEntries = mutableListOf<BarEntry>()
        val fiberEntries = mutableListOf<BarEntry>()

        for (dayIndex in 1..monthDays) {
            val totalCalories = floatArrayOf(0f)
            val totalProtein = floatArrayOf(0f)
            val totalFat = floatArrayOf(0f)
            val totalCarb = floatArrayOf(0f)
            val totalFiber = floatArrayOf(0f)

            val c = calendar.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, dayIndex)
            val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)

            db.collection("dailyMeals")
                .document(userId)
                .collection(dayStr)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (doc in querySnapshot) {
                        totalCalories[0] += doc.getDouble("calories")?.toFloat() ?: 0f
                        totalProtein[0] += doc.getDouble("protein")?.toFloat() ?: 0f
                        totalFat[0] += doc.getDouble("fat")?.toFloat() ?: 0f
                        totalCarb[0] += doc.getDouble("carb")?.toFloat() ?: 0f
                        totalFiber[0] += doc.getDouble("fiber")?.toFloat() ?: 0f
                    }

                    caloriesEntries.add(BarEntry(dayIndex - 1f, totalCalories[0]))
                    proteinEntries.add(BarEntry(dayIndex - 1f, totalProtein[0]))
                    fatEntries.add(BarEntry(dayIndex - 1f, totalFat[0]))
                    carbEntries.add(BarEntry(dayIndex - 1f, totalCarb[0]))
                    fiberEntries.add(BarEntry(dayIndex - 1f, totalFiber[0]))

                    if (caloriesEntries.size == monthDays) {
                        setupChart(chartCalories, caloriesEntries, "Калории", days)
                        setupChart(chartProtein, proteinEntries, "Белки", days)
                        setupChart(chartFat, fatEntries, "Жиры", days)
                        setupChart(chartCarb, carbEntries, "Углеводы", days)
                        setupChart(chartFiber, fiberEntries, "Клетчатка", days)
                    }
                }
        }
    }

    private fun setupChart(chart: BarChart, entries: List<BarEntry>, label: String, labels: List<String>) {
        val dataSet = BarDataSet(entries, label).apply {
            color = resources.getColor(R.color.blue)
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.5f
        }

        chart.data = data
        chart.description.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        val left = chart.axisLeft
        val right = chart.axisRight
        right.isEnabled = false
        left.granularity = 1f

        chart.invalidate()
    }
}