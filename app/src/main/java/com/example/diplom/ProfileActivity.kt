package com.example.diplom

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.diplom.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat


class ProfileActivity : AppCompatActivity() {

    private lateinit var emailText: TextView
    private lateinit var weightText: TextView
    private lateinit var heightText: TextView
    private lateinit var ageText: TextView
    private lateinit var genderText: TextView
    private lateinit var goalText: TextView

    private lateinit var editBtn: ImageButton
    private lateinit var logoutBtn: Button
    private lateinit var adminBtn: Button

    private var currentUser: FirebaseUser? = null
    private val db = FirebaseFirestore.getInstance()

    private lateinit var selectedCalendar: Calendar


    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            if (barcode != null) {
                // 2. Передаем штрих-код в MainActivity, чтобы она открыла нужный диалог
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SCAN_RESULT_FROM_STATS", barcode)
                    putExtra("selected_date", selectedCalendar.timeInMillis)
                    // Очищаем стек, чтобы не плодить копии MainActivity
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val home = findViewById<ImageView>(R.id.home)
        val fav = findViewById<ImageView>(R.id.fav)
        val scan = findViewById<ImageView>(R.id.scan)
        val food = findViewById<ImageView>(R.id.food)
        val stats = findViewById<ImageView>(R.id.stats)

        // 📌 Home
        home.setOnClickListener {
            setActiveTab(home)
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        // 📌 Profile (текущая страница)
        fav.setOnClickListener {
            setActiveTab(fav)
            // уже тут → ничего не делаем
        }

        // 📌 Scan
        scan.setOnClickListener {
            setActiveTab(scan)
            val intent = Intent(this, ScannerActivity::class.java)
            scanLauncher.launch(intent)
        }

        // 📌 Food
        food.setOnClickListener {
            setActiveTab(food)
            startActivity(Intent(this, FavoriteFoodActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        // 📌 Stats
        stats.setOnClickListener {
            setActiveTab(stats)
            startActivity(Intent(this, StatisticsActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        // 🔥 стартовое состояние (профиль активен)
        setActiveTab(fav)

        // ===== остальная твоя логика =====

        emailText = findViewById(R.id.emailText)
        weightText = findViewById(R.id.weightText)
        heightText = findViewById(R.id.heightText)
        ageText = findViewById(R.id.ageText)
        genderText = findViewById(R.id.genderText)
        goalText = findViewById(R.id.goalText)

        editBtn = findViewById(R.id.editBtn)
        logoutBtn = findViewById(R.id.logoutBtn)
        adminBtn = findViewById(R.id.adminBtn)

        currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            emailText.text = it.email
            loadUserData()
            checkAdminRole()
        }

        editBtn.setOnClickListener {
            showEditProfileDialog()
        }

        logoutBtn.setOnClickListener {
            logout()
        }

        findViewById<ImageView>(R.id.avatarImage).setOnClickListener {
            showAvatarDialog()
        }

        findViewById<Button>(R.id.createBtn).setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            showCreateGoalDialog(uid)
        }

        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }
    }

    private fun showEditProfileDialog() {

        val uid = currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val view = layoutInflater.inflate(R.layout.dialog_prof, null)

                val age = view.findViewById<EditText>(R.id.inputAge)
                val weight = view.findViewById<EditText>(R.id.inputWeight)
                val height = view.findViewById<EditText>(R.id.inputHeight)
                val genderSpinner = view.findViewById<Spinner>(R.id.genderSpinner)
                val saveBtn = view.findViewById<Button>(R.id.saveBtn)

                val genders = listOf("Мужской", "Женский")
                genderSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)

                age.setText((doc["age"] as? Number)?.toInt()?.toString() ?: "")
                weight.setText((doc["weight"] as? Number)?.toInt()?.toString() ?: "")
                height.setText((doc["height"] as? Number)?.toInt()?.toString() ?: "")

                val gender = doc.getString("gender")
                val genderIndex = genders.indexOf(gender)
                if (genderIndex != -1) genderSpinner.setSelection(genderIndex)

                val dialog = AlertDialog.Builder(this)
                    .setView(view)
                    .create()

                saveBtn.setOnClickListener {

                    val updates = mutableMapOf<String, Any>()

                    age.text.toString().toIntOrNull()?.let { updates["age"] = it }
                    weight.text.toString().toIntOrNull()?.let { updates["weight"] = it }
                    height.text.toString().toIntOrNull()?.let { updates["height"] = it }

                    updates["gender"] = genderSpinner.selectedItem.toString()

                    db.collection("users")
                        .document(uid)
                        .update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Обновлено", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadUserData()
                        }
                }

                dialog.show()
            }
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
                val goal = (doc["goal"] as? Number)?.toInt()
                val gender = doc.getString("gender")

                weightText.text = "${weight ?: "-"} кг"
                heightText.text = "${height ?: "-"} см"
                ageText.text = "${age ?: "-"} лет"
                goalText.text = "${goal ?: "-"} кг"
                genderText.text = gender ?: "Не указан"

                val avatarImage = findViewById<ImageView>(R.id.avatarImage)

                val avatarIndex = (doc["avatar"] as? Number)?.toInt() ?: 0

                val avatars = listOf(
                    R.drawable.png1,
                    R.drawable.png1,
                    R.drawable.png2,
                    R.drawable.png3,
                    R.drawable.png4,
                    R.drawable.png5,
                    R.drawable.png6
                )

                avatarImage.setImageResource(avatars[avatarIndex])
            }
    }

    private fun checkAdminRole() {

        val email = currentUser?.email ?: return

        db.collection("worker")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->

                for (doc in documents) {
                    val role = doc.getString("role")

                    if (role == "admin") {
                        adminBtn.visibility = View.VISIBLE

                        adminBtn.setOnClickListener {
                            startActivity(Intent(this, AdminActivity::class.java))
                        }
                    }
                }
            }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
    }


    private fun showAvatarDialog() {

        val avatars = listOf(
            R.drawable.png1,
            R.drawable.png1,
            R.drawable.png2,
            R.drawable.png3,
            R.drawable.png4,
            R.drawable.png5,
            R.drawable.png6
        )

        val view = layoutInflater.inflate(R.layout.dialog_choose_png, null)
        val container = view.findViewById<LinearLayout>(R.id.avatarContainer)

        // Создаём AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        avatars.forEachIndexed { index, resId ->
            val img = ImageView(this).apply {
                setImageResource(resId)
                layoutParams = LinearLayout.LayoutParams(600, 600).apply { // большие картинки
                    setMargins(0, 16, 0, 16)
                }
                clipToOutline = true
                background = ContextCompat.getDrawable(context, R.drawable.circle_bg)
                scaleType = ImageView.ScaleType.CENTER_CROP

                setOnClickListener {
                    // Сохраняем выбор в БД
                    db.collection("users")
                        .document(currentUser!!.uid)
                        .update("avatar", index)
                        .addOnSuccessListener {
                            loadUserData()
                            dialog.dismiss()
                        }
                }
            }

            container.addView(img)
        }

        dialog.show()
    }
    private fun showCreateGoalDialog(userId: String) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_new_goal, null)

        val inputNewGoal = dialogView.findViewById<EditText>(R.id.inputNewGoal)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnHold = dialogView.findViewById<Button>(R.id.btnHold)

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->

                val currentWeight = (doc.getDouble("weight") ?: 0.0).toInt()

                inputNewGoal.setText(currentWeight.toString())
                btnSave.isEnabled = true

                inputNewGoal.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        btnSave.isEnabled = s.toString().trim().isNotEmpty()
                    }
                })

                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                btnSave.setOnClickListener {
                    val newGoal = inputNewGoal.text.toString().toLongOrNull()

                    if (newGoal == null) {
                        Toast.makeText(this, "Введите корректное число", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (newGoal == currentWeight.toLong()) {
                        Toast.makeText(this, "Цель не может быть равна текущему весу", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    val need = if (newGoal > currentWeight) "up" else "down"

                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "goal" to newGoal,
                                "need" to need,
                                "startWeight" to currentWeight
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Новая цель установлена", Toast.LENGTH_SHORT).show()
                            loadUserData() // обновляем UI
                            dialog.dismiss()
                        }
                }

                btnHold.setOnClickListener {
                    db.collection("users").document(userId)
                        .update(
                            mapOf(
                                "need" to "norml",
                                "goal" to currentWeight,
                                "startWeight" to currentWeight
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Режим удержания включён", Toast.LENGTH_SHORT).show()
                            loadUserData()
                            dialog.dismiss()
                        }
                }

                dialog.show()
            }
    }
    private fun setActiveTab(active: ImageView) {

        val tabs = listOf(
            findViewById<ImageView>(R.id.home),
            findViewById<ImageView>(R.id.fav),
            findViewById<ImageView>(R.id.scan),
            findViewById<ImageView>(R.id.food),
            findViewById<ImageView>(R.id.stats)
        )

        tabs.forEach {
            it.background = null
            it.clearColorFilter()
            it.imageTintList = null
        }

        active.background = ContextCompat.getDrawable(this, R.drawable.bg_blue_circle)
    }
}