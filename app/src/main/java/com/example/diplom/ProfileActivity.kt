package com.example.diplom

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private lateinit var emailText: TextView
    private lateinit var weightText: TextView
    private lateinit var heightText: TextView
    private lateinit var ageText: TextView
    private lateinit var genderText: TextView
    private lateinit var changeWeightBtn: Button
    private lateinit var changeHeightBtn: Button
    private lateinit var changeAgeBtn: Button
    private lateinit var logoutBtn: Button

    private var currentUser: FirebaseUser? = null
    private val db = FirebaseFirestore.getInstance()

    private lateinit var selectedCalendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }

        emailText = findViewById(R.id.emailText)
        weightText = findViewById(R.id.weightText)
        heightText = findViewById(R.id.heightText)
        ageText = findViewById(R.id.ageText)
        genderText = findViewById(R.id.genderText)

        changeWeightBtn = findViewById(R.id.changeWeightBtn)
        changeHeightBtn = findViewById(R.id.changeHeightBtn)
        changeAgeBtn = findViewById(R.id.changeAgeBtn)
        logoutBtn = findViewById(R.id.logoutBtn)

        currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            emailText.text = "Email: ${it.email}"
            loadUserData()
        }

        changeWeightBtn.setOnClickListener { showChangeValueDialog("Вес", weightText) }
        changeHeightBtn.setOnClickListener { showChangeValueDialog("Рост", heightText) }
        changeAgeBtn.setOnClickListener { showChangeValueDialog("Возраст", ageText) }
        logoutBtn.setOnClickListener { logout() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.setOnItemSelectedListener { item ->
            val intent: Intent? = when (item.itemId) {

                R.id.nav_home ->
                    Intent(this, MainActivity::class.java)

                R.id.nav_fav_activity ->
                    return@setOnItemSelectedListener true

                R.id.nav_scan ->
                    return@setOnItemSelectedListener true

                R.id.nav_fav_food ->
                    Intent(this, FavoriteFoodActivity::class.java)

                R.id.nav_stats ->
                    Intent(this, StatisticsActivity::class.java)

                else -> null
            }

            intent?.putExtra("selected_date", selectedCalendar.timeInMillis)
            intent?.let { startActivity(it) }

            true
        }

        bottomNav.selectedItemId = R.id.nav_fav_activity
    }

    private fun logout() {

        FirebaseAuth.getInstance().signOut()

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
    }

    private fun showChangeValueDialog(field: String, textView: TextView) {

        if (currentUser == null) return

        val input = EditText(this).apply {

            inputType = when (field) {

                "weight", "goal", "height", "age" ->
                    InputType.TYPE_CLASS_NUMBER

                "gender" ->
                    InputType.TYPE_CLASS_TEXT

                else ->
                    InputType.TYPE_CLASS_TEXT
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Изменить $field")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->

                val valueStr = input.text.toString().trim()

                if (valueStr.isEmpty()) {
                    Toast.makeText(this, "Введите значение", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val update = mutableMapOf<String, Any>()

                when (field) {

                    "weight", "goal", "height", "age" -> {

                        val intValue = valueStr.toIntOrNull()

                        if (intValue == null) {

                            Toast.makeText(this, "Введите корректное число", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        update[field] = intValue
                    }

                    else ->
                        update[field] = valueStr
                }

                db.collection("users")
                    .document(currentUser!!.uid)
                    .update(update)
                    .addOnSuccessListener {

                        textView.text = "$field: $valueStr"

                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(this, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()

                    }
            }

            .setNegativeButton("Отмена", null)

            .show()
    }

    private fun loadUserData() {

        val uid = currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) return@addOnSuccessListener

                val weight = (doc["weight"] as? Number)?.toInt()
                val height = (doc["height"] as? Number)?.toInt()
                val age = (doc["age"] as? Number)?.toInt()
                val gender = doc.getString("gender")

                weightText.text = "Вес: ${weight ?: "-"}"
                heightText.text = "Рост: ${height ?: "-"}"
                ageText.text = "Возраст: ${age ?: "-"}"
                genderText.text = "Пол: ${gender ?: "-"}"

                val updates = mutableMapOf<String, Any>()

                if (weight == null) updates["weight"] = 0
                if (height == null) updates["height"] = 0
                if (age == null) updates["age"] = 0

                if (updates.isNotEmpty()) {

                    db.collection("users")
                        .document(uid)
                        .update(updates)
                }
            }
    }
}