package com.ncapdevi.fragnav

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.widget.FrameLayout
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.*


/**
 * Created by niccapdevila on 2/10/18.
 */
@RunWith(RobolectricTestRunner::class)
class NavigationRoboelectricTest {
    val activity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create().get()

    var fragmentManager = activity.supportFragmentManager
    val frameLayout = FrameLayout(activity).apply { id = 1 }

    @Before
    fun setup() {
        activity.setContentView(frameLayout)

    }

    @Test
    fun testConstructionWhenRestoringFromBundle() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        var mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
                .rootFragments(rootFragments)
                .selectedTabIndex(FragNavController.TAB1)
                .build()

        mFragNavController.switchTab(FragNavController.TAB2)
        mFragNavController.pushFragment(Fragment())
        mFragNavController.pushFragment(Fragment())
        mFragNavController.pushFragment(Fragment())

        val currentFragment = mFragNavController.currentFrag

        val bundle = Bundle()

        mFragNavController.onSaveInstanceState(bundle)

        mFragNavController = FragNavController.newBuilder(bundle, fragmentManager, frameLayout.id)
                .rootFragments(rootFragments)
                .selectedTabIndex(FragNavController.TAB1)
                .build()

        Assert.assertEquals(FragNavController.TAB2.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertEquals(3, mFragNavController.currentStack!!.size.toLong())
        Assert.assertEquals(currentFragment, mFragNavController.currentFrag)
    }
}