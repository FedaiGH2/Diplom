package com.example.diplom

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diplom.adapters.SearchAdapter
import com.example.diplom.databinding.DialogAddFoodBinding
import com.example.diplom.models.ProductItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class FavoriteFoodActivity : AppCompatActivity() {

    private lateinit var favSearchInput: EditText
    private lateinit var favRecyclerView: RecyclerView
    private lateinit var favAdapter: SearchAdapter
    private var favoriteList = mutableListOf<ProductItem>()
    private lateinit var db: FirebaseFirestore
    private var currentUser: FirebaseUser? = null
    private lateinit var selectedCalendar: Calendar

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val barcode = result.data?.getStringExtra("SCAN_RESULT")
            if (barcode != null) {
                // Перенаправляем на главную с результатом сканирования
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SCAN_RESULT_FROM_STATS", barcode) // Используем тот же ключ, что и в статистике
                    putExtra("selected_date", selectedCalendar.timeInMillis)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_food)

        db = FirebaseFirestore.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser

        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }

        // ===== UI =====
        favSearchInput = findViewById(R.id.favSearchInput)
        favRecyclerView = findViewById(R.id.favRecyclerView)
        favRecyclerView.layoutManager = LinearLayoutManager(this)

        favAdapter = SearchAdapter(this, favoriteList, object : SearchAdapter.OnProductClickListener {
            override fun onProductClick(product: ProductItem) {
                showAddFoodDialog(product.id)
            }
        })

        favRecyclerView.adapter = favAdapter

        loadFavorites()
        setupSearch()

        // ===== BOTTOM BAR =====
        val home = findViewById<ImageView>(R.id.home)
        val fav = findViewById<ImageView>(R.id.fav)
        val scan = findViewById<ImageView>(R.id.scan)
        val food = findViewById<ImageView>(R.id.food)
        val stats = findViewById<ImageView>(R.id.stats)

        home.setOnClickListener {
            setActiveTab(home)
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        fav.setOnClickListener {
            setActiveTab(fav)
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        scan.setOnClickListener {
            setActiveTab(scan)
            val intent = Intent(this, ScannerActivity::class.java)
            scanLauncher.launch(intent)
        }

        food.setOnClickListener {
            setActiveTab(food)
            // уже здесь — ничего не делаем
        }

        stats.setOnClickListener {
            setActiveTab(stats)
            startActivity(Intent(this, StatisticsActivity::class.java).apply {
                putExtra("selected_date", selectedCalendar.timeInMillis)
            })
        }

        // ===== АКТИВНАЯ ВКЛАДКА =====
        setActiveTab(food)
    }

    private fun navigateTo(target: Class<*>) {
        val intent = Intent(this, target)
        intent.putExtra("selected_date", selectedCalendar.timeInMillis)
        startActivity(intent)
    }
    private fun loadFavorites() {
        val userId = currentUser?.uid ?: return
        db.collection("usersFood")
            .document(userId)
            .collection("favorite_food")
            .get()
            .addOnSuccessListener { snapshot ->
                favoriteList.clear()
                for (doc in snapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("name") ?: continue
                    favoriteList.add(ProductItem(id, name))
                }
                favAdapter.notifyDataSetChanged()
            }
    }

    private fun showAddFoodDialog(productId: String) {
        val user = currentUser ?: return
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()

        // Кнопка 100 грамм
        dialogBinding.dialogBtn100g.setOnClickListener { dialogBinding.dialogGramsInput.setText("100") }

        lifecycleScope.launch {
            // Получаем данные продукта
            val doc = db.collection("products").document(productId).get().await()
            if (!doc.exists()) {
                Toast.makeText(this@FavoriteFoodActivity, "Продукт не найден", Toast.LENGTH_SHORT).show()
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

            // Проверяем, есть ли продукт в избранном
            var isFavorite = false
            val favDoc = db.collection("usersFood").document(user.uid)
                .collection("favorite_food").document(productId).get().await()
            if (favDoc.exists()) isFavorite = true

            // Устанавливаем иконку избранного
            dialogBinding.dialogAddFavorite.icon = ContextCompat.getDrawable(
                this@FavoriteFoodActivity,
                if (isFavorite) R.drawable.star else R.drawable.star_grey
            )

            dialogBinding.dialogAddFavorite.setOnClickListener {
                if (isFavorite) {
                    db.collection("usersFood").document(user.uid)
                        .collection("favorite_food").document(productId)
                        .delete().addOnSuccessListener {
                            isFavorite = false
                            dialogBinding.dialogAddFavorite.icon = ContextCompat.getDrawable(
                                this@FavoriteFoodActivity, R.drawable.star_grey
                            )
                            Toast.makeText(this@FavoriteFoodActivity, "Удалено из избранного", Toast.LENGTH_SHORT).show()

                            // Обновляем список избранного
                            loadFavorites()
                        }
                } else {
                    db.collection("usersFood").document(user.uid)
                        .collection("favorite_food").document(productId)
                        .set(mapOf(
                            "name" to name,
                            "calories" to calories,
                            "protein" to protein,
                            "fat" to fat,
                            "carb" to carb,
                            "fiber" to fiber
                        )).addOnSuccessListener {
                            isFavorite = true
                            dialogBinding.dialogAddFavorite.icon = ContextCompat.getDrawable(
                                this@FavoriteFoodActivity, R.drawable.star
                            )
                            Toast.makeText(this@FavoriteFoodActivity, "Добавлено в избранное", Toast.LENGTH_SHORT).show()

                            // Обновляем список избранного
                            loadFavorites()
                        }
                }
            }

            // Добавление еды в приём пищи
            dialogBinding.dialogAddMeal.setOnClickListener {
                val gramsStr = dialogBinding.dialogGramsInput.text.toString().trim()
                if (gramsStr.isBlank()) {
                    Toast.makeText(this@FavoriteFoodActivity, "Введите граммовку", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val grams = gramsStr.toFloat()
                val multiplier = grams / 100f
                val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.time)

                db.collection("dailyMeals").document(user.uid).collection(dayKey)
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
                        Toast.makeText(this@FavoriteFoodActivity, "Добавлено!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
            }
        }

        dialog.show()
    }
    private fun setupSearch() {
        favSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase(Locale.getDefault())
                if (query.isEmpty()) {
                    loadFavorites()
                    return
                }
                val filtered = favoriteList.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
                favAdapter.updateList(filtered)
            }
        })
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