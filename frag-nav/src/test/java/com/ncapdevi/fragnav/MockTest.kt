package com.ncapdevi.fragnav

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class MockTest : FragNavController.TransactionListener {

    @Mock
    private lateinit var mFragmentManager: FragmentManager
    @Mock
    private lateinit var mBundle: Bundle
    @Mock
    private lateinit var mFragmentTransaction: FragmentTransaction

    private lateinit var mFragNavController: FragNavController

    private val fragmentTagMap: MutableMap<String, Fragment> = mutableMapOf()

    @Before
    fun initMocks() {
        mockFragmentManager()
        mockFragmentTransaction()
    }

    private fun mockFragmentTransaction() {
        `when`(mFragmentTransaction.add(ArgumentMatchers.anyInt(), ArgumentMatchers.any(Fragment::class.java), ArgumentMatchers.anyString())).then { invocation ->
            val args = invocation.arguments
            fragmentTagMap[args[2] as String] = args[1] as Fragment
            mFragmentTransaction
        }
    }

    @SuppressLint("CommitTransaction")
    private fun mockFragmentManager() {
        `when`(mFragmentManager.fragments).thenReturn(fragmentTagMap.values.toMutableList())

        `when`(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction)
        `when`(mFragmentManager.findFragmentByTag(ArgumentMatchers.anyString())).then({ invocation ->
            return@then fragmentTagMap[invocation.arguments[0]]
        })
    }

    @Test
    fun testConstructionWhenMultipleFragments() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .build()

        assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        assertNotNull(mFragNavController.currentStack)
    }

    @Test
    fun testConstructionWhenMultipleFragmentsEagerMode() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .fragmentHideStrategy(FragNavController.DETACH_ON_NAVIGATE_HIDE_ON_SWITCH)
                .eager(true)
                .build()

        assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        assertNotNull(mFragNavController.currentStack)
        assertEquals(mFragNavController.size.toLong(), 2)
        verify<FragmentTransaction>(mFragmentTransaction, times(2)).add(ArgumentMatchers.anyInt(), ArgumentMatchers.any(Fragment::class.java), ArgumentMatchers.anyString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConstructionWhenTooManyRootFragments() {
        val rootFragments = ArrayList<Fragment>()

        for (i in 0..20) {
            rootFragments.add(Fragment())
        }

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .build()
    }

    @Test
    fun testConstructionWhenMultipleFragmentsAndNoTabSelected() {
        val rootFragments = ArrayList<Fragment>()
        rootFragments.add(Fragment())
        rootFragments.add(Fragment())

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .selectedTabIndex(FragNavController.NO_TAB)
                .build()

        assertEquals(FragNavController.NO_TAB.toLong(), mFragNavController.currentStackIndex.toLong())
        assertNull(mFragNavController.currentStack)
    }

    @Test
    fun testConstructionWhenRootFragmentListenerAndTabSelected() {
        val rootFragmentListener = mock(FragNavController.RootFragmentListener::class.java)
        doReturn(Fragment()).`when`<FragNavController.RootFragmentListener>(rootFragmentListener).getRootFragment(ArgumentMatchers.anyInt())

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragmentListener(rootFragmentListener, 5)
                .selectedTabIndex(FragNavController.TAB3)
                .build()

        assertEquals(FragNavController.TAB3.toLong(), mFragNavController.currentStackIndex.toLong())
        assertNotNull(mFragNavController.currentStack)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConstructionWhenRootFragmentListenerAndTooManyTabs() {
        val rootFragmentListener = mock(FragNavController.RootFragmentListener::class.java)

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragmentListener(rootFragmentListener, 21)
                .selectedTabIndex(FragNavController.TAB3)
                .build()
    }



    @Test
    fun pushPopClear() {
        mFragNavController = FragNavController.newBuilder(mBundle, mFragmentManager, 1)
                .transactionListener(this)
                .rootFragment(mock(Fragment::class.java))
                .build()

        assertEquals(FragNavController.TAB1.toLong(), mFragNavController.currentStackIndex.toLong())
        assertNotNull(mFragNavController.currentStack)

        var size = mFragNavController.currentStack!!.size

        mFragNavController.pushFragment(mock(Fragment::class.java))
        assertTrue(mFragNavController.currentStack!!.size == ++size)

        mFragNavController.pushFragment(mock(Fragment::class.java))
        assertTrue(mFragNavController.currentStack!!.size == ++size)

        mFragNavController.pushFragment(mock(Fragment::class.java))
        assertTrue(mFragNavController.currentStack!!.size == ++size)

        mFragNavController.popFragment()
        assertTrue(mFragNavController.currentStack!!.size == --size)

        mFragNavController.clearStack()
        assertTrue(mFragNavController.currentStack!!.size == 1)
        assertTrue(mFragNavController.isRootFragment)
    }

    override fun onTabTransaction(fragment: Fragment?, index: Int) {
        assertNotNull(fragment)
    }

    override fun onFragmentTransaction(fragment: Fragment?, transactionType: FragNavController.TransactionType) {
        assertNotNull(mFragNavController)

    }
}