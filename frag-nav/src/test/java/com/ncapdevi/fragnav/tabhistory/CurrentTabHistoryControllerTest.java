package com.ncapdevi.fragnav.tabhistory;

import com.ncapdevi.fragnav.FragNavPopController;
import com.ncapdevi.fragnav.FragNavTransactionOptions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CurrentTabHistoryControllerTest {
    @Mock
    private FragNavPopController mockFragNavPopController;

    @Test
    public void testPopDelegatedWhenPopCalled() {
        // Given
        CurrentTabHistoryController currentTabHistoryController = new CurrentTabHistoryController(
                mockFragNavPopController);

        // When
        currentTabHistoryController.popFragments(1, null);

        // Then
        verify(mockFragNavPopController, times(1)).tryPopFragments(eq(1), isNull(FragNavTransactionOptions.class));
    }
}