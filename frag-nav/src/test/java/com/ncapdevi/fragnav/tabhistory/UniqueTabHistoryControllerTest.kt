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
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class UniqueTabHistoryControllerTest {
    @Mock
    private lateinit var mockFragNavPopController: FragNavPopController

    @Mock
    private lateinit var mockFragNavSwitchController: FragNavSwitchController

    @Mock
    private lateinit var mockBundle: Bundle

    @Mock
    private lateinit var mockTransactionOptions: FragNavTransactionOptions

    private lateinit var uniqueTabHistoryController: UniqueTabHistoryController

    @Before
    fun setUp() {
        uniqueTabHistoryController = UniqueTabHistoryController(mockFragNavPopController, mockFragNavSwitchController)
        mockNavSwitchController(uniqueTabHistoryController)
        mockBundle()
    }

    @Test
    fun testNoSwitchWhenCurrentStackIsLargerThanPopCount() {
        // Given
        uniqueTabHistoryController.switchTab(1)
        uniqueTabHistoryController.switchTab(2)
        whenever(mockFragNavPopController.tryPopFragments(eq(1), eq(mockTransactionOptions))).thenReturn(1)

        // When
        val result = uniqueTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify(mockFragNavSwitchController, never()).switchTab(any(), anyOrNull())
    }

    @Test
    fun testPopDoesNothingWhenPopIsCalledWithNothingToPopWithNoHistory() {
        // Given
        uniqueTabHistoryController.switchTab(1)

        // When
        val result = uniqueTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertFalse(result)
        verify(mockFragNavSwitchController, never()).switchTab(any(), anyOrNull())
    }

    @Test
    fun testPopSwitchesTabWhenPopIsCalledWithNothingToPopAndHasHistory() {
        // Given
        uniqueTabHistoryController.switchTab(1)
        uniqueTabHistoryController.switchTab(2)

        // When
        val result = uniqueTabHistoryController.popFragments(1, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq(mockTransactionOptions))
    }

    @Test
    fun testSwitchWhenCurrentStackIsNotLargerThanPopCount() {
        // Given
        uniqueTabHistoryController.switchTab(1)
        uniqueTabHistoryController.switchTab(2)
        whenever(mockFragNavPopController.tryPopFragments(eq(2), eq(mockTransactionOptions))).thenReturn(1)

        // When
        val result = uniqueTabHistoryController.popFragments(2, mockTransactionOptions)

        // Then
        assertTrue(result)
        verify(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq(mockTransactionOptions))
    }

    @Test
    fun testTabsUniquelyRollbackWhenPopAllAvailableItemsInOneStep() {
        // Given

        // Navigating ahead through tabs
        for (i in 1..5) {
            uniqueTabHistoryController.switchTab(i)
        }

        // Navigating backwards through tabs
        for (i in 5 downTo 1) {
            uniqueTabHistoryController.switchTab(i)
        }

        // Every tab contains 1 item
        run {
            var i = 7
            while (i < 16) {
                whenever(mockFragNavPopController.tryPopFragments(eq(i), eq(mockTransactionOptions))).thenReturn(1)
                i += 2
            }
        }

        // When
        val result = uniqueTabHistoryController.popFragments(15, mockTransactionOptions)

        // Then
        assertTrue(result)
        argumentCaptor<Int>().apply {
            verify(mockFragNavSwitchController, times(4)).switchTab(capture(), eq(mockTransactionOptions))

            // The history is [2, 3, 4, 5]
            for (i in 1..4) {
                assertEquals(allValues[i - 1], i + 1)
            }
        }
    }

    @Test
    fun testTabsUniquelyRollbackWhenPopAllAvailableItemsInDifferentSteps() {
        // Given

        // Navigating ahead through tabs
        for (i in 1..5) {
            uniqueTabHistoryController.switchTab(i)
        }

        // Navigating backwards through tabs
        for (i in 5 downTo 1) {
            uniqueTabHistoryController.switchTab(i)
        }

        // When
        for (i in 0..3) {
            assertTrue(uniqueTabHistoryController.popFragments(1, mockTransactionOptions))
        }
        assertFalse(uniqueTabHistoryController.popFragments(1, mockTransactionOptions))

        // Then
        argumentCaptor<Int>().apply {
            verify(mockFragNavSwitchController, times(4)).switchTab(capture(), eq(mockTransactionOptions))

            // The history is [2, 3, 4, 5]
            for (i in 1..4) {
                assertEquals(allValues[i - 1], i + 1)
            }
        }
    }

    @Test
    fun testHistoryIsSavedAndRestoredWhenSaveCalledNewInstanceCreatedRestoreCalled() {
        // Given
        for (i in 5 downTo 1) {
            uniqueTabHistoryController.switchTab(i)
        }

        // When
        uniqueTabHistoryController.onSaveInstanceState(mockBundle)
        val newUniqueTabHistoryController =
            UniqueTabHistoryController(mockFragNavPopController, mockFragNavSwitchController)
        mockNavSwitchController(newUniqueTabHistoryController)
        newUniqueTabHistoryController.restoreFromBundle(mockBundle)
        for (i in 0..3) {
            assertTrue(newUniqueTabHistoryController.popFragments(1, mockTransactionOptions))
        }

        // Then
        argumentCaptor<Int>().apply {
            verify(mockFragNavSwitchController, times(4)).switchTab(capture(), eq(mockTransactionOptions))

            // The history is [2, 3, 4, 5]
            for (i in 1..4) {
                assertEquals(allValues[i - 1], i + 1)
            }
        }
    }

    private fun mockNavSwitchController(uniqueTabHistoryController: UniqueTabHistoryController) {
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