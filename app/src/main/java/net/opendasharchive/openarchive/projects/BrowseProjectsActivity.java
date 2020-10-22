package net.opendasharchive.openarchive.projects;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.services.dropbox.DropboxSiteController;
import net.opendasharchive.openarchive.services.webdav.WebDAVSiteController;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;

import io.scal.secureshareui.controller.SiteController;

public class BrowseProjectsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private MyRecyclerViewAdapter adapter;
    private List<File> listFolders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_projects);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("");

        rv = findViewById(R.id.rv_folder_list);

        rv.setLayoutManager(new LinearLayoutManager(this));

        new AsyncTask<Void,Void,Void>()
        {

            @Override
            protected Void doInBackground(Void[] objects) {

                getFolders();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (listFolders != null) {
                    adapter = new MyRecyclerViewAdapter(BrowseProjectsActivity.this, listFolders);
                    adapter.setClickListener(new ItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {

                            File fileFolder = adapter.getItem(position);
                            String projectName = fileFolder.getName();
                            try {
                                projectName = URLDecoder.decode(fileFolder.getName(),"UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            if (!projectExists(projectName)) {
                                createProject(projectName);
                            }

                            setResult(RESULT_OK);
                            finish();
                        }
                    });
                    rv.setAdapter(adapter);
                }
            }
        }.execute();

    }

    private boolean projectExists (String name)
    {
        List<Project> listProjects = Project.getAllBySpace(Space.getCurrentSpace().getId(), false);

        //check for duplicate name
        for (Project project : listProjects)
        {
            if (project.description.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private void createProject (String description)
    {
        Project project = new Project ();
        project.created = new Date();
        project.description = description;
        project.spaceId = Space.getCurrentSpace().getId();
        project.save();
    }


    public void getFolders ()
    {
        SiteController sc = null;

        Space space = Space.getCurrentSpace();

        if (space != null) {

            if (space.type == Space.TYPE_WEBDAV)
                sc = SiteController.getSiteController(WebDAVSiteController.SITE_KEY, this, null, null);
            else if (space.type == Space.TYPE_DROPBOX)
                sc = SiteController.getSiteController(DropboxSiteController.SITE_KEY, this, null, null);

            if (sc != null) {
                try {
                    listFolders = sc.getFolders(space, space.host);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

        private List<File> mData;
        private LayoutInflater mInflater;
        private ItemClickListener mClickListener;

        // data is passed into the constructor
        MyRecyclerViewAdapter(Context context, List<File> data) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = data;
        }

        // inflates the row layout from xml when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.rv_simple_row, parent, false);
            return new ViewHolder(view);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String folder = mData.get(position).getName();
            holder.myTextView.setText(folder);
        }

        // total number of rows
        @Override
        public int getItemCount() {
            return mData.size();
        }


        // stores and recycles views as they are scrolled off screen
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView myTextView;

            ViewHolder(View itemView) {
                super(itemView);
                myTextView = itemView.findViewById(R.id.rvRowTitle);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
            }
        }

        // convenience method for getting data at click position
        File getItem(int id) {
            return mData.get(id);
        }

        // allows clicks events to be caught
        void setClickListener(ItemClickListener itemClickListener) {
            this.mClickListener = itemClickListener;
        }


    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
