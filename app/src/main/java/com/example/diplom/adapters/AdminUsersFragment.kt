package com.example.diplom.adapters

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.diplom.databinding.DialogUserBinding
import com.example.diplom.databinding.FragmentAdminUsersBinding
import com.example.diplom.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsersFragment : Fragment() {

    private lateinit var binding: FragmentAdminUsersBinding
    private val db = FirebaseFirestore.getInstance()
    private val userList = mutableListOf<Pair<String, String>>() // Pair<id, email>
    private lateinit var container: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdminUsersBinding.inflate(inflater, parent, false)
        container = binding.usersContainer

        setupSearch()
        loadAllUsers()

        return binding.root
    }

    private fun setupSearch() {
        lifecycleScope.launch {
            val searchFlow = callbackFlow<String> {
                val watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun afterTextChanged(s: Editable?) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        trySend(s.toString())
                    }
                }
                binding.searchInput.addTextChangedListener(watcher)
                awaitClose { binding.searchInput.removeTextChangedListener(watcher) }
            }

            searchFlow.debounce(300).collectLatest { query ->
                loadAllUsers(query)
            }
        }
    }

    private fun loadAllUsers(query: String = "") {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection("users").get().await()
                userList.clear()

                snapshot.documents.forEach { doc ->
                    val email = doc.getString("email") ?: return@forEach
                    if (query.isNotBlank() && !email.lowercase().contains(query.lowercase())) return@forEach
                    userList.add(Pair(doc.id, email))
                }

                launch(Dispatchers.Main) { displayUsers() }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки пользователей", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayUsers() {
        container.removeAllViews()
        val inflater = layoutInflater

        userList.forEach { (id, email) ->
            val card = inflater.inflate(R.layout.admin_user_item, container, false)
            val emailText = card.findViewById<TextView>(R.id.emailText)
            emailText.text = email

            card.setOnClickListener { openUserDialog(id) }
            container.addView(card)
        }
    }

    private fun openUserDialog(userId: String) {
        val dialogBinding = DialogUserBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (!doc.exists()) return@launch

                val email = doc.getString("email") ?: ""
                dialogBinding.inputEmail.setText(email)
                dialogBinding.inputEmail.isEnabled = false

                dialogBinding.inputAge.setText(doc.getLong("age")?.toString() ?: "")
                dialogBinding.inputWeight.setText(doc.getDouble("weight")?.toString() ?: "")
                dialogBinding.inputHeight.setText(doc.getDouble("height")?.toString() ?: "")
                dialogBinding.inputGoal.setText(doc.getLong("goal")?.toString() ?: "")

                // Spinner для пола
                val genders = listOf("Мужской", "Женский")
                val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genders)
                genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dialogBinding.genderSpinner.adapter = genderAdapter
                val currentGender = doc.getString("gender") ?: "Мужской"
                dialogBinding.genderSpinner.setSelection(genders.indexOf(currentGender))

                // Spinner для роли
                val roles = listOf("none", "admin", "support")
                val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
                roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dialogBinding.roleSpinner.adapter = roleAdapter

                // Загружаем роль из worker, если есть
                val workerDoc = db.collection("worker").document(email).get().await()
                val currentRole = if (workerDoc.exists()) workerDoc.getString("role") else "none"
                dialogBinding.roleSpinner.setSelection(roles.indexOf(currentRole))

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.saveUserBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val updated = hashMapOf<String, Any>(
                        "age" to (dialogBinding.inputAge.text.toString().toIntOrNull() ?: 0),
                        "weight" to (dialogBinding.inputWeight.text.toString().toDoubleOrNull() ?: 0.0),
                        "height" to (dialogBinding.inputHeight.text.toString().toDoubleOrNull() ?: 0.0),
                        "goal" to (dialogBinding.inputGoal.text.toString().toIntOrNull() ?: 0),
                        "gender" to dialogBinding.genderSpinner.selectedItem.toString()
                    )
                    db.collection("users").document(userId).update(updated).await()

                    // Сохраняем роль в worker только если не "none"
                    val selectedRole = dialogBinding.roleSpinner.selectedItem.toString()
                    val email = dialogBinding.inputEmail.text.toString()
                    if (selectedRole != "none") {
                        db.collection("worker").document(email)
                            .set(hashMapOf("email" to email, "role" to selectedRole))
                            .await()
                    } else {
                        // Если выбрано "none", удаляем запись из worker (если существует)
                        db.collection("worker").document(email).delete().await()
                    }

                    Toast.makeText(requireContext(), "Пользователь обновлён", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadAllUsers(binding.searchInput.text.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogBinding.deleteUserBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    db.collection("users").document(userId).delete().await()
                    Toast.makeText(requireContext(), "Пользователь удалён", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadAllUsers(binding.searchInput.text.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}