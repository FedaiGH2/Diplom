package com.example.diplom.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Инициализация элементов
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        registerBtn = findViewById(R.id.registerBtn)
        backToLoginBtn = findViewById(R.id.backToLoginBtn)

        // Кнопка регистрации
        registerBtn.setOnClickListener { registerUser() }

        // Кнопка возврата на логин
        backToLoginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirm = confirmPasswordInput.text.toString().trim()

        // Проверки
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

        // Создание пользователя
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
        val view = layoutInflater.inflate(R.layout.dialog_register, null)

        val age = view.findViewById<EditText>(R.id.inputAge)
        val weight = view.findViewById<EditText>(R.id.inputWeight)
        val height = view.findViewById<EditText>(R.id.inputHeight)
        val goal = view.findViewById<EditText>(R.id.inputGoal)
        val genderSpinner = view.findViewById<Spinner>(R.id.genderSpinner)
        val saveBtn = view.findViewById<Button>(R.id.saveBtn)

        val genders = listOf("Мужской", "Женский")
        genderSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            genders
        )

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        saveBtn.setOnClickListener {

            val ageVal = age.text.toString().trim().toIntOrNull()
            val weightVal = weight.text.toString().trim().toIntOrNull()
            val heightVal = height.text.toString().trim().toIntOrNull()
            val goalVal = goal.text.toString().trim().toIntOrNull()

            if (ageVal == null || weightVal == null || heightVal == null || goalVal == null) {
                Toast.makeText(this, "Заполните все поля корректно", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (goalVal < 30 || goalVal > 300) {
                Toast.makeText(this, "Введите корректный целевой вес", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = genderSpinner.selectedItem.toString()

            // 🔥 АВТО ЛОГИКА
            val needValue = when {
                weightVal > goalVal -> "down"
                weightVal < goalVal -> "up"
                else -> "norml"
            }

            val userData = hashMapOf(
                "email" to user.email,
                "age" to ageVal,
                "weight" to weightVal,
                "startWeight" to weightVal,
                "height" to heightVal,
                "gender" to gender,
                "goal" to goalVal,
                "need" to needValue,
                "avatar" to 0
            )

            db.collection("users")
                .document(user.uid)
                .set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }
}