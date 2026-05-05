package com.example.diplom.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerBtn: Button
    private lateinit var backToLoginBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerBtn = findViewById(R.id.registerBtn)
        backToLoginBtn = findViewById(R.id.backToLoginBtn)

        registerBtn.setOnClickListener { registerUser() }
        backToLoginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun registerUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirm = confirmPasswordInput.text.toString().trim()

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirm) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                val user = authResult.user
                if (user != null) showUserDetailsDialog(user)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUserDetailsDialog(user: FirebaseUser) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val weightInput = EditText(this).apply {
            hint = "Ваш вес (кг)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val heightInput = EditText(this).apply {
            hint = "Рост (см)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val ageInput = EditText(this).apply {
            hint = "Возраст"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val genderBtn = Button(this).apply { text = "Выберите пол" }
        val selectedGender = arrayOf("")
        genderBtn.setOnClickListener {
            val options = arrayOf("Мужской", "Женский")
            AlertDialog.Builder(this)
                .setTitle("Выберите пол")
                .setItems(options) { _, which ->
                    selectedGender[0] = options[which]
                    genderBtn.text = "Пол: ${selectedGender[0]}"
                }
                .show()
        }

        layout.addView(weightInput)
        layout.addView(heightInput)
        layout.addView(ageInput)
        layout.addView(genderBtn)

        AlertDialog.Builder(this)
            .setTitle("Введите данные о себе")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val weight = weightInput.text.toString().trim().toIntOrNull()
                val height = heightInput.text.toString().trim().toIntOrNull()
                val age = ageInput.text.toString().trim().toIntOrNull()
                val genderStr = selectedGender[0]

                if (weight == null || height == null || age == null || genderStr.isEmpty()) {
                    Toast.makeText(this, "Введите корректные данные", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val userData = hashMapOf(
                    "email" to user.email,
                    "weight" to weight,
                    "height" to height,
                    "age" to age,
                    "gender" to genderStr,
                    "goal" to 0,
                    "avatar" to 0
                )

                db.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setCancelable(false)
            .show()
    }
}