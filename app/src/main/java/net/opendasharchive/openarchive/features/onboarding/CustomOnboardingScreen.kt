package net.opendasharchive.openarchive.features.onboarding

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.R

class CustomOnboardingScreen : Fragment() {

    private var layoutResId = 0
    private var mTitle: String? = null
    private var mSubTitle: String? = null
    private var mImageResource = 0

    private var mButton: Button? = null
    private var mButtonText: String? = null
    private var mButtonListener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_LAYOUT_RES_ID)) {
                layoutResId = it.getInt(ARG_LAYOUT_RES_ID)
            }
            if (it.containsKey(ARG_LAYOUT_TITLE)) {
                mTitle = it.getString(ARG_LAYOUT_TITLE)
            }
            if (it.containsKey(ARG_LAYOUT_SUBTITLE)) {
                mSubTitle = it.getString(ARG_LAYOUT_SUBTITLE)
            }
            if (it.containsKey(ARG_LAYOUT_IMAGE)) {
                mImageResource = it.getInt(ARG_LAYOUT_IMAGE)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layoutResId, container, false)
        (view.findViewById(R.id.custom_slide_big_text) as TextView).text = mTitle

        if (!TextUtils.isEmpty(mSubTitle)) {
            val tv = view.findViewById(R.id.custom_slide_big_text_sub) as TextView
            tv.text = mSubTitle
            tv.visibility = View.VISIBLE
        }

        (view.findViewById(R.id.custom_slide_image) as ImageView).setImageResource(mImageResource)

        mButton = view.findViewById(R.id.custom_slide_button)
        if (mButton != null && mButtonListener != null) {
            mButton?.text = mButtonText
            mButton?.visibility = View.VISIBLE
            mButton?.setOnClickListener(mButtonListener)
        }
        return view
    }

    fun enableButton(buttonText: String, listener: View.OnClickListener) {
        mButtonText = buttonText
        mButtonListener = listener
    }

    companion object {

        private const val ARG_LAYOUT_RES_ID = "layoutResId"
        private const val ARG_LAYOUT_TITLE = "layoutTitle"
        private const val ARG_LAYOUT_SUBTITLE = "layoutSubtitle"
        private const val ARG_LAYOUT_IMAGE = "layoutImage"

        fun newInstance(layoutResId: Int, title: String, subtitle: String, image: Int): CustomOnboardingScreen = CustomOnboardingScreen().also {
            it.arguments = Bundle().apply {
                putInt(ARG_LAYOUT_RES_ID, layoutResId)
                putString(ARG_LAYOUT_TITLE, title)
                putString(ARG_LAYOUT_SUBTITLE, subtitle)
                putInt(ARG_LAYOUT_IMAGE, image)
            }
        }

    }
}