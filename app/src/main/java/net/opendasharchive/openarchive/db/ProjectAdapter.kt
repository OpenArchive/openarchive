package net.opendasharchive.openarchive.db

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.media.grid.MediaGridFragment
import net.opendasharchive.openarchive.features.media.NewProjectFragment
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter

class ProjectAdapter( private val context: Context, fragmentManager: FragmentManager) : SmartFragmentStatePagerAdapter(fragmentManager) {

    private var data: List<Project>? = null

    override fun getItem(position: Int): Fragment {
        return if (position == 0) {
            NewProjectFragment()
        } else {
            MediaGridFragment().also { fragment ->
                getProject(position)?.let { project ->
                    fragment.setProjectId(project.id)
                }
                fragment.arguments = Bundle()
            }
        }
    }

    override fun getCount(): Int {
        return if (data != null) data!!.size + 1 else 0
    }

    fun getProject(i: Int): Project? {
        return data?.let {
            if (i > 0) {
                it[i - 1]
            } else {
                null
            }
        }
    }

    fun updateData(data: List<Project>?) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (position == 0) {
            val imageSpan = ImageSpan(context, R.drawable.ic_add_circle_outline_black_24dp)
            val spannableString = SpannableString(" ")
            val start = 0
            val end = 1
            val flag = 0
            spannableString.setSpan(imageSpan, start, end, flag)
            spannableString
        } else {
            data?.let {
                it[position - 1].description
            }
        }
    }

}