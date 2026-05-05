package com.example.diplom

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diplom.adapters.SearchAdapter
import com.example.diplom.models.ProductItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FavoriteFoodActivity : AppCompatActivity() {

    private lateinit var favSearchInput: EditText
    private lateinit var favRecyclerView: RecyclerView
    private lateinit var favAdapter: SearchAdapter
    private var favoriteList = mutableListOf<ProductItem>()
    private lateinit var db: FirebaseFirestore
    private var currentUser: FirebaseUser? = null
    private lateinit var selectedCalendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_food)

        db = FirebaseFirestore.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser

        val calendarMillis = intent.getLongExtra("selected_date", System.currentTimeMillis())
        selectedCalendar = Calendar.getInstance().apply { timeInMillis = calendarMillis }

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

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            val intent: Intent? = when (item.itemId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_fav_activity -> Intent(this, ProfileActivity::class.java)
                R.id.nav_scan -> {
                    IntentIntegrator(this).apply {
                        setBeepEnabled(true)
                        setPrompt("Сканируйте штрихкод продукта")
                        setOrientationLocked(false)
                        initiateScan()
                    }
                    return@setOnItemSelectedListener true
                }
                R.id.nav_fav_food -> return@setOnItemSelectedListener true
                R.id.nav_stats -> Intent(this, StatisticsActivity::class.java)
                else -> null
            }
            intent?.putExtra("selected_date", selectedCalendar.timeInMillis)
            intent?.let { startActivity(it) }
            true
        }
        bottomNav.selectedItemId = R.id.nav_fav_food
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
        val userId = currentUser?.uid ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_food, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val nameText = dialogView.findViewById<TextView>(R.id.dialogProductName)
        val proteinText = dialogView.findViewById<TextView>(R.id.dialogProteinText)
        val fatText = dialogView.findViewById<TextView>(R.id.dialogFatText)
        val carbText = dialogView.findViewById<TextView>(R.id.dialogCarbText)
        val fiberText = dialogView.findViewById<TextView>(R.id.dialogFiberText)
        val gramsInput = dialogView.findViewById<EditText>(R.id.dialogGramsInput)
        val btn100g = dialogView.findViewById<Button>(R.id.dialogBtn100g)
        val btnAddFavorite = dialogView.findViewById<Button>(R.id.dialogAddFavorite)
        val btnAddMeal = dialogView.findViewById<Button>(R.id.dialogAddMeal)

        btn100g.setOnClickListener { gramsInput.setText("100") }

        db.collection("products").document(productId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@addOnSuccessListener
                }
                val name = doc.getString("name") ?: ""
                val protein = doc.getDouble("protein")?.toFloat() ?: 0f
                val fat = doc.getDouble("fat")?.toFloat() ?: 0f
                val carb = doc.getDouble("carb")?.toFloat() ?: 0f
                val fiber = doc.getDouble("fiber")?.toFloat() ?: 0f

                nameText.text = name
                proteinText.text = "Белки: $protein"
                fatText.text = "Жиры: $fat"
                carbText.text = "Углеводы: $carb"
                fiberText.text = "Клетчатка: $fiber"

                btnAddFavorite.setOnClickListener {
                    val fav = hashMapOf(
                        "name" to name,
                        "calories" to doc.getDouble("calories"),
                        "protein" to protein,
                        "fat" to fat,
                        "carb" to carb,
                        "fiber" to fiber
                    )
                    db.collection("usersFood")
                        .document(userId)
                        .collection("favorite_food")
                        .document(productId)
                        .set(fav)
                    Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                }

                btnAddMeal.setOnClickListener {
                    val gramsStr = gramsInput.text.toString().trim()
                    if (gramsStr.isEmpty()) {
                        Toast.makeText(this, "Введите граммовку", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val grams = gramsStr.toFloatOrNull() ?: return@setOnClickListener
                    val multiplier = grams / 100f
                    val meal = hashMapOf(
                        "name" to name,
                        "grams" to grams,
                        "calories" to (doc.getDouble("calories")?.times(multiplier)),
                        "protein" to (protein * multiplier),
                        "fat" to (fat * multiplier),
                        "carb" to (carb * multiplier),
                        "fiber" to (fiber * multiplier),
                        "timestamp" to System.currentTimeMillis()
                    )
                    val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(selectedCalendar.time)
                    db.collection("dailyMeals")
                        .document(userId)
                        .collection(dayKey)
                        .add(meal)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Добавлено!", Toast.LENGTH_SHORT).show()
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
}