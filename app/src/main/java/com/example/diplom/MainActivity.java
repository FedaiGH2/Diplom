package com.example.diplom;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diplom.logic.SearchAdapter;
import com.example.diplom.logic.TodayMealAdapter;
import com.example.diplom.models.MealItem;
import com.example.diplom.models.ProductItem;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView searchResults;
    private SearchAdapter searchAdapter;
    private List<ProductItem> searchList = new ArrayList<>();

    private TextView dayOfWeekText;
    private Calendar currentCalendar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RecyclerView todayMealsRecycler;
    private TodayMealAdapter adapter;
    private List<MealItem> todayMeals = new ArrayList<>();

    private PieChart mainPieChart, proteinChart, fatChart, carbChart, fiberChart;

    private float totalCalories = 0;
    private float totalProtein = 0;
    private float totalFat = 0;
    private float totalCarb = 0;
    private float totalFiber = 0;

    private float CALORIES_NORM = 2000f;
    private float PROTEIN_NORM = 100f;
    private float FAT_NORM = 70f;
    private float CARB_NORM = 300f;
    private float FIBER_NORM = 35f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        TextView mainChartValue = findViewById(R.id.mainChartValue);

        TextView proteinValue = findViewById(R.id.proteinValue);
        TextView fatValue = findViewById(R.id.fatValue);
        TextView carbValue = findViewById(R.id.carbValue);
        TextView fiberValue = findViewById(R.id.fiberValue);


        mainPieChart = findViewById(R.id.mainPieChart);
        proteinChart = findViewById(R.id.proteinChart);
        fatChart = findViewById(R.id.fatChart);
        carbChart = findViewById(R.id.carbChart);
        fiberChart = findViewById(R.id.fiberChart);

        dayOfWeekText = findViewById(R.id.dayOfWeekText);
        long selectedDateMillis = getIntent().getLongExtra("selected_date", -1);
        currentCalendar = Calendar.getInstance();
        if (selectedDateMillis != -1) currentCalendar.setTimeInMillis(selectedDateMillis);
        updateDayUI();

        Button prevDayBtn = findViewById(R.id.prevDayBtn);
        Button nextDayBtn = findViewById(R.id.nextDayBtn);
        prevDayBtn.setOnClickListener(v -> changeDay(-1));
        nextDayBtn.setOnClickListener(v -> changeDay(1));

        todayMealsRecycler = findViewById(R.id.todayMealsRecycler);
        adapter = new TodayMealAdapter(this, todayMeals, currentCalendar);
        todayMealsRecycler.setAdapter(adapter);
        todayMealsRecycler.setLayoutManager(new LinearLayoutManager(this));

        setupBottomNavigation();
        setupSearch();
    }

    private void changeDay(int delta) {
        currentCalendar.add(Calendar.DAY_OF_MONTH, delta);
        updateDayUI();
        adapter.setCurrentCalendar(currentCalendar);
        loadDailyMeals();
    }

    private void updateDayUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd-MM-yyyy", new Locale("ru"));
        dayOfWeekText.setText(sdf.format(currentCalendar.getTime()));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent;
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_fav_activity) intent = new Intent(this, ProfileActivity.class);
            else if (id == R.id.nav_scan) { startScan(); return true; }
            else if (id == R.id.nav_fav_food) intent = new Intent(this, FavoriteFoodActivity.class);
            else if (id == R.id.nav_stats) intent = new Intent(this, StatisticsActivity.class);
            else return false;

            intent.putExtra("selected_date", currentCalendar.getTimeInMillis());
            startActivity(intent);
            return true;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void setupSearch() {
        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResultsRecycler);
        searchResults.setLayoutManager(new LinearLayoutManager(this));

        searchAdapter = new SearchAdapter(this, searchList, product -> {
            showAddFoodDialog(product.getId());
            searchResults.setVisibility(View.GONE);
            searchInput.setText("");
        });
        searchResults.setAdapter(searchAdapter);

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    searchList.clear();
                    searchAdapter.notifyDataSetChanged();
                    searchResults.setVisibility(View.GONE);
                    return;
                }
                db.collection("products")
                        .whereGreaterThanOrEqualTo("name", query)
                        .whereLessThanOrEqualTo("name", query + "\uf8ff")
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            searchList.clear();
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                String id = doc.getId();
                                String name = doc.getString("name");
                                if (name == null) continue;

                                float calories = getFloatSafe(doc, "calories");
                                float protein = getFloatSafe(doc, "protein");
                                float fat = getFloatSafe(doc, "fat");
                                float carb = getFloatSafe(doc, "carb");
                                float fiber = getFloatSafe(doc, "fiber");

                                searchList.add(new ProductItem(id, name, calories, protein, fat, carb, fiber));
                            }
                            searchAdapter.notifyDataSetChanged();
                            searchResults.setVisibility(View.VISIBLE);
                        });
            }
        });
    }

    private float getFloatSafe(DocumentSnapshot doc, String field) {
        Object obj = doc.get(field);
        if (obj instanceof Number) return ((Number) obj).floatValue();
        if (obj instanceof String) {
            try { return Float.parseFloat((String) obj); }
            catch (NumberFormatException ignored) {}
        }
        return 0f;
    }

    private void showAddFoodDialog(String productId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_food, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();


        TextView nameText = view.findViewById(R.id.dialogProductName);
        TextView proteinText = view.findViewById(R.id.dialogProteinText);
        TextView fatText = view.findViewById(R.id.dialogFatText);
        TextView carbText = view.findViewById(R.id.dialogCarbText);
        TextView fiberText = view.findViewById(R.id.dialogFiberText);
        TextView caloriesText = view.findViewById(R.id.dialogCaloriesText);

        EditText gramsInput = view.findViewById(R.id.dialogGramsInput);

        Button btn100g = view.findViewById(R.id.dialogBtn100g);
        Button btnAddMeal = view.findViewById(R.id.dialogAddMeal);
        Button btnAddFavorite = view.findViewById(R.id.dialogAddFavorite);


        btn100g.setOnClickListener(v -> gramsInput.setText("100"));


        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    String name = doc.getString("name");
                    float protein = getFloatSafe(doc, "protein");
                    float fat = getFloatSafe(doc, "fat");
                    float carb = getFloatSafe(doc, "carb");
                    float fiber = getFloatSafe(doc, "fiber");
                    float calories = getFloatSafe(doc, "calories");

                    nameText.setText(name);
                    proteinText.setText("Белки: " + Math.round(protein));
                    fatText.setText("Жиры: " + Math.round(fat));
                    carbText.setText("Углеводы: " + Math.round(carb));
                    fiberText.setText("Клетчатка: " + Math.round(fiber));
                    caloriesText.setText("Калории: " + Math.round(calories));
                });


        btnAddFavorite.setOnClickListener(v -> {
            if (currentUser == null) return;
            String userId = currentUser.getUid();

            db.collection("products").document(productId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;
                        Map<String, Object> fav = new HashMap<>();
                        fav.put("name", doc.getString("name"));
                        fav.put("calories", getFloatSafe(doc, "calories"));
                        fav.put("protein", getFloatSafe(doc, "protein"));
                        fav.put("fat", getFloatSafe(doc, "fat"));
                        fav.put("carb", getFloatSafe(doc, "carb"));
                        fav.put("fiber", getFloatSafe(doc, "fiber"));

                        db.collection("usersFood").document(userId)
                                .collection("favorite_food").document(productId)
                                .set(fav);

                        Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                    });
        });


        btnAddMeal.setOnClickListener(v -> {
            if (currentUser == null) return;

            String gramsStr = gramsInput.getText().toString().trim();
            if (gramsStr.isEmpty()) {
                Toast.makeText(this, "Введите граммовку", Toast.LENGTH_SHORT).show();
                return;
            }

            float grams = Float.parseFloat(gramsStr);
            String userId = currentUser.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(currentCalendar.getTime());

            db.collection("products").document(productId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;

                        final float multiplier = grams / 100f;

                        Map<String, Object> meal = new HashMap<>();
                        meal.put("name", doc.getString("name"));
                        meal.put("grams", grams);
                        meal.put("calories", getFloatSafe(doc, "calories") * multiplier);
                        meal.put("protein", getFloatSafe(doc, "protein") * multiplier);
                        meal.put("fat", getFloatSafe(doc, "fat") * multiplier);
                        meal.put("carb", getFloatSafe(doc, "carb") * multiplier);
                        meal.put("fiber", getFloatSafe(doc, "fiber") * multiplier);
                        meal.put("timestamp", System.currentTimeMillis());

                        db.collection("dailyMeals")
                                .document(userId)
                                .collection(today)
                                .add(meal)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Добавлено!", Toast.LENGTH_SHORT).show();
                                    loadDailyMeals();
                                    updateCharts();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Ошибка при добавлении продукта", Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Ошибка при получении данных продукта", Toast.LENGTH_SHORT).show()
                    );
        });

        dialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadDailyMeals();
        loadUserNorms();
    }

    public void loadDailyMeals() {
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.getTime());

        db.collection("dailyMeals").document(userId).collection(today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    totalCalories = 0; totalProtein = 0; totalFat = 0; totalCarb = 0; totalFiber = 0;
                    todayMeals.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        float calories = getFloatSafe(doc, "calories");
                        float protein = getFloatSafe(doc, "protein");
                        float fat = getFloatSafe(doc, "fat");
                        float carb = getFloatSafe(doc, "carb");
                        float fiber = getFloatSafe(doc, "fiber");
                        float grams = getFloatSafe(doc, "grams");
                        if (grams == 0f) grams = 100f;

                        totalCalories += calories;
                        totalProtein += protein;
                        totalFat += fat;
                        totalCarb += carb;
                        totalFiber += fiber;

                        todayMeals.add(new MealItem(id, name, grams, calories, protein, fat, carb, fiber));
                    }

                    adapter.notifyDataSetChanged();
                    updateCharts();
                });
    }

    private void updateCharts() {
        setupMainChart(totalCalories);


        TextView mainChartValue = findViewById(R.id.mainChartValue);
        mainChartValue.setText("За сегодня: "+ Math.round(totalCalories) + " / " + Math.round(CALORIES_NORM)+" ккал");


        ((TextView)findViewById(R.id.proteinValue)).setText(Math.round(totalProtein) + " / " + Math.round(PROTEIN_NORM)+" г");
        ((TextView)findViewById(R.id.fatValue)).setText( Math.round(totalFat) + " / " + Math.round(FAT_NORM)+" г");
        ((TextView)findViewById(R.id.carbValue)).setText(Math.round(totalCarb) + " / " + Math.round(CARB_NORM)+" г");
        ((TextView)findViewById(R.id.fiberValue)).setText(Math.round(totalFiber) + " / " + Math.round(FIBER_NORM)+" г");

        setupMacroCharts(totalProtein, totalFat, totalCarb, totalFiber);
    }

    private void setupMainChart(float calories) {
        float remaining = CALORIES_NORM - calories;
        if (remaining < 0) remaining = 0;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(calories));
        entries.add(new PieEntry(remaining));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.BLUE, Color.LTGRAY);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        mainPieChart.setData(data);

        mainPieChart.setUsePercentValues(true);
        mainPieChart.setDrawHoleEnabled(true);
        mainPieChart.setHoleRadius(60f);


        mainPieChart.getDescription().setEnabled(false);
        mainPieChart.getLegend().setEnabled(false);
        mainPieChart.setDrawEntryLabels(false);

        mainPieChart.invalidate();
    }

    private void setupMacroCharts(float protein, float fat, float carb, float fiber) {
        setupSmallChart(proteinChart, protein, "Белки", PROTEIN_NORM);
        setupSmallChart(fatChart, fat, "Жиры", FAT_NORM);
        setupSmallChart(carbChart, carb, "Углеводы", CARB_NORM);
        setupSmallChart(fiberChart, fiber, "Клетчатка", FIBER_NORM);
    }

    private void setupSmallChart(PieChart chart, float value, String label, float norm) {
        float remaining = norm - value;
        if (remaining < 0) remaining = 0;

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(value, label));
        entries.add(new PieEntry(remaining, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.GREEN, Color.LTGRAY);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chart.setData(data);

        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(60f);


        chart.getLegend().setEnabled(false);


        chart.setDrawEntryLabels(false);

        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String barcode = result.getContents();
                db.collection("products").document(barcode).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) showAddFoodDialog(barcode);
                            else {
                                Intent intent = new Intent(this, AddProductActivity.class);
                                intent.putExtra("barcode", barcode);
                                startActivity(intent);
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Ошибка при проверке продукта", Toast.LENGTH_SHORT).show());
            } else Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show();
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadUserNorms() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    float weight = parseFloatFromStringOrNumber(doc.get("weight"));
                    float height = parseFloatFromStringOrNumber(doc.get("height"));
                    int age = parseIntFromStringOrNumber(doc.get("age"));
                    String gender = doc.getString("gender");

                    if (weight == 0f || height == 0f || age == 0 || gender == null) return;

                    if (gender.equalsIgnoreCase("Мужской")) {
                        CALORIES_NORM = (10 * weight) + (6.25f * height) - (5 * age) + 5;
                    } else {
                        CALORIES_NORM = (10 * weight) + (6.25f * height) - (5 * age) - 161;
                    }

                    PROTEIN_NORM = 2.2f * weight;
                    FAT_NORM = 1f * weight;
                    CARB_NORM = 5f * weight;

                    updateCharts();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show());
    }


    private float parseFloatFromStringOrNumber(Object obj) {
        if (obj instanceof Number) return ((Number) obj).floatValue();
        if (obj instanceof String) {
            try { return Float.parseFloat((String) obj); }
            catch (NumberFormatException ignored) {}
        }
        return 0f;
    }

    private int parseIntFromStringOrNumber(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }

}
