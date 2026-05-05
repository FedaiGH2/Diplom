package com.example.diplom.adapters

import kotlinx.coroutines.channels.awaitClose
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.diplom.databinding.AddFoodToBdBinding
import com.example.diplom.databinding.FragmentAdminProductsBinding
import com.example.diplom.models.ProductItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminProductsFragment : Fragment() {

    private lateinit var binding: FragmentAdminProductsBinding
    private val db = FirebaseFirestore.getInstance()
    private val productList = mutableListOf<ProductItem>()
    private lateinit var container: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminProductsBinding.inflate(inflater, parent, false)
        container = binding.productsContainer

        setupSearch()
        loadAllProducts()

        return binding.root
    }

    private fun setupSearch() {
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
                loadAllProducts(query)
            }
        }
    }

    private fun loadAllProducts(query: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            val snapshot = db.collection("products").get().await()
            productList.clear()

            snapshot.documents.forEach { doc ->
                val name = doc.getString("name") ?: return@forEach
                if (query.isNotBlank() && !name.lowercase().contains(query.lowercase())) return@forEach

                productList.add(ProductItem(
                    doc.id, name,
                    doc.getDouble("calories")?.toFloat() ?: 0f,
                    doc.getDouble("protein")?.toFloat() ?: 0f,
                    doc.getDouble("fat")?.toFloat() ?: 0f,
                    doc.getDouble("carb")?.toFloat() ?: 0f,
                    doc.getDouble("fiber")?.toFloat() ?: 0f
                ))
            }

            launch(Dispatchers.Main) {
                displayProducts()
            }
        }
    }

    private fun displayProducts() {
        container.removeAllViews()
        val inflater = layoutInflater

        productList.forEach { product ->
            val card = inflater.inflate(com.example.diplom.R.layout.admin_food_item, container, false)
            val nameText = card.findViewById<android.widget.TextView>(com.example.diplom.R.id.nameText)
            nameText.text = product.name

            card.setOnClickListener { openEditProductDialog(product) }

            container.addView(card)
        }
    }

    private fun openEditProductDialog(product: ProductItem) {
        val dialogBinding = AddFoodToBdBinding.inflate(layoutInflater)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.inputName.setText(product.name)
        dialogBinding.inputCalories.setText(product.calories.toString())
        dialogBinding.inputProtein.setText(product.protein.toString())
        dialogBinding.inputFat.setText(product.fat.toString())
        dialogBinding.inputCarb.setText(product.carb.toString())
        dialogBinding.inputFiber.setText(product.fiber.toString())

        dialogBinding.addProductBtn.setOnClickListener {
            val updatedProduct = hashMapOf(
                "name" to dialogBinding.inputName.text.toString(),
                "calories" to dialogBinding.inputCalories.text.toString().toFloat(),
                "protein" to dialogBinding.inputProtein.text.toString().toFloat(),
                "fat" to dialogBinding.inputFat.text.toString().toFloat(),
                "carb" to dialogBinding.inputCarb.text.toString().toFloat(),
                "fiber" to dialogBinding.inputFiber.text.toString().toFloat()
            )

            db.collection("products").document(product.id)
                .set(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Продукт обновлен", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadAllProducts()
                }
        }

        dialogBinding.deleteProductBtn.visibility = View.VISIBLE
        dialogBinding.deleteProductBtn.setOnClickListener {
            db.collection("products").document(product.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Продукт удалён", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadAllProducts()
                }
        }

        dialog.show()
    }
}