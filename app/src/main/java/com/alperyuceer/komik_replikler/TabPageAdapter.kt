package com.alperyuceer.komik_replikler

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alperyuceer.komik_replikler.fragments.*

class TabPageAdapter(
    activity: FragmentActivity,
    private val categories: List<String>
) : FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return GenericFragment.newInstance(categories[position].lowercase())
    }

    fun getCategories(): List<String> = categories
}
