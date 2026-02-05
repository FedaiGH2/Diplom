package com.example.diplom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;




public class AddFoodActivity extends AppCompatActivity {

    private EditText gramsInput;
    private Button addBtn;

    private String productId;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        gramsInput = findViewById(R.id.amountInput);
        addBtn = findViewById(R.id.btnAddMeal);
        Button btn100g = findViewById(R.id.btn100g);


        btn100g.setOnClickListener(v -> gramsInput.setText("100"));


        addBtn.setOnClickListener(v -> addMeal());
        TextView productNameText = findViewById(R.id.productNameText);
        TextView proteinText = findViewById(R.id.proteinText);
        TextView fatText = findViewById(R.id.fatText);
        TextView carbText = findViewById(R.id.carbText);
        TextView fiberText = findViewById(R.id.fiberText);


        productId = getIntent().getStringExtra("productId");
        if (productId == null) {
            Toast.makeText(this, "Продукт не выбран", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name = doc.getString("name");
                    float protein = doc.getDouble("protein").floatValue();
                    float fat = doc.getDouble("fat").floatValue();
                    float carb = doc.getDouble("carb").floatValue();
                    float fiber = doc.getDouble("fiber").floatValue();

                    productNameText.setText(name);
                    proteinText.setText("Белки: " + protein + " г на 100 г");
                    fatText.setText("Жиры: " + fat + " г на 100 г");
                    carbText.setText("Углеводы: " + carb + " г на 100 г");
                    fiberText.setText("Клетчатка: " + fiber + " г на 100 г");
                });

        addBtn.setOnClickListener(v -> addMeal());

        Button addFavoriteBtn = findViewById(R.id.btnAddFavorite);

        addFavoriteBtn.setOnClickListener(v -> {
            if (currentUser == null) return;

            String userId = currentUser.getUid();
            Map<String, Object> favorite = new HashMap<>();
            favorite.put("name", productNameText.getText().toString());


            db.collection("products").document(productId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            favorite.put("calories", doc.getDouble("calories"));
                            favorite.put("protein", doc.getDouble("protein"));
                            favorite.put("fat", doc.getDouble("fat"));
                            favorite.put("carb", doc.getDouble("carb"));
                            favorite.put("fiber", doc.getDouble("fiber"));

                            db.collection("usersFood")
                                    .document(userId)
                                    .collection("favorite_food")
                                    .document(productId)
                                    .set(favorite)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Продукт добавлен в избранное", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Ошибка добавления в избранное", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
        });
    }

    private void addMeal() {
        if (currentUser == null) return;

        String gramsStr = gramsInput.getText().toString().trim();
        if (gramsStr.isEmpty()) {
            Toast.makeText(this, "Введите граммовку", Toast.LENGTH_SHORT).show();
            return;
        }

        float grams;
        try {
            grams = Float.parseFloat(gramsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите корректное число", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String name = doc.getString("name");
                    float calories = doc.getDouble("calories").floatValue() * grams / 100f;
                    float protein = doc.getDouble("protein").floatValue() * grams / 100f;
                    float fat = doc.getDouble("fat").floatValue() * grams / 100f;
                    float carb = doc.getDouble("carb").floatValue() * grams / 100f;
                    float fiber = doc.getDouble("fiber").floatValue() * grams / 100f;

                    Map<String, Object> meal = new HashMap<>();
                    meal.put("name", name);
                    meal.put("grams", grams);
                    meal.put("calories", calories);
                    meal.put("protein", protein);
                    meal.put("fat", fat);
                    meal.put("carb", carb);
                    meal.put("fiber", fiber);
                    meal.put("timestamp", System.currentTimeMillis());

                    db.collection("dailyMeals").document(userId)
                            .collection(today)
                            .add(meal)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Продукт добавлен", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Ошибка добавления продукта", Toast.LENGTH_SHORT).show()
                            );
                });
    }
}
