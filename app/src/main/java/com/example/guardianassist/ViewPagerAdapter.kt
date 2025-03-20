package com.example.guardianassist

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.guardianassist.adminfragments.ClockIn
import com.example.guardianassist.adminfragments.ClockOut
import com.example.guardianassist.adminfragments.Hourlycheck
import com.example.guardianassist.adminfragments.Patrol
import com.example.guardianassist.adminfragments.Uniformcheck

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val fragmentList = listOf(
        ClockIn(),
        ClockOut(),
        Hourlycheck(),
        Patrol(),
        Uniformcheck()
    )

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]
}
