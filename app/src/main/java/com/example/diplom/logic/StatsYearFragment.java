package com.example.diplom.logic;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.diplom.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StatsYearFragment extends Fragment {

    private BarChart chartCalories, chartProtein, chartFat, chartCarb, chartFiber;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    public StatsYearFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartCalories = view.findViewById(R.id.chartCalories);
        chartProtein = view.findViewById(R.id.chartProtein);
        chartFat = view.findViewById(R.id.chartFat);
        chartCarb = view.findViewById(R.id.chartCarb);
        chartFiber = view.findViewById(R.id.chartFiber);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        loadYearStats();
    }

    private void loadYearStats() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        ArrayList<String> months = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, 1);

        for (int i = 0; i < 12; i++) {
            Calendar c = (Calendar) calendar.clone();
            c.set(Calendar.MONTH, i);
            months.add(new SimpleDateFormat("MMM", Locale.getDefault()).format(c.getTime()));
        }

        ArrayList<BarEntry> caloriesEntries = new ArrayList<>();
        ArrayList<BarEntry> proteinEntries = new ArrayList<>();
        ArrayList<BarEntry> fatEntries = new ArrayList<>();
        ArrayList<BarEntry> carbEntries = new ArrayList<>();
        ArrayList<BarEntry> fiberEntries = new ArrayList<>();


        for (int i = 0; i < 12; i++) {
            final int monthIndex = i;
            Calendar start = Calendar.getInstance();
            start.set(Calendar.MONTH, monthIndex);
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            int daysInMonth = start.getActualMaximum(Calendar.DAY_OF_MONTH);

            final float[] totalCalories = {0f};
            final float[] totalProtein = {0f};
            final float[] totalFat = {0f};
            final float[] totalCarb = {0f};
            final float[] totalFiber = {0f};


            for (int d = 1; d <= daysInMonth; d++) {
                final int dayOfMonth = d;
                Calendar dayCal = (Calendar) start.clone();
                dayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String dayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.getTime());

                db.collection("dailyMeals")
                        .document(userId)
                        .collection(dayStr)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                totalCalories[0] += doc.getDouble("calories") != null ? doc.getDouble("calories").floatValue() : 0f;
                                totalProtein[0] += doc.getDouble("protein") != null ? doc.getDouble("protein").floatValue() : 0f;
                                totalFat[0] += doc.getDouble("fat") != null ? doc.getDouble("fat").floatValue() : 0f;
                                totalCarb[0] += doc.getDouble("carb") != null ? doc.getDouble("carb").floatValue() : 0f;
                                totalFiber[0] += doc.getDouble("fiber") != null ? doc.getDouble("fiber").floatValue() : 0f;
                            }

                            // добавляем данные месяца только после последнего дня
                            if (dayOfMonth == daysInMonth) {
                                caloriesEntries.add(new BarEntry(monthIndex, totalCalories[0]));
                                proteinEntries.add(new BarEntry(monthIndex, totalProtein[0]));
                                fatEntries.add(new BarEntry(monthIndex, totalFat[0]));
                                carbEntries.add(new BarEntry(monthIndex, totalCarb[0]));
                                fiberEntries.add(new BarEntry(monthIndex, totalFiber[0]));

                                if (caloriesEntries.size() == 12) {
                                    setupChart(chartCalories, caloriesEntries, "Калории", months);
                                    setupChart(chartProtein, proteinEntries, "Белки", months);
                                    setupChart(chartFat, fatEntries, "Жиры", months);
                                    setupChart(chartCarb, carbEntries, "Углеводы", months);
                                    setupChart(chartFiber, fiberEntries, "Клетчатка", months);
                                }
                            }
                        });
            }
        }
    }

    private void setupChart(BarChart chart, ArrayList<BarEntry> entries, String label, ArrayList<String> labels){
        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.blue));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        chart.setData(data);
        chart.getDescription().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        YAxis left = chart.getAxisLeft();
        YAxis right = chart.getAxisRight();
        right.setEnabled(false);
        left.setGranularity(1f);

        chart.invalidate();
    }
}
