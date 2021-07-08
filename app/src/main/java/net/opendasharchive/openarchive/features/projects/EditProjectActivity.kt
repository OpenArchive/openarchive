package net.opendasharchive.openarchive.features.projects

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEditProjectBinding
import net.opendasharchive.openarchive.db.Project
import net.opendasharchive.openarchive.db.Project.Companion.getById
import net.opendasharchive.openarchive.util.Constants.EMPTY_STRING
import net.opendasharchive.openarchive.util.Globals

class EditProjectActivity : AppCompatActivity() {

    private var mProject: Project? = null
    private lateinit var mBinding: ActivityEditProjectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityEditProjectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }

    override fun onPause() {
        super.onPause()
        updateLicense()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.title = EMPTY_STRING
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val projectId = intent.getLongExtra(Globals.EXTRA_CURRENT_PROJECT_ID, -1L)

        if (projectId != -1L) {
            mProject = getById(projectId)
            if (mProject == null) {
                finish()
                return
            }
        } else {
            finish()
            return
        }

        updateProject()
    }

    private fun updateProject() {

        mProject?.let { project ->
            mBinding.apply {
                if (!editProjectLayout.edtProjectName.text.isNullOrEmpty()) {
                    editProjectLayout.edtProjectName.setText(project.description)
                    editProjectLayout.edtProjectName.isEnabled = false
                } else {
                    editProjectLayout.edtProjectName.setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val newProjectName = editProjectLayout.edtProjectName.text.toString()
                            if (newProjectName.isNotEmpty()) {
                                project.description = newProjectName
                                project.save()
                            }
                        }
                        false
                    }
                }

                if (project.archived) {
                    editProjectLayout.actionArchiveProject.text =
                        getString(R.string.action_unarchive_project)
                } else {
                    editProjectLayout.actionArchiveProject.text =
                        getString(R.string.action_archive_project)
                }

                editProjectLayout.tbCcDerivEnable.isChecked = !project.licenseUrl.isNullOrEmpty()

                editProjectLayout.ccRow1.visibility =
                    if (editProjectLayout.tbCcDerivEnable.isChecked) View.VISIBLE else View.GONE
                editProjectLayout.ccRow2.visibility =
                    if (editProjectLayout.tbCcDerivEnable.isChecked) View.VISIBLE else View.GONE
                editProjectLayout.ccRow3.visibility =
                    if (editProjectLayout.tbCcDerivEnable.isChecked) View.VISIBLE else View.GONE

                editProjectLayout.tbCcDerivEnable.setOnCheckedChangeListener { buttonView, isChecked ->
                    editProjectLayout.ccRow1.visibility = if (isChecked) View.VISIBLE else View.GONE
                    editProjectLayout.ccRow2.visibility = if (isChecked) View.VISIBLE else View.GONE
                    editProjectLayout.ccRow3.visibility = if (isChecked) View.VISIBLE else View.GONE
                    updateLicense()
                }

                if (!project.licenseUrl.isNullOrEmpty()) {
                    if (project.licenseUrl == "http://creativecommons.org/licenses/by-sa/4.0/") {
                        editProjectLayout.tbCcDeriv.isChecked = true
                        editProjectLayout.tbCcComm.isChecked = true
                        editProjectLayout.tbCcSharealike.isChecked = true
                    } else if (project.licenseUrl == "http://creativecommons.org/licenses/by-nc-sa/4.0/") {
                        editProjectLayout.tbCcDeriv.isChecked = true
                        editProjectLayout.tbCcSharealike.isChecked = true
                    } else if (project.licenseUrl == "http://creativecommons.org/licenses/by/4.0/") {
                        editProjectLayout.tbCcDeriv.isChecked = true
                        editProjectLayout.tbCcComm.isChecked = true
                    } else if (project.licenseUrl == "http://creativecommons.org/licenses/by-nc/4.0/") {
                        editProjectLayout.tbCcDeriv.isChecked = true
                    } else if (project.licenseUrl == "http://creativecommons.org/licenses/by-nd/4.0/") {
                        editProjectLayout.tbCcComm.isChecked = true
                    }
                }

                editProjectLayout.tbCcDeriv.setOnCheckedChangeListener { buttonView, isChecked ->
                    updateLicense()
                    editProjectLayout.tbCcSharealike.isEnabled = isChecked
                }

                editProjectLayout.tbCcComm.setOnCheckedChangeListener { buttonView, isChecked -> updateLicense() }
                editProjectLayout.tbCcSharealike.setOnCheckedChangeListener { buttonView, isChecked -> updateLicense() }
                editProjectLayout.tbCcSharealike.isEnabled = editProjectLayout.tbCcDeriv.isChecked
                editProjectLayout.ccLicenseDisplay.text = project.licenseUrl
            }
        }
    }

    fun updateLicense() {

        mProject?.let { project ->

            //the default
            var licenseUrl = "https://creativecommons.org/licenses/by/4.0/"

            mBinding.editProjectLayout.apply {
                if (!tbCcDerivEnable.isChecked) {
                    ccLicenseDisplay.text = EMPTY_STRING
                    project.licenseUrl = null
                    project.save()
                    return
                }
                if (tbCcDeriv.isChecked && tbCcComm.isChecked && tbCcSharealike.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-sa/4.0/"
                } else if (tbCcDeriv.isChecked && tbCcSharealike.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-nc-sa/4.0/"
                } else if (tbCcDeriv.isChecked && tbCcComm.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by/4.0/"
                } else if (tbCcDeriv.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-nc/4.0/"
                } else if (tbCcComm.isChecked) {
                    licenseUrl = "http://creativecommons.org/licenses/by-nd/4.0/"
                }
                ccLicenseDisplay.text = licenseUrl
            }
            project.licenseUrl = licenseUrl
            project.save()
        } ?: run {
            finish()
        }
    }

    fun removeProject(view: View?) {
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        mProject?.delete()
                        mProject = null
                        finish()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
            }
        val message = getString(R.string.action_remove_project)
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        builder.setTitle(R.string.remove_from_app)
            .setMessage(message).setPositiveButton(R.string.action_remove, dialogClickListener)
            .setNegativeButton(R.string.action_cancel, dialogClickListener).show()
    }

    fun archiveProject(view: View?) {
        mProject?.let { project ->
            project.archived = !project.archived
            project.save()

            if (project.archived) {
                mBinding.editProjectLayout.actionArchiveProject.text =
                    getString(R.string.action_unarchive_project)
            } else {
                mBinding.editProjectLayout.actionArchiveProject.text =
                    getString(R.string.action_archive_project)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

}