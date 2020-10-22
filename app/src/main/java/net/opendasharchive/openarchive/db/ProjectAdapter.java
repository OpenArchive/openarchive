package net.opendasharchive.openarchive.db;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.fragments.MediaGridFragment;
import net.opendasharchive.openarchive.fragments.MediaListFragment;
import net.opendasharchive.openarchive.fragments.NewProjectFragment;
import net.opendasharchive.openarchive.util.SmartFragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by micahjlucas on 1/20/15.
 */
public class ProjectAdapter extends SmartFragmentStatePagerAdapter {

    private List<Project> data;
    private Activity context;

    public ProjectAdapter(Activity context, FragmentManager fm) {
        super(fm);
        this.context = context;

    }

    @Override
    public Fragment getItem(int i) {

        if (i == 0)
        {
            NewProjectFragment frag = new NewProjectFragment();
            return frag;
        }
        else {
            MediaListFragment fragment = new MediaGridFragment();
            fragment.setProjectId(getProject(i).getId());
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }
    }

    public Project getProject (int i)
    {
        if (data != null && i > 0)
            return data.get(i-1);
        else
            return null;
    }

    public void updateData (List<Project> data)
    {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (data != null)
            return data.size()+1;
        else
            return 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        if (position == 0)
        {
            ImageSpan imageSpan = new ImageSpan(context, R.drawable.ic_add_circle_outline_black_24dp);
            SpannableString spannableString = new SpannableString(" ");

            int start = 0;
            int end = 1;
            int flag = 0;
            spannableString.setSpan(imageSpan, start, end, flag);

            return spannableString;
        }
        else
            return data.get(position-1).description;
    }




}