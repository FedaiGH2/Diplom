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

public class StatsWeekFragment extends Fragment {

    private BarChart chartCalories, chartProtein, chartFat, chartCarb, chartFiber;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    public StatsWeekFragment() {}

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

        loadWeekStats();
    }

    private void loadWeekStats() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        Calendar calendar = Calendar.getInstance();
        ArrayList<String> days = new ArrayList<>();
        ArrayList<String> dateStrings = new ArrayList<>();

        // последние 7 дней
        for (int i = 6; i >= 0; i--) {
            Calendar c = (Calendar) calendar.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            dateStrings.add(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime()));
            days.add(new SimpleDateFormat("EEE", Locale.getDefault()).format(c.getTime()));
        }

        ArrayList<BarEntry> caloriesEntries = new ArrayList<>();
        ArrayList<BarEntry> proteinEntries = new ArrayList<>();
        ArrayList<BarEntry> fatEntries = new ArrayList<>();
        ArrayList<BarEntry> carbEntries = new ArrayList<>();
        ArrayList<BarEntry> fiberEntries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            final int index = i;
            final float[] totalCalories = {0f};
            final float[] totalProtein = {0f};
            final float[] totalFat = {0f};
            final float[] totalCarb = {0f};
            final float[] totalFiber = {0f};

            String dayStr = dateStrings.get(i);

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

                        caloriesEntries.add(new BarEntry(index, totalCalories[0]));
                        proteinEntries.add(new BarEntry(index, totalProtein[0]));
                        fatEntries.add(new BarEntry(index, totalFat[0]));
                        carbEntries.add(new BarEntry(index, totalCarb[0]));
                        fiberEntries.add(new BarEntry(index, totalFiber[0]));

                        if (caloriesEntries.size() == 7) {
                            setupChart(chartCalories, caloriesEntries, "Калории", days);
                            setupChart(chartProtein, proteinEntries, "Белки", days);
                            setupChart(chartFat, fatEntries, "Жиры", days);
                            setupChart(chartCarb, carbEntries, "Углеводы", days);
                            setupChart(chartFiber, fiberEntries, "Клетчатка", days);
                        }
                    });
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
