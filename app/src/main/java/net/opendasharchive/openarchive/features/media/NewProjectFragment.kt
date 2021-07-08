package net.opendasharchive.openarchive.features.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.databinding.FragmentNewProjectBinding

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewProjectFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NewProjectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
class NewProjectFragment : Fragment() {

    private var _mBinding: FragmentNewProjectBinding? = null

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentNewProjectBinding.inflate(inflater, container, false)
        return _mBinding?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri?)
    }

}