package com.example.diplom.logic;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StatsPagerAdapter extends FragmentStateAdapter {

    public StatsPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new StatsWeekFragment();
            case 1: return new StatsMonthFragment();
            default: return new StatsYearFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
