package com.ncapdevi.fragnav

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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

        val mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragments = rootFragments
        }
        mFragNavController.initialize()


        mFragNavController.switchTab(FragNavController.TAB2)
        mFragNavController.pushFragment(Fragment())
        mFragNavController.pushFragment(Fragment())
        mFragNavController.pushFragment(Fragment())

        val currentFragment = mFragNavController.currentFrag

        val bundle = Bundle()

        mFragNavController.onSaveInstanceState(bundle)

        mFragNavController.initialize(savedInstanceState = bundle)

        Assert.assertEquals(FragNavController.TAB2.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertEquals(4, mFragNavController.currentStack!!.size.toLong())
        Assert.assertEquals(currentFragment, mFragNavController.currentFrag)
    }

    @Test
    fun testConstructionWhenMultipleFragments() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragments = rootFragments

        }
        mFragNavController.initialize()

        Assert.assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)
    }

    @Test
    fun testConstructionWhenMultipleFragmentsEagerMode() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragments = rootFragments
            fragmentHideStrategy = FragNavController.DETACH_ON_NAVIGATE_HIDE_ON_SWITCH
            createEager = true

        }

        mFragNavController.initialize()

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

        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragments = rootFragments
        }
    }

    @Test
    fun testConstructionWhenMultipleFragmentsAndNoTabSelected() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragments = rootFragments
        }

        mFragNavController.initialize(FragNavController.NO_TAB)


        Assert.assertEquals(FragNavController.NO_TAB.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNull(mFragNavController.currentStack)
    }

    @Test
    fun testConstructionWhenRootFragmentListenerAndTabSelected() {
        val rootFragmentListener = mock<FragNavController.RootFragmentListener>()
        doReturn(Fragment()).whenever(rootFragmentListener)
                .getRootFragment(any())
        doReturn(5).whenever(rootFragmentListener).numberOfRootFragments

        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragmentListener = rootFragmentListener
        }
        mFragNavController.initialize(FragNavController.TAB3)

        Assert.assertEquals(FragNavController.TAB3.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun testConstructionWhenRootFragmentListenerAndTooManyTabs() {
        val rootFragmentListener = mock<FragNavController.RootFragmentListener>()

        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragmentListener = rootFragmentListener

        }
        mFragNavController.initialize(FragNavController.TAB20)
    }


    @Test
    fun pushPopClear() {
        mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            transactionListener = this@FragNavControllerTest
            rootFragments = listOf(Fragment())
        }

        mFragNavController.initialize()

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

    @Test
    fun testTabStackClear() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        val mFragNavController = FragNavController(fragmentManager, frameLayout.id).apply {
            this.rootFragments = rootFragments
        }
        mFragNavController.initialize()

        Assert.assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        Assert.assertNotNull(mFragNavController.currentStack)

        var size = mFragNavController.currentStack?.size ?: 1

        mFragNavController.pushFragment(Fragment())
        Assert.assertTrue(mFragNavController.currentStack?.size == ++size)

        mFragNavController.pushFragment(Fragment())
        Assert.assertTrue(mFragNavController.currentStack?.size == ++size)

        mFragNavController.switchTab(FragNavController.TAB2)

        mFragNavController.clearStack(FragNavController.TAB1)

        mFragNavController.switchTab(FragNavController.TAB1)
        Assert.assertTrue(mFragNavController.currentStack?.size == 1)
    }

    override fun onTabTransaction(fragment: Fragment?, index: Int) {
        Assert.assertNotNull(fragment)
    }

    override fun onFragmentTransaction(fragment: Fragment?, transactionType: FragNavController.TransactionType) {
        Assert.assertNotNull(mFragNavController)

    }
}