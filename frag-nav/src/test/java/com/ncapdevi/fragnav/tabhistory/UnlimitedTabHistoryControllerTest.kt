package com.ncapdevi.fragnav.tabhistory

import android.os.Bundle
import com.ncapdevi.fragnav.FragNavPopController
import com.ncapdevi.fragnav.FragNavSwitchController
import com.ncapdevi.fragnav.FragNavTransactionOptions
import com.nhaarman.mockitokotlin2.*
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

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
        unlimitedTabHistoryController =
                UnlimitedTabHistoryController(mockFragNavPopController, mockFragNavSwitchController)
        mockNavSwitchController(unlimitedTabHistoryController)
        mockBundle()
    }

    @Test
    fun testNoSwitchWhenCurrentStackIsLargerThanPopCount() {
        // Given
        unlimitedTabHistoryController.switchTab(1)
        unlimitedTabHistoryController.switchTab(2)
        whenever(mockFragNavPopController.tryPopFragments(eq(1), eq(mockTransactionOptions))).thenReturn(1)

        // When
        val result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify(mockFragNavSwitchController, never()).switchTab(any(), anyOrNull())
    }

    @Test
    fun testPopDoesNothingWhenPopIsCalledWithNothingToPopWithNoHistory() {
        // Given
        unlimitedTabHistoryController.switchTab(1)

        // When
        val result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertFalse(result)
        verify(mockFragNavSwitchController, never()).switchTab(any(), anyOrNull())
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
        verify(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq(mockTransactionOptions))
    }

    @Test
    fun testSwitchWhenCurrentStackIsNotLargerThanPopCount() {
        // Given
        unlimitedTabHistoryController.switchTab(1)
        unlimitedTabHistoryController.switchTab(2)
        whenever(mockFragNavPopController.tryPopFragments(eq(2), eq(mockTransactionOptions))).thenReturn(1)

        // When
        val result = unlimitedTabHistoryController.popFragments(2, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq(mockTransactionOptions))
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
        var index = 7
        while (index < 16) {
            whenever(mockFragNavPopController.tryPopFragments(eq(index), eq(mockTransactionOptions))).thenReturn(1)
            index += 2
        }

        // When
        val result = unlimitedTabHistoryController.popFragments(15, mockTransactionOptions)

        // Then
        assertTrue(result)
        argumentCaptor<Int>().apply {
            verify(mockFragNavSwitchController, times(9)).switchTab(capture(), eq(mockTransactionOptions))

            // The history is [2, 3, 4, 5, 5, 4, 3, 2, 1]
            for (i in 1..4) {
                assertEquals(allValues[i - 1], i + 1)
            }
            for (i in 4..8) {
                assertEquals(allValues[i], 9 - i)
            }
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
        argumentCaptor<Int>().apply {
            verify(mockFragNavSwitchController, times(9)).switchTab(
                capture(),
                eq(mockTransactionOptions)
            )

            // The history is [2, 3, 4, 5, 5, 4, 3, 2, 1]
            for (i in 1..4) {
                assertEquals(allValues[i - 1], i + 1)
            }
            for (i in 4..8) {
                assertEquals(allValues[i], 9 - i)
            }
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
            mockFragNavSwitchController
        )
        mockNavSwitchController(newUniqueTabHistoryController)
        newUniqueTabHistoryController.restoreFromBundle(mockBundle)
        for (i in 0..3) {
            assertTrue(newUniqueTabHistoryController.popFragments(1, mockTransactionOptions))
        }

        // Then
        argumentCaptor<Int>().apply {
            verify(mockFragNavSwitchController, times(4)).switchTab(
                capture(),
                eq(mockTransactionOptions)
            )

            // The history is [2, 3, 4, 5]
            for (i in 1..4) {
                assertEquals(allValues[i - 1], i + 1)
            }
        }
    }

    private fun mockNavSwitchController(uniqueTabHistoryController: UnlimitedTabHistoryController) {
        doAnswer { invocation ->
            uniqueTabHistoryController.switchTab(invocation.getArgument<Any>(0) as Int)
            null
        }.whenever(mockFragNavSwitchController).switchTab(any(), anyOrNull())
    }

    private fun mockBundle() {
        val storage = ArrayList<Int>()
        doAnswer { invocation ->
            storage.clear()
            storage.addAll(invocation.getArgument<ArrayList<Int>>(1))
            null
        }.whenever(mockBundle).putIntegerArrayList(any(), any())
        doAnswer {
            if (storage.size > 0) {
                storage
            } else null
        }.whenever(mockBundle).getIntegerArrayList(any())
    }
}