package net.opendasharchive.openarchive.db;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.projects.EditProjectActivity;
import net.opendasharchive.openarchive.util.Globals;

import java.util.List;

public class ProjectListAdapter extends RecyclerView.Adapter {

    Context mContext;
    List<Project> mListProjects;
    RecyclerView mRV;

    public ProjectListAdapter(Context context, List<Project> listProjects, RecyclerView recyclerView) {
        super();
        mContext = context;
        mListProjects = listProjects;
        mRV = recyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        final View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        ProjectViewHolder pvh = new ProjectViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int itemPosition = mRV.getChildLayoutPosition(view);
                Project p = mListProjects.get(itemPosition);

                Intent reviewProjectIntent = new Intent(mContext, EditProjectActivity.class);
                reviewProjectIntent.putExtra(Globals.EXTRA_CURRENT_PROJECT_ID, p.getId());
                mContext.startActivity(reviewProjectIntent);
            }
        });

        return pvh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        ProjectViewHolder pvh = (ProjectViewHolder)holder;
        Project project = mListProjects.get(position);

        StringBuffer sb = new StringBuffer();
        sb.append(project.description);

        if (project.isArchived())
            sb.append(" (").append(mContext.getString(R.string.status_archived)).append(")");

        pvh.tvTitle.setText(sb.toString());

    }

    @Override
    public int getItemCount() {
        return mListProjects.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(android.R.id.text1);
        }
    }
}
