package com.example.diplom;

import android.os.Bundle;
import com.example.diplom.MainActivity;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.text.TextWatcher;
import android.text.Editable;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;

import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.diplom.logic.TodayMealAdapter;
import com.example.diplom.models.MealItem;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.view.View;

import com.example.diplom.logic.SearchAdapter;
import com.example.diplom.models.ProductItem;


public class FavoriteFoodActivity extends AppCompatActivity {

    private EditText favSearchInput;
    private RecyclerView favRecyclerView;
    private SearchAdapter favAdapter;
    private List<ProductItem> favoriteList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_food);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();


        long calendarMillis = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
        selectedCalendar = Calendar.getInstance();
        selectedCalendar.setTimeInMillis(calendarMillis);

        favSearchInput = findViewById(R.id.favSearchInput);
        favRecyclerView = findViewById(R.id.favRecyclerView);
        favRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        favAdapter = new SearchAdapter(this, favoriteList, product -> showAddFoodDialog(product.getId()));
        favRecyclerView.setAdapter(favAdapter);

        loadFavorites();
        setupSearch();


        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent;
            if (id == R.id.nav_home) {
                intent = new Intent(FavoriteFoodActivity.this, MainActivity.class);
            } else if (id == R.id.nav_fav_activity) {
                intent = new Intent(FavoriteFoodActivity.this, ProfileActivity.class);
            } else if (id == R.id.nav_scan) {
                IntentIntegrator integrator = new IntentIntegrator(FavoriteFoodActivity.this);
                integrator.setBeepEnabled(true);
                integrator.setPrompt("Сканируйте штрихкод продукта");
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
                return true;
            } else if (id == R.id.nav_fav_food) {
                return true; // мы уже здесь
            } else if (id == R.id.nav_stats) {
                intent = new Intent(FavoriteFoodActivity.this, StatisticsActivity.class);
            } else return false;


            intent.putExtra("selected_date", selectedCalendar.getTimeInMillis());
            startActivity(intent);
            return true;
        });

        bottomNav.setSelectedItemId(R.id.nav_fav_food);
    }

    private void loadFavorites() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("usersFood")
                .document(userId)
                .collection("favorite_food")
                .get()
                .addOnSuccessListener(snapshot -> {
                    favoriteList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");

                        favoriteList.add(new ProductItem(id, name));
                    }
                    favAdapter.notifyDataSetChanged();
                });
    }

    private void showAddFoodDialog(String productId) {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_food, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        TextView nameText = view.findViewById(R.id.dialogProductName);
        TextView proteinText = view.findViewById(R.id.dialogProteinText);
        TextView fatText = view.findViewById(R.id.dialogFatText);
        TextView carbText = view.findViewById(R.id.dialogCarbText);
        TextView fiberText = view.findViewById(R.id.dialogFiberText);

        EditText gramsInput = view.findViewById(R.id.dialogGramsInput);
        Button btn100g = view.findViewById(R.id.dialogBtn100g);
        Button btnAddFavorite = view.findViewById(R.id.dialogAddFavorite);
        Button btnAddMeal = view.findViewById(R.id.dialogAddMeal);

        btn100g.setOnClickListener(v -> gramsInput.setText("100"));

        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }
                    String name = doc.getString("name");
                    float protein = doc.getDouble("protein").floatValue();
                    float fat = doc.getDouble("fat").floatValue();
                    float carb = doc.getDouble("carb").floatValue();
                    float fiber = doc.getDouble("fiber").floatValue();

                    nameText.setText(name);
                    proteinText.setText("Белки: " + protein);
                    fatText.setText("Жиры: " + fat);
                    carbText.setText("Углеводы: " + carb);
                    fiberText.setText("Клетчатка: " + fiber);

                    // Добавление в избранное
                    btnAddFavorite.setOnClickListener(v -> {
                        Map<String, Object> fav = new HashMap<>();
                        fav.put("name", name);
                        fav.put("calories", doc.getDouble("calories"));
                        fav.put("protein", protein);
                        fav.put("fat", fat);
                        fav.put("carb", carb);
                        fav.put("fiber", fiber);

                        db.collection("usersFood")
                                .document(currentUser.getUid())
                                .collection("favorite_food")
                                .document(productId)
                                .set(fav);

                        Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                    });

                    // Добавление в дневное меню на выбранный день
                    btnAddMeal.setOnClickListener(v -> {
                        String gramsStr = gramsInput.getText().toString().trim();
                        if (gramsStr.isEmpty()) {
                            Toast.makeText(this, "Введите граммовку", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        float grams = Float.parseFloat(gramsStr);
                        float multiplier = grams / 100f;

                        Map<String, Object> meal = new HashMap<>();
                        meal.put("name", name);
                        meal.put("grams", grams);
                        meal.put("calories", doc.getDouble("calories") * multiplier);
                        meal.put("protein", protein * multiplier);
                        meal.put("fat", fat * multiplier);
                        meal.put("carb", carb * multiplier);
                        meal.put("fiber", fiber * multiplier);
                        meal.put("timestamp", System.currentTimeMillis());

                        String dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(selectedCalendar.getTime());

                        db.collection("dailyMeals")
                                .document(currentUser.getUid())
                                .collection(dayKey)
                                .add(meal)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Добавлено!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                    });
                });

        dialog.show();
    }

    private void setupSearch() {
        favSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();


                if (query.isEmpty()) {
                    loadFavorites();
                    return;
                }

                List<ProductItem> filtered = new ArrayList<>();
                for (ProductItem item : favoriteList) {
                    if (item.getName().toLowerCase().contains(query)) {
                        filtered.add(item);
                    }
                }

                favAdapter.updateList(filtered);
            }
        });
    }
}
