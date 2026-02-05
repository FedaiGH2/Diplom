package com.example.diplom.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.diplom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerBtn, backToLoginBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerBtn = findViewById(R.id.registerBtn);
        backToLoginBtn = findViewById(R.id.backToLoginBtn);

        registerBtn.setOnClickListener(v -> registerUser());
        backToLoginBtn.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirm = confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = authResult.getUser();
                    if (user != null) showUserDetailsDialog(user);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showUserDetailsDialog(FirebaseUser user) {
        if (user == null) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        EditText weightInput = new EditText(this);
        weightInput.setHint("Ваш вес (кг)");
        weightInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText heightInput = new EditText(this);
        heightInput.setHint("Рост (см)");
        heightInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText ageInput = new EditText(this);
        ageInput.setHint("Возраст");
        ageInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        Button genderBtn = new Button(this);
        genderBtn.setText("Выберите пол");
        final String[] selectedGender = {""};
        genderBtn.setOnClickListener(v -> {
            String[] options = {"Мужской", "Женский"};
            new AlertDialog.Builder(this)
                    .setTitle("Выберите пол")
                    .setItems(options, (dialog, which) -> {
                        selectedGender[0] = options[which];
                        genderBtn.setText("Пол: " + selectedGender[0]);
                    })
                    .show();
        });

        layout.addView(weightInput);
        layout.addView(heightInput);
        layout.addView(ageInput);
        layout.addView(genderBtn);

        new AlertDialog.Builder(this)
                .setTitle("Введите данные о себе")
                .setView(layout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String weightStr = weightInput.getText().toString().trim();
                    String heightStr = heightInput.getText().toString().trim();
                    String ageStr = ageInput.getText().toString().trim();
                    String genderStr = selectedGender[0];

                    if (weightStr.isEmpty() || heightStr.isEmpty() || ageStr.isEmpty() || genderStr.isEmpty()) {
                        Toast.makeText(this, "Введите все данные", Toast.LENGTH_SHORT).show();
                        return;
                    }


                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", user.getEmail());
                    userData.put("weight", weightStr);
                    userData.put("height", heightStr);
                    userData.put("age", ageStr);
                    userData.put("gender", genderStr);
                    userData.put("goal", 0);
                    userData.put("avatar", 0);

                    db.collection("users")
                            .document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setCancelable(false)
                .show();
    }
}
