package com.example.diplom.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class StatsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> StatsWeekFragment()
        1 -> StatsMonthFragment()
        else -> StatsYearFragment()
    }

    override fun getItemCount(): Int = 3
}