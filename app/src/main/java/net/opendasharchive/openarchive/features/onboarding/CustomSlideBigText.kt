package net.opendasharchive.openarchive.features.onboarding

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.R

class CustomSlideBigText : Fragment() {

    private var layoutResId = 0
    private var mTitle: String? = null
    private var mButtonText: String? = null
    private var mSubTitle: String? = null
    private var mButtonListener: View.OnClickListener? = null

    fun setTitle(title: String) {
        mTitle = title
    }

    fun setSubTitle(subTitle: String) {
        mSubTitle = subTitle
    }

    fun showButton(buttonText: String, buttonListener: View.OnClickListener) {
        mButtonText = buttonText
        mButtonListener = buttonListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        arguments?.let {
            if (it.containsKey(ARG_LAYOUT_RES_ID)) {
                layoutResId = it.getInt(ARG_LAYOUT_RES_ID)
            }
        }

        val view = inflater.inflate(layoutResId, container, false)
        (view.findViewById(R.id.custom_slide_big_text) as TextView).text = mTitle

        if (!TextUtils.isEmpty(mSubTitle)) {
            val tv = view.findViewById(R.id.custom_slide_big_text_sub) as TextView
            tv.text = mSubTitle
            tv.visibility = View.VISIBLE
        }

        if (mButtonText != null) {
            val button = view.findViewById(R.id.custom_slide_button) as Button
            button.visibility = View.VISIBLE
            button.text = mButtonText
            button.setOnClickListener(mButtonListener)
        }
        return view
    }

    companion object {
        private const val ARG_LAYOUT_RES_ID = "layoutResId"

        fun newInstance(@LayoutRes layoutResId: Int): CustomSlideBigText = CustomSlideBigText().also {
            val bundle = Bundle()
            bundle.putInt(ARG_LAYOUT_RES_ID, layoutResId)
            it.arguments = bundle
        }

    }

}