package net.opendasharchive.openarchive.features.media

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Recycler

data class SectionViewHolder(
    var mediaSection: View? = null,
    var mediaGrid: Recycler? = null,
    var sectionStatus: TextView? = null,
    var sectionTimestamp: TextView? = null,
    var action: View? = null
)