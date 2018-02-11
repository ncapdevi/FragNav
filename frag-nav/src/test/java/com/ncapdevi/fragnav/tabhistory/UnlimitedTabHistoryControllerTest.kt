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
class UnlimitedTabHistoryControllerTest {
    @Mock
    private lateinit var mockFragNavPopController: FragNavPopController

    @Mock
    private lateinit var mockFragNavSwitchController: FragNavSwitchController

    @Mock
    private lateinit var mockBundle: Bundle

    @Mock
    private lateinit var mockTransactionOptions: FragNavTransactionOptions

    private lateinit var unlimitedTabHistoryController: UnlimitedTabHistoryController

    @Before
    fun setUp() {
        unlimitedTabHistoryController = UnlimitedTabHistoryController(mockFragNavPopController,
                mockFragNavSwitchController)
        mockNavSwitchController(unlimitedTabHistoryController)
        mockBundle()
    }

    @Test
    fun testNoSwitchWhenCurrentStackIsLargerThanPopCount() {
        // Given
        unlimitedTabHistoryController.switchTab(1)
        unlimitedTabHistoryController.switchTab(2)
        `when`(mockFragNavPopController.tryPopFragments(eq(1), eq<FragNavTransactionOptions>(mockTransactionOptions))).thenReturn(1)

        // When
        val result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, never()).switchTab(anyInt(), nullable(FragNavTransactionOptions::class.java))
    }

    @Test
    fun testPopDoesNothingWhenPopIsCalledWithNothingToPopWithNoHistory() {
        // Given
        unlimitedTabHistoryController.switchTab(1)

        // When
        val result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertFalse(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, never()).switchTab(anyInt(), nullable(FragNavTransactionOptions::class.java))
    }

    @Test
    fun testPopSwitchesTabWhenPopIsCalledWithNothingToPopAndHasHistory() {
        // Given
        unlimitedTabHistoryController.switchTab(1)
        unlimitedTabHistoryController.switchTab(2)

        // When
        val result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq<FragNavTransactionOptions>(mockTransactionOptions))
    }

    @Test
    fun testSwitchWhenCurrentStackIsNotLargerThanPopCount() {
        // Given
        unlimitedTabHistoryController.switchTab(1)
        unlimitedTabHistoryController.switchTab(2)
        `when`(mockFragNavPopController.tryPopFragments(eq(2), eq<FragNavTransactionOptions>(mockTransactionOptions))).thenReturn(1)

        // When
        val result = unlimitedTabHistoryController.popFragments(2, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq<FragNavTransactionOptions>(mockTransactionOptions))
    }

    @Test
    fun testTabsUnlimitedRollbackWhenPopAllAvailableItemsInOneStep() {
        // Given

        // Navigating ahead through tabs
        for (i in 1..5) {
            unlimitedTabHistoryController.switchTab(i)
        }

        // Navigating backwards through tabs
        for (i in 5 downTo 1) {
            unlimitedTabHistoryController.switchTab(i)
        }

        // Every tab contains 1 item
        run {
            var i = 7
            while (i < 16) {
                `when`(mockFragNavPopController.tryPopFragments(eq(i), eq<FragNavTransactionOptions>(mockTransactionOptions))).thenReturn(1)
                i += 2
            }
        }

        // When
        val result = unlimitedTabHistoryController.popFragments(15, mockTransactionOptions)

        // Then
        assertTrue(result)
        val argumentCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(9)).switchTab(argumentCaptor.capture(), eq<FragNavTransactionOptions>(mockTransactionOptions))
        val allValues = argumentCaptor.allValues

        // The history is [2, 3, 4, 5, 5, 4, 3, 2, 1]
        for (i in 1..4) {
            assertEquals(allValues[i - 1] as Int, i + 1)
        }
        for (i in 4..8) {
            assertEquals(allValues[i] as Int, 9 - i)
        }
    }

    @Test
    fun testTabsUnlimitedRollbackWhenPopAllAvailableItemsInDifferentSteps() {
        // Given

        // Navigating ahead through tabs
        for (i in 1..5) {
            unlimitedTabHistoryController.switchTab(i)
        }

        // Navigating backwards through tabs
        for (i in 5 downTo 1) {
            unlimitedTabHistoryController.switchTab(i)
        }

        // When
        for (i in 0..8) {
            assertTrue(unlimitedTabHistoryController.popFragments(1, mockTransactionOptions))
        }
        assertFalse(unlimitedTabHistoryController.popFragments(1, mockTransactionOptions))

        // Then
        val argumentCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify<FragNavSwitchController>(mockFragNavSwitchController, times(9)).switchTab(argumentCaptor.capture(), eq<FragNavTransactionOptions>(mockTransactionOptions))
        val allValues = argumentCaptor.allValues

        // The history is [2, 3, 4, 5, 5, 4, 3, 2, 1]
        for (i in 1..4) {
            assertEquals(allValues[i - 1] as Int, i + 1)
        }
        for (i in 4..8) {
            assertEquals(allValues[i] as Int, 9 - i)
        }
    }

    @Test
    fun testHistoryIsSavedAndRestoredWhenSaveCalledNewInstanceCreatedRestoreCalled() {
        // Given
        for (i in 5 downTo 1) {
            unlimitedTabHistoryController.switchTab(i)
        }

        // When
        unlimitedTabHistoryController.onSaveInstanceState(mockBundle)
        val newUniqueTabHistoryController = UnlimitedTabHistoryController(
                mockFragNavPopController,
                mockFragNavSwitchController)
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

    private fun mockNavSwitchController(uniqueTabHistoryController: UnlimitedTabHistoryController) {
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