package com.ncapdevi.sample.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import com.ncapdevi.sample.R

/**
 * Created by niccapdevila on 3/26/16.
 */
open class BaseFragment : Fragment() {

    lateinit var btn: Button
    lateinit var mFragmentNavigation: FragmentNavigation
    internal var mInt = 0
    private var cachedView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            mInt = args.getInt(ARGS_INSTANCE)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (cachedView == null) {
            cachedView = inflater.inflate(R.layout.fragment_main, container, false)
            btn = cachedView!!.findViewById(R.id.button)
        }
        return cachedView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is FragmentNavigation) {
            mFragmentNavigation = context
        }
    }

    interface FragmentNavigation {
        fun pushFragment(fragment: Fragment)
    }

    companion object {
        val ARGS_INSTANCE = "com.ncapdevi.sample.argsInstance"
    }
}
