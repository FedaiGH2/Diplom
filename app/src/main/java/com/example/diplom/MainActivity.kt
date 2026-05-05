package com.example.diplom

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.content.Context
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diplom.adapters.SearchAdapter
import com.example.diplom.adapters.TodayMealAdapter
import com.example.diplom.databinding.ActivityMainBinding
import com.example.diplom.databinding.AddFoodToBdBinding
import com.example.diplom.databinding.DialogAddFoodBinding
import com.example.diplom.models.MealItem
import com.example.diplom.models.ProductItem
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val todayMeals = mutableListOf<MealItem>()
    private lateinit var todayAdapter: TodayMealAdapter

    private val searchList = mutableListOf<ProductItem>()
    private lateinit var searchAdapter: SearchAdapter

    private var currentCalendar = Calendar.getInstance()
    private var goalDialogShownForCurrentGoal = false
    private var CALORIES_NORM = 2000f
    private var PROTEIN_NORM = 100f
    private var FAT_NORM = 70f
    private var CARB_NORM = 300f
    private var FIBER_NORM = 35f

    // 1. ЛАУНЧЕР ДЛЯ ПОЛУЧЕНИЯ РЕЗУЛЬТАТА ИЗ SCANNER_ACTIVITY
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            barcode?.let { handleBarcodeResult(it) }
        }
    }

    // Лаунчер для разрешений
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openScannerActivity()
        else Toast.makeText(this, "Нужно разрешение на камеру", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // --- DATE ---
        val selectedDateMillis = intent.getLongExtra("selected_date", -1)
        if (selectedDateMillis != -1L) {
            currentCalendar.timeInMillis = selectedDateMillis
        }

        // --- INIT UI ---
        updateDayUI()
        setupTodayMeals()
        setupSearch()
        setupDayNavigation()
        setupWeightProgress()

        // --- SEARCH LIST ---
        binding.searchResultsRecycler.layoutManager =
            LinearLayoutManager(this)

        searchAdapter = SearchAdapter(
            this,
            searchList,
            object : SearchAdapter.OnProductClickListener {
                override fun onProductClick(product: ProductItem) {
                    showAddFoodDialog(product.id)
                    binding.searchResultsRecycler.visibility = View.GONE
                    binding.searchInput.setText("")
                }
            }
        )

        binding.searchResultsRecycler.adapter = searchAdapter

        // --- EDIT WEIGHT ---
        binding.btnEditWeight.setOnClickListener {
            val current =
                (binding.currentWeightText.text.toString().split(" ")[1]).toFloatOrNull() ?: 0f
            showEditWeightDialog(current)
        }

        // --- HANDLE BARCODE FROM OTHER ACTIVITY ---
        handleIncomingBarcode(intent)

        // =========================================================
        //                BOTTOM BAR (CUSTOM)
        // =========================================================

        val home = findViewById<ImageView>(R.id.home)
        val fav = findViewById<ImageView>(R.id.fav)
        val scan = findViewById<ImageView>(R.id.scan)
        val food = findViewById<ImageView>(R.id.food)
        val stats = findViewById<ImageView>(R.id.stats)

        home.setOnClickListener {
            setActiveTab(home)
        }

        fav.setOnClickListener {
            setActiveTab(fav)
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        scan.setOnClickListener {
            setActiveTab(scan)
            startScan()
        }

        food.setOnClickListener {
            setActiveTab(food)
            startActivity(Intent(this, FavoriteFoodActivity::class.java))
        }

        stats.setOnClickListener {
            setActiveTab(stats)
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        // --- DEFAULT ACTIVE TAB ---
        setActiveTab(home)
    }

    private fun setupTodayMeals() {
        todayAdapter = TodayMealAdapter(this, todayMeals, currentCalendar)
        binding.todayMealsRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = todayAdapter
        }
    }

    private fun setupDayNavigation() {
        binding.prevDayBtn.setOnClickListener { changeDay(-1) }
        binding.nextDayBtn.setOnClickListener { changeDay(1) }
    }

    private fun changeDay(delta: Int) {
        currentCalendar.add(Calendar.DAY_OF_MONTH, delta)
        updateDayUI()
        todayAdapter.setCurrentCalendar(currentCalendar)
        loadDailyMeals()
    }

    private fun updateDayUI() {
        val sdf = SimpleDateFormat("EEEE dd-MM-yyyy", Locale("ru"))
        binding.dayOfWeekText.text = sdf.format(currentCalendar.time)
    }



    private fun navigateTo(target: Class<*>) {
        val intent = Intent(this, target)
        intent.putExtra("selected_date", currentCalendar.timeInMillis)
        startActivity(intent)
    }

    private fun setupSearch() {
        searchAdapter = SearchAdapter(this, searchList, object: SearchAdapter.OnProductClickListener {
            override fun onProductClick(product: ProductItem) {
                showAddFoodDialog(product.id)
                binding.searchResultsRecycler.visibility = View.GONE
                binding.searchInput.setText("")
            }
        })

        binding.searchResultsRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
        }

        lifecycleScope.launch {
            val searchFlow = callbackFlow<String> {
                val watcher = object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        trySend(s.toString())
                    }
                }
                binding.searchInput.addTextChangedListener(watcher)
                awaitClose { binding.searchInput.removeTextChangedListener(watcher) }
            }

            searchFlow.debounce(300).collectLatest { query ->
                searchList.clear()
                if (query.isBlank()) {
                    withContext(Dispatchers.Main) {
                        searchAdapter.notifyDataSetChanged()
                        binding.searchResultsRecycler.visibility = View.GONE
                    }
                    return@collectLatest
                }

                try {
                    val lowerQuery = query.lowercase()
                    val snapshot = db.collection("products").get().await() // Оптимизировано (без orderBy для теста)

                    snapshot.documents.forEach { doc ->
                        val name = doc.getString("name") ?: return@forEach
                        if (!name.lowercase().contains(lowerQuery)) return@forEach

                        searchList.add(ProductItem(
                            doc.id, name,
                            doc.getDouble("calories")?.toFloat() ?: 0f,
                            doc.getDouble("protein")?.toFloat() ?: 0f,
                            doc.getDouble("fat")?.toFloat() ?: 0f,
                            doc.getDouble("carb")?.toFloat() ?: 0f,
                            doc.getDouble("fiber")?.toFloat() ?: 0f
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        searchAdapter.notifyDataSetChanged()
                        binding.searchResultsRecycler.visibility = if (searchList.isEmpty()) View.GONE else View.VISIBLE
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    // --- ОБНОВЛЕННАЯ СЕКЦИЯ СКАНЕРА ---

    private fun startScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openScannerActivity()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openScannerActivity() {
        val intent = Intent(this, ScannerActivity::class.java)
        scanLauncher.launch(intent)
    }

    private fun handleBarcodeResult(barcode: String) {
        db.collection("products").document(barcode).get().addOnSuccessListener { doc ->

            if (doc.exists()) {

                showAddFoodDialog(barcode)

            } else {

                showNotFoundDialog(barcode)

            }

        }
    }

    // --- ДИАЛОГИ И ЛОГИКА ЕДЫ ---

    fun showAddFoodDialog(productId: String) {
        // Используем актуальную проверку пользователя
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogBtn100g.setOnClickListener { dialogBinding.dialogGramsInput.setText("100") }

        lifecycleScope.launch {
            val doc = db.collection("products").document(productId).get().await()
            if (!doc.exists()) {
                Toast.makeText(this@MainActivity, "Продукт не найден в базе", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@launch
            }

            val name = doc.getString("name") ?: ""
            val protein = doc.getDouble("protein")?.toFloat() ?: 0f
            val fat = doc.getDouble("fat")?.toFloat() ?: 0f
            val carb = doc.getDouble("carb")?.toFloat() ?: 0f
            val fiber = doc.getDouble("fiber")?.toFloat() ?: 0f
            val calories = doc.getDouble("calories")?.toFloat() ?: 0f

            dialogBinding.dialogProductName.text = name
            dialogBinding.dialogProteinText.text = "Белки: ${protein.roundToInt()}"
            dialogBinding.dialogFatText.text = "Жиры: ${fat.roundToInt()}"
            dialogBinding.dialogCarbText.text = "Углеводы: ${carb.roundToInt()}"
            dialogBinding.dialogFiberText.text = "Клетчатка: ${fiber.roundToInt()}"
            dialogBinding.dialogCaloriesText.text = "Калории: ${calories.roundToInt()}"

            // Проверка избранного
            var isFavorite = false
            val favDoc = db.collection("usersFood").document(user.uid)
                .collection("favorite_food").document(productId).get().await()
            if (favDoc.exists()) isFavorite = true

            dialogBinding.dialogAddFavorite.icon = ContextCompat.getDrawable(
                this@MainActivity,
                if (isFavorite) R.drawable.star else R.drawable.star_grey
            )

            // Логика кнопки избранного
            dialogBinding.dialogAddFavorite.setOnClickListener {
                val favRef = db.collection("usersFood").document(user.uid)
                    .collection("favorite_food").document(productId)

                if (isFavorite) {
                    favRef.delete().addOnSuccessListener {
                        isFavorite = false
                        dialogBinding.dialogAddFavorite.icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.star_grey)
                        Toast.makeText(this@MainActivity, "Удалено из избранного", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val favData = mapOf(
                        "name" to name, "calories" to calories, "protein" to protein,
                        "fat" to fat, "carb" to carb, "fiber" to fiber
                    )
                    favRef.set(favData).addOnSuccessListener {
                        isFavorite = true
                        dialogBinding.dialogAddFavorite.icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.star)
                        Toast.makeText(this@MainActivity, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Ошибка при добавлении", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Логика добавления в дневник питания
            dialogBinding.dialogAddMeal.setOnClickListener {
                val gramsStr = dialogBinding.dialogGramsInput.text.toString().trim()
                if (gramsStr.isBlank()) {
                    Toast.makeText(this@MainActivity, "Введите вес продукта", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val grams = gramsStr.toFloat()
                val m = grams / 100f
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

                db.collection("dailyMeals").document(user.uid).collection(todayKey)
                    .add(mapOf(
                        "name" to name, "grams" to grams,
                        "calories" to calories * m, "protein" to protein * m,
                        "fat" to fat * m, "carb" to carb * m, "fiber" to fiber * m,
                        "timestamp" to System.currentTimeMillis()
                    )).addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "Добавлено в дневник", Toast.LENGTH_SHORT).show()
                        loadDailyMeals()
                        dialog.dismiss()
                    }.addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Ошибка записи в дневник", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        dialog.show()
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleIncomingBarcode(intent)
    }

    override fun onResume() {
        super.onResume()
        loadDailyMeals()
        loadUserNorms()
        setupWeightProgress()

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val weight = doc.getDouble("weight")?.toFloat() ?: return@addOnSuccessListener
                val goal = doc.getDouble("goal")?.toFloat() ?: return@addOnSuccessListener
                val need = doc.getString("need") ?: "norml"

                val shouldShowDialog = when (need) {
                    "up" -> weight >= goal
                    "down" -> weight <= goal
                    else -> false
                }

                if (shouldShowDialog && !goalDialogShownForCurrentGoal) {
                    goalDialogShownForCurrentGoal = true
                    showGoalReachedDialog(userId)
                }

                if (!shouldShowDialog) {
                    goalDialogShownForCurrentGoal = false
                }
            }
    }

    fun loadDailyMeals() {
        if (currentUser == null) return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

        db.collection("dailyMeals").document(currentUser.uid).collection(todayKey)
            .get().addOnSuccessListener { snapshot ->
                todayMeals.clear()
                var totalCal = 0f; var totalProt = 0f; var totalFat = 0f; var totalCarb = 0f; var totalFib = 0f

                snapshot.documents.forEach { doc ->
                    val calories = doc.getDouble("calories")?.toFloat() ?: 0f
                    val protein = doc.getDouble("protein")?.toFloat() ?: 0f
                    val fat = doc.getDouble("fat")?.toFloat() ?: 0f // Исправил опечатку
                    val fatReal = doc.getDouble("fat")?.toFloat() ?: 0f
                    val carb = doc.getDouble("carb")?.toFloat() ?: 0f
                    val fiber = doc.getDouble("fiber")?.toFloat() ?: 0f

                    totalCal += calories; totalProt += protein; totalFat += fatReal; totalCarb += carb; totalFib += fiber
                    todayMeals.add(MealItem(doc.id, doc.getString("name") ?: "",
                        doc.getDouble("grams")?.toFloat() ?: 0f, calories, protein, fatReal, carb, fiber))
                }
                todayAdapter.notifyDataSetChanged()
                updateCharts(totalCal, totalProt, totalFat, totalCarb, totalFib)
            }
    }

    private fun updateCharts(calories: Float, protein: Float, fat: Float, carb: Float, fiber: Float) {
        setupPieChart(binding.mainPieChart, calories, CALORIES_NORM, Color.BLUE)
        setupPieChart(binding.proteinChart, protein, PROTEIN_NORM, Color.GREEN)
        setupPieChart(binding.fatChart, fat, FAT_NORM, Color.RED)
        setupPieChart(binding.carbChart, carb, CARB_NORM, Color.MAGENTA)
        setupPieChart(binding.fiberChart, fiber, FIBER_NORM, Color.CYAN)

        binding.mainChartValue.text = "${calories.roundToInt()} / ${CALORIES_NORM.roundToInt()} ккал"
        binding.proteinValue.text = "${protein.roundToInt()} / ${PROTEIN_NORM.roundToInt()} г"
        binding.fatValue.text = "${fat.roundToInt()} / ${FAT_NORM.roundToInt()} г"
        binding.carbValue.text = "${carb.roundToInt()} / ${CARB_NORM.roundToInt()} г"
        binding.fiberValue.text = "${fiber.roundToInt()} / ${FIBER_NORM.roundToInt()} г"
    }

    private fun setupPieChart(chart: com.github.mikephil.charting.charts.PieChart, value: Float, norm: Float, color: Int) {
        val remaining = (norm - value).coerceAtLeast(0f)
        val dataSet = PieDataSet(arrayListOf(PieEntry(value), PieEntry(remaining)), "").apply {
            setColors(color, Color.LTGRAY)
            setDrawValues(false)
        }
        chart.data = PieData(dataSet)
        chart.apply {
            setUsePercentValues(true); setDrawHoleEnabled(true); setHoleRadius(60f)
            legend.isEnabled = false; description.isEnabled = false; setDrawEntryLabels(false)
            invalidate()
        }
    }

    private fun loadUserNorms() {
        if (currentUser == null) return

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->

                val weight = doc.getDouble("weight")?.toFloat() ?: return@addOnSuccessListener
                val height = doc.getDouble("height")?.toFloat() ?: return@addOnSuccessListener
                val age = doc.getLong("age")?.toInt() ?: return@addOnSuccessListener
                val gender = doc.getString("gender") ?: return@addOnSuccessListener
                val need = doc.getString("need") ?: "norml"

                // Базовый обмен (BMR)
                var bmr = if (gender.equals("Мужской", true)) {
                    10 * weight + 6.25f * height - 5 * age + 5
                } else {
                    10 * weight + 6.25f * height - 5 * age - 161
                }

                // Можно добавить коэффициент активности (по желанию)
                val activityMultiplier = 1.4f
                var calories = bmr * activityMultiplier

                // Корректировка под цель
                when (need) {
                    "norml" -> {
                        // поддержание — без изменений
                    }
                    "up" -> {
                        calories += 500f  // профицит для набора
                    }
                    "down" -> {
                        calories -= 500f  // дефицит для похудения
                    }
                }

                CALORIES_NORM = calories

                // БЖУ в зависимости от цели
                when (need) {
                    "up" -> {
                        PROTEIN_NORM = 2.0f * weight
                        FAT_NORM = 1.0f * weight
                        CARB_NORM = 5.5f * weight
                    }
                    "down" -> {
                        PROTEIN_NORM = 2.5f * weight
                        FAT_NORM = 0.8f * weight
                        CARB_NORM = 3.0f * weight
                    }
                    else -> { // norml
                        PROTEIN_NORM = 2.2f * weight
                        FAT_NORM = 1.0f * weight
                        CARB_NORM = 4.0f * weight
                    }
                }

                loadDailyMeals()
            }
    }

    private fun showAddProductDialog(barcode: String) {

        val user = FirebaseAuth.getInstance().currentUser ?: return

        val dialogBinding = AddFoodToBdBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.addProductBtn.setOnClickListener {

            val name = dialogBinding.inputName.text.toString().trim()
            val cal = dialogBinding.inputCalories.text.toString().trim()
            val prot = dialogBinding.inputProtein.text.toString().trim()
            val fat = dialogBinding.inputFat.text.toString().trim()
            val carb = dialogBinding.inputCarb.text.toString().trim()
            val fib = dialogBinding.inputFiber.text.toString().trim()

            if (name.isEmpty() || cal.isEmpty()) {

                Toast.makeText(this, "Введите название и калории", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val product = mutableMapOf<String, Any>()

            product["name"] = name
            product["calories"] = cal.toFloat()
            product["protein"] = prot.toFloatOrNull() ?: 0f
            product["fat"] = fat.toFloatOrNull() ?: 0f
            product["carb"] = carb.toFloatOrNull() ?: 0f
            product["fiber"] = fib.toFloatOrNull() ?: 0f
            product["email"] = user.email ?: ""
            product["timestamp"] = System.currentTimeMillis()

            db.collection("addproduct")
                .document(barcode)
                .set(product)
                .addOnSuccessListener {

                    Toast.makeText(this, "Запрос отправлен администратору", Toast.LENGTH_SHORT).show()

                    dialog.dismiss()

                }
                .addOnFailureListener {

                    Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show()

                }
        }

        dialog.show()
    }

    private fun showNotFoundDialog(barcode: String) {

        val view = layoutInflater.inflate(R.layout.not_found_food, null)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        val addBtn = view.findViewById<Button>(R.id.dialogAddMeal)

        addBtn.setOnClickListener {

            dialog.dismiss()

            showAddProductDialog(barcode)

        }

        dialog.show()
    }


    private fun setupWeightProgress() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userData = document.data ?: return@addOnSuccessListener

                    val startWeight = (userData["startWeight"] as? Number)?.toFloat() ?: 0f
                    val currentWeight = (userData["weight"] as? Number)?.toFloat() ?: startWeight
                    val goalWeight = (userData["goal"] as? Number)?.toFloat() ?: startWeight
                    val need = (userData["need"] as? String) ?: "norml"

                    // 🔥 СКРЫТИЕ / ПОКАЗ БЛОКА
                    if (need == "norml") {
                        binding.weightProgressContainer.visibility = View.GONE
                    } else {
                        binding.weightProgressContainer.visibility = View.VISIBLE
                    }

                    val progressBar = binding.weightProgressBar
                    val startText = binding.startWeightText
                    val currentText = binding.currentWeightText
                    val goalText = binding.goalWeightText

                    startText.text = "Начало: ${startWeight.roundToInt()} кг"
                    currentText.text = "Сейчас: ${currentWeight.roundToInt()} кг"
                    goalText.text = "Цель: ${goalWeight.roundToInt()} кг"

                    val progress = if (goalWeight != startWeight) {
                        ((currentWeight - startWeight) / (goalWeight - startWeight) * 100)
                            .coerceIn(0f, 100f)
                            .toInt()
                    } else 0

                    ObjectAnimator.ofInt(progressBar, "progress", 0, progress).apply {
                        duration = 500
                        start()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка при получении данных пользователя", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showEditWeightDialog(currentWeight: Float) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_weight, null)
        val inputWeight = dialogView.findViewById<EditText>(R.id.inputWeight)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveBtn)

        inputWeight.setText(currentWeight.toInt().toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        saveBtn.setOnClickListener {
            val newWeight = inputWeight.text.toString().toFloatOrNull() ?: return@setOnClickListener

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(uid)
                .update("weight", newWeight)
                .addOnSuccessListener {
                    Toast.makeText(this, "Вес обновлён", Toast.LENGTH_SHORT).show()
                    setupWeightProgress() // Обновляем прогресс

                    // --- Проверка достижения цели после изменения веса ---
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val weight = doc.getDouble("weight")?.toFloat() ?: return@addOnSuccessListener
                            val goal = doc.getDouble("goal")?.toFloat() ?: return@addOnSuccessListener
                            val need = doc.getString("need") ?: "norml"

                            val shouldShowDialog = when (need) {
                                "up" -> weight >= goal
                                "down" -> weight <= goal
                                else -> false
                            }

                            if (shouldShowDialog && !goalDialogShownForCurrentGoal) {
                                goalDialogShownForCurrentGoal = true
                                showGoalReachedDialog( uid)
                            }

                            if (!shouldShowDialog) {
                                goalDialogShownForCurrentGoal = false
                            }
                        }

                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка при обновлении веса", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showGoalReachedDialog(userId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_goal, null)

        val inputNewGoal = dialogView.findViewById<EditText>(R.id.inputNewGoal)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnHold = dialogView.findViewById<Button>(R.id.btnHold)

        // Получаем текущий вес пользователя из БД
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val currentWeight = (doc.getLong("weight") ?: 0L).toInt()

                // Заполняем поле цели текущим весом
                inputNewGoal.setText(currentWeight.toString())
                btnSave.isEnabled = true // сразу доступна

                // TextWatcher на будущее, если пользователь будет менять число
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
                        Toast.makeText(this, "Вы не можете задать цель равную текущему весу.", Toast.LENGTH_LONG).show()
                        Toast.makeText(this, "Используйте удержание веса.", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    // Определяем направление
                    val need = if (newGoal > currentWeight) "up" else "down"

                    db.collection("users").document(userId)
                        .update("goal", newGoal, "need", need)
                        .addOnSuccessListener {
                            // Обновляем startWeight
                            db.collection("users").document(userId)
                                .update("startWeight", currentWeight)
                                .addOnSuccessListener { setupWeightProgress() }
                            dialog.dismiss()
                            Toast.makeText(this, "Цель сохранена", Toast.LENGTH_SHORT).show()
                        }
                }

                btnHold.setOnClickListener {
                    db.collection("users").document(userId)
                        .update("need", "norml")
                        .addOnSuccessListener {
                            setupWeightProgress()
                            dialog.dismiss()
                            Toast.makeText(this, "Удержание веса установлено", Toast.LENGTH_SHORT).show()
                        }
                    db.collection("users").document(userId)
                        .update("startWeight", currentWeight)
                    db.collection("users").document(userId)
                        .update("goal", currentWeight)
                }

                dialog.show()
            }
    }
    private fun handleIncomingBarcode(intent: Intent?) {
        val barcode = intent?.getStringExtra("SCAN_RESULT_FROM_STATS")
        if (barcode != null) {
            handleBarcodeResult(barcode)
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