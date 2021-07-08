package net.opendasharchive.openarchive.db

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.features.projects.EditProjectActivity
import net.opendasharchive.openarchive.util.Globals

class ProjectListAdapter (
        private val mContext: Context,
        private var mListProjects: List<Project>,
        var mRV: RecyclerView
) : RecyclerView.Adapter<ProjectListAdapter.ProjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val pvh = ProjectViewHolder(view)
        view.setOnClickListener { v ->
            val itemPosition = mRV.getChildLayoutPosition(v)
            val p = mListProjects[itemPosition]
            val reviewProjectIntent = Intent(mContext, EditProjectActivity::class.java)
            reviewProjectIntent.putExtra(Globals.EXTRA_CURRENT_PROJECT_ID, p.id)
            mContext.startActivity(reviewProjectIntent)
        }

        return pvh
    }

    override fun getItemCount(): Int = mListProjects.size

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val (description, _, _, archived) = mListProjects[position]

        val sb = StringBuffer()
        sb.append(description)

        if (archived) sb.append(" (").append(mContext.getString(R.string.status_archived)).append(")")

        holder.tvTitle?.text = sb.toString()
    }

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTitle: TextView? = null
        init {
            tvTitle = itemView.findViewById(android.R.id.text1)
        }

    }
}