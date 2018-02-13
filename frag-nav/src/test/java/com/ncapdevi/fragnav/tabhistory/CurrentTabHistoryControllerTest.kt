package com.ncapdevi.fragnav.tabhistory

import com.ncapdevi.fragnav.FragNavPopController
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CurrentTabHistoryControllerTest {
    @Mock
    private lateinit var mockFragNavPopController: FragNavPopController

    @Test
    fun testPopDelegatedWhenPopCalled() {
        // Given
        val currentTabHistoryController = CurrentTabHistoryController(mockFragNavPopController)

        // When
        currentTabHistoryController.popFragments(1, null)

        // Then
        verify(mockFragNavPopController, times(1)).tryPopFragments(eq(1), isNull())
    }
}