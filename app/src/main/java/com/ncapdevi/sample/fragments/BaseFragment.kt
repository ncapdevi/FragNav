package com.ncapdevi.sample.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            mInt = args.getInt(ARGS_INSTANCE)
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)?.apply {
            btn = findViewById(R.id.button)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is FragmentNavigation) {
            mFragmentNavigation = context
        }
    }

    interface FragmentNavigation {
        fun pushFragment(fragment: Fragment, sharedElementList: List<Pair<View, String>>?= null)
    }

    companion object {
       const val ARGS_INSTANCE = "com.ncapdevi.sample.argsInstance"
    }
}
