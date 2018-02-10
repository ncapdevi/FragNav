package com.ncapdevi.sample.fragments

import android.os.Bundle
import android.view.View

/**
 * Created by niccapdevila on 3/26/16.
 */
class RecentsFragment : BaseFragment() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn.setOnClickListener {
            mFragmentNavigation.pushFragment(RecentsFragment.newInstance(mInt + 1))
        }
        btn.text = javaClass.simpleName + " " + mInt
    }

    companion object {

        fun newInstance(instance: Int): RecentsFragment {
            val args = Bundle()
            args.putInt(BaseFragment.ARGS_INSTANCE, instance)
            val fragment = RecentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
