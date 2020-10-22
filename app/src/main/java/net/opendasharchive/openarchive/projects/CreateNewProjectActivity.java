package net.opendasharchive.openarchive.projects;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;

import java.util.Date;
import java.util.List;

public class CreateNewProjectActivity extends AppCompatActivity {

    public static final String SPECIAL_CHARS = ".*[\\\\/*\\s]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_project);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.title_new_project));


         final EditText etNewProjectName = findViewById(R.id.edtNewProject);
         etNewProjectName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){

                    saveProject ();

                }
            return false;
        }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_project, menu);

        return super.onCreateOptionsMenu(menu);

    }

    private void saveProject ()
    {
        final EditText etNewProjectName = findViewById(R.id.edtNewProject);
        String newProjectName = etNewProjectName.getText().toString();
        if (!TextUtils.isEmpty(newProjectName))
        {
            if (newProjectName.matches(SPECIAL_CHARS))
            {
                Toast.makeText(CreateNewProjectActivity.this, getString(R.string.warning_special_chars),Toast.LENGTH_SHORT).show();
            }
            else {
                if (createProject(newProjectName)) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        }
    }

    private boolean createProject (String description)
    {
        List<Project> listProjects = Project.getAllBySpace(Space.getCurrentSpace().getId(), false);

        //check for duplicate name
        for (Project project : listProjects)
        {
           if (project.description.equals(description)) {
               Toast.makeText(this,getString(R.string.error_project_exists),Toast.LENGTH_LONG).show();
               return false;
           }
        }

        Project project = new Project ();
        project.created = new Date();
        project.description = description;
        project.spaceId = Space.getCurrentSpace().getId();
        project.save();

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;

            case R.id.action_done:
                saveProject();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
