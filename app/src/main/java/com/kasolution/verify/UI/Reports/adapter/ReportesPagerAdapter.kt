package com.kasolution.verify.UI.Reports.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kasolution.verify.UI.Reports.fragment.ReporteCajaFragment
import com.kasolution.verify.UI.Reports.fragment.ReporteFinanzasFragment
import com.kasolution.verify.UI.Reports.fragment.ReporteInventarioFragment

class ReportesPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReporteFinanzasFragment()
            1 -> ReporteInventarioFragment()
            2 -> ReporteCajaFragment()
            else -> ReporteFinanzasFragment()
        }
    }
}