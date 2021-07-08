package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentNavigationDrawerBinding

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
class NavigationDrawerFragment : Fragment() {

    /**
     * Remember the position of the selected item.
     */
    private val STATE_SELECTED_POSITION = "selected_navigation_drawer_position"

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private var mCallbacks: NavigationDrawerCallbacks? = null

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private val mDrawerToggle: ActionBarDrawerToggle? = null

    private var mCurrentSelectedPosition = 0
    private var mFromSavedInstanceState = false
    private var mDrawerLayout: DrawerLayout? = null
    private var mFragmentContainerView: View? = null

    private var _mBinding: FragmentNavigationDrawerBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentNavigationDrawerBinding.inflate(inflater, container, false)
        return _mBinding?.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION)
            mFromSavedInstanceState = true
        }
        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle?.onConfigurationChanged(newConfig)
    }

    private fun initLayout() {
        _mBinding?.let { mBinding ->
            mBinding.drawerList.setOnItemClickListener { parent, view, position, id ->
                selectItem(position)
            }

            getActionBar()?.let {
                mBinding.drawerList.adapter = ArrayAdapter(
                    it.themedContext,
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    resources.getStringArray(R.array.ar_title_menu_sections)
                )
            }

            mBinding.drawerList.setItemChecked(mCurrentSelectedPosition, true)
        }
    }

    fun isDrawerOpen(): Boolean {
        return mDrawerLayout != null && mFragmentContainerView != null && mDrawerLayout?.isDrawerOpen(
            mFragmentContainerView!!
        ) == true
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    private fun setUp(fragmentId: Int, drawerLayout: DrawerLayout) {
        mFragmentContainerView = requireActivity().findViewById(fragmentId)
        mDrawerLayout = drawerLayout

        // set a custom shadow that overlays the main content when the drawer opens

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout?.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        // set up the drawer's list view with items and click listener
        getActionBar()?.setDisplayHomeAsUpEnabled(true)
        getActionBar()?.setHomeButtonEnabled(true)
        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout?.post { mDrawerToggle?.syncState() }

        mDrawerLayout?.setDrawerListener(mDrawerToggle)
    }

    private fun selectItem(position: Int) {
        mCurrentSelectedPosition = position
        _mBinding?.drawerList?.setItemChecked(position, true)
        mDrawerLayout?.closeDrawer(mFragmentContainerView!!)
        mCallbacks?.onNavigationDrawerItemSelected(position)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = try {
            activity as NavigationDrawerCallbacks
        } catch (e: ClassCastException) {
            throw ClassCastException("Activity must implement NavigationDrawerCallbacks.")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (mDrawerToggle?.onOptionsItemSelected(item) == true) {
            true
        } else super.onOptionsItemSelected(item)
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private fun showGlobalContextActionBar() {
        val actionBar = getActionBar()
        actionBar?.let {
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.navigationMode = ActionBar.NAVIGATION_MODE_STANDARD
            actionBar.setTitle(R.string.app_name)
        }
    }

    private fun getActionBar(): ActionBar? {
        return (activity as AppCompatActivity).supportActionBar
    }

    //Callbacks interface that all activities using this fragment must implement.
    interface NavigationDrawerCallbacks {
        fun onNavigationDrawerItemSelected(position: Int)
    }

}