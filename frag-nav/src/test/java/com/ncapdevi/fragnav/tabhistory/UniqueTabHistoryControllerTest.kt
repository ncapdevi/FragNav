package com.ncapdevi.fragnav.tabhistory

import android.os.Bundle
import com.ncapdevi.fragnav.FragNavPopController
import com.ncapdevi.fragnav.FragNavSwitchController
import com.ncapdevi.fragnav.FragNavTransactionOptions
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class UniqueTabHistoryControllerTest {
    @Mock
    private val mockFragNavPopController: FragNavPopController? = null

    @Mock
    private val mockFragNavSwitchController: FragNavSwitchController? = null

    @Mock
    private val mockBundle: Bundle? = null

    @Mock
    private val mockTransactionOptions: FragNavTransactionOptions? = null

    private var uniqueTabHistoryController: UniqueTabHistoryController? = null

    @Before
    fun setUp() {
        uniqueTabHistoryController = UniqueTabHistoryController(mockFragNavPopController!!,
                mockFragNavSwitchController!!)
        mockNavSwitchController(uniqueTabHistoryController!!)
        mockBundle()
    }

    @Test
    fun testNoSwitchWhenCurrentStackIsLargerThanPopCount() {
        // Given
        uniqueTabHistoryController!!.switchTab(1)
        uniqueTabHistoryController!!.switchTab(2)
        `when`(mockFragNavPopController!!.tryPopFragments(eq(1), eq<FragNavTransactionOptions>(mockTransactionOptions))).thenReturn(1)

        // When
        val result = uniqueTabHistoryController!!.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, never()).switchTab(anyInt(), nullable(FragNavTransactionOptions::class.java))
    }

    @Test
    fun testPopDoesNothingWhenPopIsCalledWithNothingToPopWithNoHistory() {
        // Given
        uniqueTabHistoryController!!.switchTab(1)

        // When
        val result = uniqueTabHistoryController!!.popFragments(1, mockTransactionOptions)

        // Then
        assertFalse(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, never()).switchTab(anyInt(), nullable(FragNavTransactionOptions::class.java))
    }

    @Test
    fun testPopSwitchesTabWhenPopIsCalledWithNothingToPopAndHasHistory() {
        // Given
        uniqueTabHistoryController!!.switchTab(1)
        uniqueTabHistoryController!!.switchTab(2)

        // When
        val result = uniqueTabHistoryController!!.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq<FragNavTransactionOptions>(mockTransactionOptions))
    }

    @Test
    fun testSwitchWhenCurrentStackIsNotLargerThanPopCount() {
        // Given
        uniqueTabHistoryController!!.switchTab(1)
        uniqueTabHistoryController!!.switchTab(2)
        `when`(mockFragNavPopController!!.tryPopFragments(eq(2), eq<FragNavTransactionOptions>(mockTransactionOptions))).thenReturn(1)

        // When
        val result = uniqueTabHistoryController!!.popFragments(2, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq<FragNavTransactionOptions>(mockTransactionOptions))
    }

    @Test
    fun testTabsUniquelyRollbackWhenPopAllAvailableItemsInOneStep() {
        // Given

        // Navigating ahead through tabs
        for (i in 1..5) {
            uniqueTabHistoryController!!.switchTab(i)
        }

        // Navigating backwards through tabs
        for (i in 5 downTo 1) {
            uniqueTabHistoryController!!.switchTab(i)
        }

        // Every tab contains 1 item
        run {
            var i = 7
            while (i < 16) {
                `when`(mockFragNavPopController!!.tryPopFragments(eq(i), eq<FragNavTransactionOptions>(mockTransactionOptions))).thenReturn(1)
                i += 2
            }
        }

        // When
        val result = uniqueTabHistoryController!!.popFragments(15, mockTransactionOptions)

        // Then
        assertTrue(result)
        val argumentCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(4)).switchTab(argumentCaptor.capture(), eq<FragNavTransactionOptions>(mockTransactionOptions))
        val allValues = argumentCaptor.allValues

        // The history is [2, 3, 4, 5]
        for (i in 1..4) {
            assertEquals(allValues[i - 1] as Int, i + 1)
        }
    }

    @Test
    fun testTabsUniquelyRollbackWhenPopAllAvailableItemsInDifferentSteps() {
        // Given

        // Navigating ahead through tabs
        for (i in 1..5) {
            uniqueTabHistoryController!!.switchTab(i)
        }

        // Navigating backwards through tabs
        for (i in 5 downTo 1) {
            uniqueTabHistoryController!!.switchTab(i)
        }

        // When
        for (i in 0..3) {
            assertTrue(uniqueTabHistoryController!!.popFragments(1, mockTransactionOptions))
        }
        assertFalse(uniqueTabHistoryController!!.popFragments(1, mockTransactionOptions))

        // Then
        val argumentCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(4)).switchTab(argumentCaptor.capture(), eq<FragNavTransactionOptions>(mockTransactionOptions))
        val allValues = argumentCaptor.allValues

        // The history is [2, 3, 4, 5]
        for (i in 1..4) {
            assertEquals(allValues[i - 1] as Int, i + 1)
        }
    }

    @Test
    fun testHistoryIsSavedAndRestoredWhenSaveCalledNewInstanceCreatedRestoreCalled() {
        // Given
        for (i in 5 downTo 1) {
            uniqueTabHistoryController!!.switchTab(i)
        }

        // When
        uniqueTabHistoryController!!.onSaveInstanceState(mockBundle!!)
        val newUniqueTabHistoryController = UniqueTabHistoryController(
                mockFragNavPopController!!,
                mockFragNavSwitchController!!)
        mockNavSwitchController(newUniqueTabHistoryController)
        newUniqueTabHistoryController.restoreFromBundle(mockBundle)
        for (i in 0..3) {
            assertTrue(newUniqueTabHistoryController.popFragments(1, mockTransactionOptions))
        }

        // Then
        val argumentCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify(mockFragNavSwitchController, times(4)).switchTab(argumentCaptor.capture(), eq<FragNavTransactionOptions>(mockTransactionOptions))
        val allValues = argumentCaptor.allValues

        // The history is [2, 3, 4, 5]
        for (i in 1..4) {
            assertEquals(allValues[i - 1] as Int, i + 1)
        }
    }

    private fun mockNavSwitchController(uniqueTabHistoryController: UniqueTabHistoryController) {
        doAnswer { invocation ->
            uniqueTabHistoryController.switchTab(invocation.getArgument<Any>(0) as Int)
            null
        }.`when`<FragNavSwitchController>(mockFragNavSwitchController).switchTab(anyInt(), nullable(FragNavTransactionOptions::class.java))
    }

    private fun mockBundle() {
        val storage = ArrayList<Int>()
        doAnswer { invocation ->
            storage.clear()
            storage.addAll(invocation.getArgument<Any>(1) as ArrayList<Int>)
            null
        }.`when`<Bundle>(mockBundle).putIntegerArrayList(anyString(), any<ArrayList<Int>>())
        doAnswer {
            if (storage.size > 0) {
                storage
            } else null
        }.`when`<Bundle>(mockBundle).getIntegerArrayList(anyString())
    }
}