package com.example.diplom.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import com.example.diplom.R
import android.view.View

import androidx.fragment.app.Fragment
import com.example.diplom.databinding.AddFoodToBdBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AdminAddFoodFragment : Fragment() {

    private lateinit var container: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_admin_add_food, parent, false)

        container = view.findViewById(R.id.foodContainer)

        loadRequests()

        return view
    }

    private fun loadRequests(){

        db.collection("addproduct")
            .get()
            .addOnSuccessListener { snap ->

                container.removeAllViews()

                snap.documents.forEach { doc ->

                    val card = layoutInflater.inflate(
                        R.layout.admin_food_item,
                        container,
                        false
                    )

                    val nameText = card.findViewById<TextView>(R.id.nameText)

                    val name = doc.getString("name") ?: ""

                    nameText.text = name

                    card.setOnClickListener {

                        showApproveDialog(doc)

                    }

                    container.addView(card)

                }
            }
    }

    private fun showApproveDialog(doc: DocumentSnapshot) {

        val dialogBinding = AddFoodToBdBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val barcode = doc.id

        // Заполняем поля значениями продукта
        dialogBinding.inputName.setText(doc.getString("name"))
        dialogBinding.inputCalories.setText(doc.getDouble("calories").toString())
        dialogBinding.inputProtein.setText(doc.getDouble("protein").toString())
        dialogBinding.inputFat.setText(doc.getDouble("fat").toString())
        dialogBinding.inputCarb.setText(doc.getDouble("carb").toString())
        dialogBinding.inputFiber.setText(doc.getDouble("fiber").toString())

        // Показываем кнопку "Удалить", т.к. это админ
        dialogBinding.deleteProductBtn.visibility = View.VISIBLE

        // Добавление продукта в products
        dialogBinding.addProductBtn.setOnClickListener {
            val product = hashMapOf(
                "name" to dialogBinding.inputName.text.toString(),
                "calories" to dialogBinding.inputCalories.text.toString().toFloat(),
                "protein" to dialogBinding.inputProtein.text.toString().toFloat(),
                "fat" to dialogBinding.inputFat.text.toString().toFloat(),
                "carb" to dialogBinding.inputCarb.text.toString().toFloat(),
                "fiber" to dialogBinding.inputFiber.text.toString().toFloat()
            )

            db.collection("products").document(barcode)
                .set(product)
                .addOnSuccessListener {
                    db.collection("addproduct").document(barcode).delete()
                    Toast.makeText(requireContext(), "Продукт добавлен", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadRequests()
                }
        }

        // Удаление продукта из addproduct
        dialogBinding.deleteProductBtn.setOnClickListener {
            db.collection("addproduct").document(barcode)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Продукт удалён", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadRequests()
                }
        }

        dialog.show()
    }
}