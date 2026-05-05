package com.example.diplom.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.diplom.MainActivity
import com.example.diplom.R
import com.example.diplom.models.MealItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodayMealAdapter(
    private val context: Context,
    private val meals: MutableList<MealItem>,
    private var currentCalendar: Calendar
) : RecyclerView.Adapter<TodayMealAdapter.ViewHolder>() {

    private val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun setCurrentCalendar(calendar: Calendar) {
        currentCalendar = calendar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_today_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = meals[position]
        holder.nameText.text = meal.name
        holder.gramsText.text = "${meal.grams} г"

        holder.gramsText.setOnClickListener { showEditMealDialog(meal, position) }

        holder.deleteBtn.setOnClickListener {
            val user = currentUser ?: return@setOnClickListener
            val userId = user.uid
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(currentCalendar.time)

            db.collection("dailyMeals")
                .document(userId)
                .collection(today)
                .document(meal.id)
                .delete()
                .addOnSuccessListener {
                    meals.removeAt(position)
                    notifyItemRemoved(position)
                    if (context is MainActivity) {
                        context.loadDailyMeals()
                    }
                }
        }
    }

    private fun showEditMealDialog(meal: MealItem, position: Int) {
        val user = currentUser ?: return
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_meal, null)

        val productNameText: TextView = view.findViewById(R.id.dialogProductName)
        val caloriesText: TextView = view.findViewById(R.id.dialogCaloriesText)
        val proteinText: TextView = view.findViewById(R.id.dialogProteinText)
        val fatText: TextView = view.findViewById(R.id.dialogFatText)
        val carbText: TextView = view.findViewById(R.id.dialogCarbText)
        val fiberText: TextView = view.findViewById(R.id.dialogFiberText)
        val gramsInput: EditText = view.findViewById(R.id.dialogGramsInput)
        val editButton: Button = view.findViewById(R.id.dialogEditButton)

        // Заполняем данные
        productNameText.text = meal.name
        caloriesText.text = "Калории: ${meal.calories.toInt()}"
        proteinText.text = "Белки: ${meal.protein} г"
        fatText.text = "Жиры: ${meal.fat} г"
        carbText.text = "Углеводы: ${meal.carb} г"
        fiberText.text = "Клетчатка: ${meal.fiber} г"
        gramsInput.setText(meal.grams.toString())

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        dialog.show()

        editButton.setOnClickListener {
            val gramsStr = gramsInput.text.toString().trim()
            if (gramsStr.isEmpty()) return@setOnClickListener

            val newGrams = gramsStr.toFloatOrNull()
            if (newGrams == null) {
                Toast.makeText(context, "Введите корректное число", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = user.uid
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

            db.collection("dailyMeals").document(userId)
                .collection(today)
                .document(meal.id)
                .delete()
                .addOnSuccessListener {
                    val factor = newGrams / meal.grams
                    val newMeal = hashMapOf(
                        "name" to meal.name,
                        "grams" to newGrams,
                        "calories" to meal.calories * factor,
                        "protein" to meal.protein * factor,
                        "fat" to meal.fat * factor,
                        "carb" to meal.carb * factor,
                        "fiber" to meal.fiber * factor,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("dailyMeals").document(userId)
                        .collection(today)
                        .add(newMeal)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Продукт обновлён", Toast.LENGTH_SHORT).show()

                            val updatedMeal = meals[position]
                            updatedMeal.grams = newGrams
                            updatedMeal.calories = meal.calories * factor
                            updatedMeal.protein = meal.protein * factor
                            updatedMeal.fat = meal.fat * factor
                            updatedMeal.carb = meal.carb * factor
                            updatedMeal.fiber = meal.fiber * factor

                            notifyItemChanged(position)

                            if (context is MainActivity) {
                                context.loadDailyMeals()
                            }
                            dialog.dismiss()
                        }
                }
        }
    }

    override fun getItemCount(): Int = meals.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.nameText)
        val gramsText: TextView = itemView.findViewById(R.id.gramsText)
        val deleteBtn: Button = itemView.findViewById(R.id.deleteBtn)
    }
}