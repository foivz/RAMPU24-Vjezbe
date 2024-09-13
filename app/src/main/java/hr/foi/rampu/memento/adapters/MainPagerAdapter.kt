package hr.foi.rampu.memento.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import hr.foi.rampu.memento.R
import hr.foi.rampu.memento.fragments.CompletedFragment
import hr.foi.rampu.memento.fragments.NewsFragment
import hr.foi.rampu.memento.fragments.PendingFragment
import kotlin.reflect.KClass

class MainPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    data class FragmentItem(val titleRes: Int, val iconRes: Int, val fragmentClass: KClass<*>)

    val fragmentItems = ArrayList<FragmentItem>()

    fun addFragment(fragment: FragmentItem) {
        fragmentItems.add(fragment)
    }

    /*
    val titleList = listOf(R.string.tasks_pending, R.string.tasks_completed, R.string.news)
    val iconList = listOf(
        R.drawable.baseline_assignment_late_24,
        R.drawable.baseline_assignment_turned_in_24,
        R.drawable.baseline_wysiwyg_24
    ) */

    override fun getItemCount(): Int = fragmentItems.size

    override fun createFragment(position: Int): Fragment {
        return fragmentItems[position].fragmentClass.java.newInstance() as Fragment
    }
}