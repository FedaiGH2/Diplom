package com.example.diplom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private EditText inputName, inputCalories, inputProtein, inputFat, inputCarb, inputFiber;
    private Button addProductBtn;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        db = FirebaseFirestore.getInstance();

        inputName = findViewById(R.id.inputName);
        inputCalories = findViewById(R.id.inputCalories);
        inputProtein = findViewById(R.id.inputProtein);
        inputFat = findViewById(R.id.inputFat);
        inputCarb = findViewById(R.id.inputCarb);
        inputFiber = findViewById(R.id.inputFiber);
        addProductBtn = findViewById(R.id.addProductBtn);


        String barcode = getIntent().getStringExtra("barcode");

        addProductBtn.setOnClickListener(v -> {
            if (barcode != null) {
                addProductToFirebase(barcode);
            } else {
                Toast.makeText(this, "Ошибка: штрихкод отсутствует", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void addProductToFirebase(String barcode) {
        String name = inputName.getText().toString().trim();
        String calStr = inputCalories.getText().toString().trim();
        String proteinStr = inputProtein.getText().toString().trim();
        String fatStr = inputFat.getText().toString().trim();
        String carbStr = inputCarb.getText().toString().trim();
        String fiberStr = inputFiber.getText().toString().trim();

        if (name.isEmpty() || calStr.isEmpty() || proteinStr.isEmpty() || fatStr.isEmpty()
                || carbStr.isEmpty() || fiberStr.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float calories = Float.parseFloat(calStr);
            float protein = Float.parseFloat(proteinStr);
            float fat = Float.parseFloat(fatStr);
            float carb = Float.parseFloat(carbStr);
            float fiber = Float.parseFloat(fiberStr);

            Map<String, Object> product = new HashMap<>();
            product.put("name", name);
            product.put("calories", calories);
            product.put("protein", protein);
            product.put("fat", fat);
            product.put("carb", carb);
            product.put("fiber", fiber);


            db.collection("products").document(barcode).set(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Продукт добавлен", Toast.LENGTH_SHORT).show();
                        finish(); // Возврат в MainActivity
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Введите корректные числа", Toast.LENGTH_SHORT).show();
        }
    }
}

