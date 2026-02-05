package com.example.diplom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.example.diplom.MainActivity;
import androidx.appcompat.app.AppCompatActivity;

import com.example.diplom.logic.ProfileLogic;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;


public class ProfileActivity extends AppCompatActivity {

    private TextView emailText, weightText, genderText, heightText, ageText;
    private Button changeWeightBtn, changeHeightBtn, changeAgeBtn, logoutBtn;

    private FirebaseUser currentUser;
    private ProfileLogic profileLogic;
    private Calendar selectedCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        long calendarMillis = getIntent().getLongExtra("selected_date", System.currentTimeMillis());
        selectedCalendar = Calendar.getInstance();
        selectedCalendar.setTimeInMillis(calendarMillis);

        emailText = findViewById(R.id.emailText);
        weightText = findViewById(R.id.weightText);
        heightText = findViewById(R.id.heightText);
        ageText = findViewById(R.id.ageText);
        genderText = findViewById(R.id.genderText);

        changeWeightBtn = findViewById(R.id.changeWeightBtn);
        changeHeightBtn = findViewById(R.id.changeHeightBtn);
        changeAgeBtn = findViewById(R.id.changeAgeBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        profileLogic = new ProfileLogic(this, currentUser);

        if (currentUser != null) {
            emailText.setText("Email: " + currentUser.getEmail());
            loadUserData();
        }


        changeWeightBtn.setOnClickListener(v -> profileLogic.showChangeValueDialog("weight", weightText));
        changeHeightBtn.setOnClickListener(v -> profileLogic.showChangeValueDialog("height", heightText));
        changeAgeBtn.setOnClickListener(v -> profileLogic.showChangeValueDialog("age", ageText));
        logoutBtn.setOnClickListener(v -> profileLogic.logout());


        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent;

            if (id == R.id.nav_home) intent = new Intent(ProfileActivity.this, MainActivity.class);
            else if (id == R.id.nav_fav_activity) return true;
            else if (id == R.id.nav_scan) return true;
            else if (id == R.id.nav_fav_food) intent = new Intent(ProfileActivity.this, FavoriteFoodActivity.class);
            else if (id == R.id.nav_stats) intent = new Intent(ProfileActivity.this, StatisticsActivity.class);
            else return false;

            intent.putExtra("selected_date", selectedCalendar.getTimeInMillis());
            startActivity(intent);
            return true;
        });

        bottomNav.setSelectedItemId(R.id.nav_fav_activity);
    }

    private void loadUserData() {
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;


                    String weightStr = toStringSafe(doc.get("weight"));
                    String heightStr = toStringSafe(doc.get("height"));
                    String ageStr = toStringSafe(doc.get("age"));
                    String gender = doc.getString("gender");

                    weightText.setText("Вес: " + (!weightStr.isEmpty() ? weightStr : "-"));
                    heightText.setText("Рост: " + (!heightStr.isEmpty() ? heightStr : "-"));
                    ageText.setText("Возраст: " + (!ageStr.isEmpty() ? ageStr : "-"));
                    genderText.setText("Пол: " + (gender != null ? gender : "-"));


                    if (currentUser != null) {
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.getUid())
                                .update("weight", weightStr, "height", heightStr, "age", ageStr);
                    }
                });
    }

    private String toStringSafe(Object obj) {
        if (obj == null) return "";
        if (obj instanceof Number) return String.valueOf(((Number) obj).intValue());
        if (obj instanceof String) return (String) obj;
        return obj.toString();
    }
}