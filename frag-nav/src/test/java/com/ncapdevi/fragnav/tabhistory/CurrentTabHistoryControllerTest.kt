package com.ncapdevi.fragnav.tabhistory

import com.ncapdevi.fragnav.FragNavPopController
import com.ncapdevi.fragnav.FragNavTransactionOptions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CurrentTabHistoryControllerTest {
    @Mock
    private val mockFragNavPopController: FragNavPopController? = null

    @Test
    fun testPopDelegatedWhenPopCalled() {
        // Given
        val currentTabHistoryController = CurrentTabHistoryController(
                mockFragNavPopController!!)

        // When
        currentTabHistoryController.popFragments(1, null)

        // Then
        verify(mockFragNavPopController, times(1)).tryPopFragments(eq(1), isNull(FragNavTransactionOptions::class.java))
    }
}