package com.example.diplom.adapters

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.diplom.databinding.AddFoodToBdBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AdminPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun createFragment(position: Int): Fragment = when(position) {
        0 -> AdminAddFoodFragment()
        1 -> AdminUsersFragment()
        2 -> AdminProductsFragment() // новая вкладка
        else -> throw IllegalStateException("Unexpected position $position")
    }

    override fun getItemCount(): Int = 3
}