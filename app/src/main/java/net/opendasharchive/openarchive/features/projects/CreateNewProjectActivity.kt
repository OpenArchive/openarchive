package net.opendasharchive.openarchive.features.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityCreateNewProjectBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Project.Companion.getAllBySpace
import net.opendasharchive.openarchive.db.Space.Companion.getCurrentSpace
import java.util.*

class CreateNewProjectActivity : AppCompatActivity() {

    private val SPECIAL_CHARS = ".*[\\\\/*\\s]"

    private lateinit var mBinding: ActivityCreateNewProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityCreateNewProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_new_project)

        mBinding.createNewProjectLayout.edtNewProject.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveProject()
            }
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_new_project, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun saveProject() {
        val newProjectName = mBinding.createNewProjectLayout.edtNewProject.text.toString()
        if (newProjectName.isNotEmpty()) {
            if (newProjectName.matches(SPECIAL_CHARS.toRegex())) {
                Toast.makeText(
                    this@CreateNewProjectActivity,
                    getString(R.string.warning_special_chars),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (createProject(newProjectName)) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun createProject(description: String): Boolean {
        getCurrentSpace()?.let { currentSpace ->
            val listProjects = getAllBySpace(currentSpace.id, false)
            //check for duplicate name
            listProjects?.forEach { project ->
                if (project.description == description) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_project_exists),
                        Toast.LENGTH_LONG
                    ).show()
                    return false
                }
            }
            val project = Project()
            project.created = Date()
            project.description = description
            project.spaceId = currentSpace.id
            project.save()
            return true

        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_done -> {
                saveProject()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}