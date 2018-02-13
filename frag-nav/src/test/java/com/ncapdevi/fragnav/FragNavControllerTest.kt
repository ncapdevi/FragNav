package com.ncapdevi.fragnav

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.widget.FrameLayout
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
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
class FragNavControllerTest : FragNavController.TransactionListener {
    private val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
    private lateinit var mFragNavController: FragNavController

    private var fragmentManager = activity.supportFragmentManager
    private val frameLayout = FrameLayout(activity).apply { id = 1 }

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

    @Test
    fun testConstructionWhenMultipleFragments() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
            .rootFragments(rootFragments)
            .build()

        Assert.assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)
    }

    @Test
    fun testConstructionWhenMultipleFragmentsEagerMode() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
            .rootFragments(rootFragments)
            .fragmentHideStrategy(FragNavController.DETACH_ON_NAVIGATE_HIDE_ON_SWITCH)
            .eager(true)
            .build()

        Assert.assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)
        Assert.assertEquals(mFragNavController.size.toLong(), 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConstructionWhenTooManyRootFragments() {
        val rootFragments = ArrayList<Fragment>()

        for (i in 0..20) {
            rootFragments.add(Fragment())
        }

        mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
            .rootFragments(rootFragments)
            .build()
    }

    @Test
    fun testConstructionWhenMultipleFragmentsAndNoTabSelected() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
            .rootFragments(rootFragments)
            .selectedTabIndex(FragNavController.NO_TAB)
            .build()

        Assert.assertEquals(FragNavController.NO_TAB.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNull(mFragNavController.currentStack)
    }

    @Test
    fun testConstructionWhenRootFragmentListenerAndTabSelected() {
        val rootFragmentListener = mock<FragNavController.RootFragmentListener>()
        doReturn(Fragment()).whenever(rootFragmentListener)
            .getRootFragment(any())

        mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
            .rootFragmentListener(rootFragmentListener, 5)
            .selectedTabIndex(FragNavController.TAB3)
            .build()

        Assert.assertEquals(FragNavController.TAB3.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConstructionWhenRootFragmentListenerAndTooManyTabs() {
        val rootFragmentListener = mock<FragNavController.RootFragmentListener>()

        mFragNavController = FragNavController.newBuilder(null, fragmentManager, frameLayout.id)
            .rootFragmentListener(rootFragmentListener, 21)
            .selectedTabIndex(FragNavController.TAB3)
            .build()
    }


    @Test
    fun pushPopClear() {
        mFragNavController = FragNavController.newBuilder(Bundle(), fragmentManager, frameLayout.id)
            .transactionListener(this)
            .rootFragment(Fragment())
            .build()

        Assert.assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)

        var size = mFragNavController.currentStack!!.size


        mFragNavController.pushFragment(Fragment())
        Assert.assertTrue(mFragNavController.currentStack!!.size == ++size)

        mFragNavController.pushFragment(Fragment())
        Assert.assertTrue(mFragNavController.currentStack!!.size == ++size)

        mFragNavController.pushFragment(Fragment())
        Assert.assertTrue(mFragNavController.currentStack!!.size == ++size)

        mFragNavController.popFragment()
        Assert.assertTrue(mFragNavController.currentStack!!.size == --size)

        mFragNavController.clearStack()
        Assert.assertTrue(mFragNavController.currentStack!!.size == 1)
        Assert.assertTrue(mFragNavController.isRootFragment)
    }


    override fun onTabTransaction(fragment: Fragment?, index: Int) {
        Assert.assertNotNull(fragment)
    }

    override fun onFragmentTransaction(fragment: Fragment?, transactionType: FragNavController.TransactionType) {
        Assert.assertNotNull(mFragNavController)

    }
}