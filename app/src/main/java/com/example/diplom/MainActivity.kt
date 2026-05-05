package com.example.diplom

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private val todayMeals = mutableListOf<MealItem>()
    private lateinit var todayAdapter: TodayMealAdapter

    private val searchList = mutableListOf<ProductItem>()
    private lateinit var searchAdapter: SearchAdapter

    private var currentCalendar = Calendar.getInstance()

    private var CALORIES_NORM = 2000f
    private var PROTEIN_NORM = 100f
    private var FAT_NORM = 70f
    private var CARB_NORM = 300f
    private var FIBER_NORM = 35f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedDateMillis = intent.getLongExtra("selected_date", -1)
        if (selectedDateMillis != -1L) currentCalendar.timeInMillis = selectedDateMillis

        updateDayUI()
        setupTodayMeals()
        setupBottomNavigation()
        setupSearch()
        setupDayNavigation()
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

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_fav_activity -> { startActivity(ProfileActivity::class.java); true }
                R.id.nav_scan -> { startScan(); true }
                R.id.nav_fav_food -> { startActivity(FavoriteFoodActivity::class.java); true }
                R.id.nav_stats -> { startActivity(StatisticsActivity::class.java); true }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun startActivity(target: Class<*>) {
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

                    // Получаем первую порцию документов (можно поставить лимит)
                    val snapshot = db.collection("products")
                        .orderBy("name")
                        .get()
                        .await()

                    snapshot.documents.forEach { doc ->
                        val name = doc.getString("name") ?: return@forEach

                        // Сравниваем в нижнем регистре, чтобы b и B искало одинаково
                        if (!name.lowercase().startsWith(lowerQuery)) return@forEach

                        searchList.add(
                            ProductItem(
                                doc.id,
                                name,
                                doc.getDouble("calories")?.toFloat() ?: 0f,
                                doc.getDouble("protein")?.toFloat() ?: 0f,
                                doc.getDouble("fat")?.toFloat() ?: 0f,
                                doc.getDouble("carb")?.toFloat() ?: 0f,
                                doc.getDouble("fiber")?.toFloat() ?: 0f
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        searchAdapter.notifyDataSetChanged()
                        binding.searchResultsRecycler.visibility = if (searchList.isEmpty()) View.GONE else View.VISIBLE
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun showAddFoodDialog(productId: String) {
        if (currentUser == null) return
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogBtn100g.setOnClickListener { dialogBinding.dialogGramsInput.setText("100") }

        lifecycleScope.launch {
            val doc = db.collection("products").document(productId).get().await()
            if (!doc.exists()) { Toast.makeText(this@MainActivity, "Продукт не найден", Toast.LENGTH_SHORT).show(); dialog.dismiss(); return@launch }

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

            dialogBinding.dialogAddFavorite.setOnClickListener {
                db.collection("usersFood").document(currentUser.uid)
                    .collection("favorite_food").document(productId)
                    .set(mapOf("name" to name, "calories" to calories, "protein" to protein, "fat" to fat, "carb" to carb, "fiber" to fiber))
                Toast.makeText(this@MainActivity, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
            }

            dialogBinding.dialogAddMeal.setOnClickListener {
                val gramsStr = dialogBinding.dialogGramsInput.text.toString().trim()
                if (gramsStr.isBlank()) { Toast.makeText(this@MainActivity, "Введите граммовку", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                val grams = gramsStr.toFloat()
                val multiplier = grams / 100f
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

                db.collection("dailyMeals").document(currentUser.uid).collection(todayKey)
                    .add(mapOf(
                        "name" to name,
                        "grams" to grams,
                        "calories" to calories * multiplier,
                        "protein" to protein * multiplier,
                        "fat" to fat * multiplier,
                        "carb" to carb * multiplier,
                        "fiber" to fiber * multiplier,
                        "timestamp" to System.currentTimeMillis()
                    )).addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "Добавлено!", Toast.LENGTH_SHORT).show()
                        loadDailyMeals()
                        dialog.dismiss()
                    }
            }
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadDailyMeals()
        loadUserNorms()
    }

    fun loadDailyMeals() {
        if (currentUser == null) return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

        db.collection("dailyMeals").document(currentUser.uid).collection(todayKey)
            .get().addOnSuccessListener { snapshot ->
                todayMeals.clear()
                var totalCalories = 0f
                var totalProtein = 0f
                var totalFat = 0f
                var totalCarb = 0f
                var totalFiber = 0f

                snapshot.documents.forEach { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: return@forEach
                    val calories = doc.getDouble("calories")?.toFloat() ?: 0f
                    val protein = doc.getDouble("protein")?.toFloat() ?: 0f
                    val fat = doc.getDouble("fat")?.toFloat() ?: 0f
                    val carb = doc.getDouble("carb")?.toFloat() ?: 0f
                    val fiber = doc.getDouble("fiber")?.toFloat() ?: 0f
                    val grams = doc.getDouble("grams")?.toFloat() ?: 100f

                    totalCalories += calories
                    totalProtein += protein
                    totalFat += fat
                    totalCarb += carb
                    totalFiber += fiber

                    todayMeals.add(MealItem(id, name, grams, calories, protein, fat, carb, fiber))
                }

                todayAdapter.notifyDataSetChanged()
                updateCharts(totalCalories, totalProtein, totalFat, totalCarb, totalFiber)
            }
    }

    private fun updateCharts(calories: Float, protein: Float, fat: Float, carb: Float, fiber: Float) {
        setupPieChart(binding.mainPieChart, calories, CALORIES_NORM, Color.BLUE)
        setupPieChart(binding.proteinChart, protein, PROTEIN_NORM, Color.GREEN)
        setupPieChart(binding.fatChart, fat, FAT_NORM, Color.RED)
        setupPieChart(binding.carbChart, carb, CARB_NORM, Color.MAGENTA)
        setupPieChart(binding.fiberChart, fiber, FIBER_NORM, Color.CYAN)

        binding.mainChartValue.text = "За сегодня: ${calories.roundToInt()} / ${CALORIES_NORM.roundToInt()} ккал"
        binding.proteinValue.text = "${protein.roundToInt()} / ${PROTEIN_NORM.roundToInt()} г"
        binding.fatValue.text = "${fat.roundToInt()} / ${FAT_NORM.roundToInt()} г"
        binding.carbValue.text = "${carb.roundToInt()} / ${CARB_NORM.roundToInt()} г"
        binding.fiberValue.text = "${fiber.roundToInt()} / ${FIBER_NORM.roundToInt()} г"
    }

    private fun setupPieChart(
        chart: com.github.mikephil.charting.charts.PieChart,
        value: Float,
        norm: Float,
        color: Int
    ) {
        val remaining = (norm - value).coerceAtLeast(0f)
        val entries = arrayListOf(PieEntry(value), PieEntry(remaining))
        val dataSet = PieDataSet(entries, "").apply {
            setColors(color, Color.LTGRAY)
            setDrawValues(false)
        }
        chart.data = PieData(dataSet)

        chart.apply {
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(60f)
            legend.isEnabled = false
            description.isEnabled = false
            setDrawEntryLabels(false)
            invalidate()
        }
    }

    private fun startScan() {
        val intent = Intent(this, BarcodeScannerActivity::class.java)
        startActivityForResult(intent, 456)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 456 && resultCode == RESULT_OK) {
            val barcode = data?.getStringExtra("barcode")
            if (barcode != null) {
                db.collection("products").document(barcode).get().addOnSuccessListener { doc ->
                    if (doc.exists()) showAddFoodDialog(barcode)
                    else showAddProductDialog(barcode)
                }
            }
        }
    }

    private fun loadUserNorms() {
        if (currentUser == null) return
        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
            val weight = doc.getDouble("weight")?.toFloat() ?: return@addOnSuccessListener
            val height = doc.getDouble("height")?.toFloat() ?: return@addOnSuccessListener
            val age = doc.getLong("age")?.toInt() ?: return@addOnSuccessListener
            val gender = doc.getString("gender") ?: return@addOnSuccessListener

            CALORIES_NORM = if (gender.equals("Мужской", true)) 10 * weight + 6.25f * height - 5 * age + 5
            else 10 * weight + 6.25f * height - 5 * age - 161

            PROTEIN_NORM = 2.2f * weight
            FAT_NORM = 1f * weight
            CARB_NORM = 5f * weight

            loadDailyMeals()
        }
    }

    private fun showAddProductDialog(barcode: String) {

        val dialogBinding = AddFoodToBdBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()


        dialogBinding.dialogProduct.text = "Добавление продукта"

        dialogBinding.addProductBtn.setOnClickListener {

            val name = dialogBinding.inputName.text.toString().trim()
            val calStr = dialogBinding.inputCalories.text.toString().trim()
            val proteinStr = dialogBinding.inputProtein.text.toString().trim()
            val fatStr = dialogBinding.inputFat.text.toString().trim()
            val carbStr = dialogBinding.inputCarb.text.toString().trim()
            val fiberStr = dialogBinding.inputFiber.text.toString().trim()

            if (name.isEmpty() || calStr.isEmpty() || proteinStr.isEmpty() ||
                fatStr.isEmpty() || carbStr.isEmpty() || fiberStr.isEmpty()
            ) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {

                val calories = calStr.toFloat()
                val protein = proteinStr.toFloat()
                val fat = fatStr.toFloat()
                val carb = carbStr.toFloat()
                val fiber = fiberStr.toFloat()

                val product = hashMapOf(
                    "name" to name,
                    "calories" to calories,
                    "protein" to protein,
                    "fat" to fat,
                    "carb" to carb,
                    "fiber" to fiber
                )

                db.collection("products")
                    .document(barcode)
                    .set(product)
                    .addOnSuccessListener {

                        Toast.makeText(this, "Продукт добавлен", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                    }
                    .addOnFailureListener {

                        Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()

                    }

            } catch (e: Exception) {

                Toast.makeText(this, "Введите корректные числа", Toast.LENGTH_SHORT).show()

            }
        }

        dialog.show()
    }


}