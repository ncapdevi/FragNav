package com.ncapdevi.fragnav.tabhistory;

import android.os.Bundle;

import com.ncapdevi.fragnav.FragNavPopController;
import com.ncapdevi.fragnav.FragNavSwitchController;
import com.ncapdevi.fragnav.FragNavTransactionOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnlimitedTabHistoryControllerTest {
    @Mock
    private FragNavPopController mockFragNavPopController;

    @Mock
    private FragNavSwitchController mockFragNavSwitchController;

    @Mock
    private Bundle mockBundle;

    @Mock
    private FragNavTransactionOptions mockTransactionOptions;

    private UnlimitedTabHistoryController unlimitedTabHistoryController;

    @Before
    public void setUp() {
        unlimitedTabHistoryController = new UnlimitedTabHistoryController(mockFragNavPopController,
                mockFragNavSwitchController);
        mockNavSwitchController(unlimitedTabHistoryController);
        mockBundle();
    }

    @Test
    public void testNoSwitchWhenCurrentStackIsLargerThanPopCount() {
        // Given
        unlimitedTabHistoryController.switchTab(1);
        unlimitedTabHistoryController.switchTab(2);
        when(mockFragNavPopController.tryPopFragments(eq(1), eq(mockTransactionOptions))).thenReturn(1);

        // When
        boolean result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions);

        // Then
        assertTrue(result);
        verify(mockFragNavSwitchController, never()).switchTab(anyInt(), nullable(FragNavTransactionOptions.class));
    }

    @Test
    public void testPopDoesNothingWhenPopIsCalledWithNothingToPopWithNoHistory() {
        // Given
        unlimitedTabHistoryController.switchTab(1);

        // When
        boolean result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions);

        // Then
        assertFalse(result);
        verify(mockFragNavSwitchController, never()).switchTab(anyInt(), nullable(FragNavTransactionOptions.class));
    }

    @Test
    public void testPopSwitchesTabWhenPopIsCalledWithNothingToPopAndHasHistory() {
        // Given
        unlimitedTabHistoryController.switchTab(1);
        unlimitedTabHistoryController.switchTab(2);

        // When
        boolean result = unlimitedTabHistoryController.popFragments(1, mockTransactionOptions);

        // Then
        assertTrue(result);
        verify(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq(mockTransactionOptions));
    }

    @Test
    public void testSwitchWhenCurrentStackIsNotLargerThanPopCount() {
        // Given
        unlimitedTabHistoryController.switchTab(1);
        unlimitedTabHistoryController.switchTab(2);
        when(mockFragNavPopController.tryPopFragments(eq(2), eq(mockTransactionOptions))).thenReturn(1);

        // When
        boolean result = unlimitedTabHistoryController.popFragments(2, mockTransactionOptions);

        // Then
        assertTrue(result);
        verify(mockFragNavSwitchController, times(1)).switchTab(eq(1), eq(mockTransactionOptions));
    }

    @Test
    public void testTabsUnlimitedRollbackWhenPopAllAvailableItemsInOneStep() {
        // Given

        // Navigating ahead through tabs
        for (int i = 1; i < 6; i++) {
            unlimitedTabHistoryController.switchTab(i);
        }

        // Navigating backwards through tabs
        for (int i = 5; i > 0; i--) {
            unlimitedTabHistoryController.switchTab(i);
        }

        // Every tab contains 1 item
        for (int i = 7; i < 16; i += 2) {
            when(mockFragNavPopController.tryPopFragments(eq(i), eq(mockTransactionOptions))).thenReturn(1);
        }

        // When
        boolean result = unlimitedTabHistoryController.popFragments(15, mockTransactionOptions);

        // Then
        assertTrue(result);
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockFragNavSwitchController, times(9)).switchTab(argumentCaptor.capture(), eq(mockTransactionOptions));
        List<Integer> allValues = argumentCaptor.getAllValues();

        // The history is [2, 3, 4, 5, 5, 4, 3, 2, 1]
        for (int i = 1; i < 5; i++) {
            assertEquals((int) allValues.get(i - 1), i + 1);
        }
        for (int i = 4; i < 9; i++) {
            assertEquals((int) allValues.get(i), 9 - i);
        }
    }

    @Test
    public void testTabsUnlimitedRollbackWhenPopAllAvailableItemsInDifferentSteps() {
        // Given

        // Navigating ahead through tabs
        for (int i = 1; i < 6; i++) {
            unlimitedTabHistoryController.switchTab(i);
        }

        // Navigating backwards through tabs
        for (int i = 5; i > 0; i--) {
            unlimitedTabHistoryController.switchTab(i);
        }

        // When
        for (int i = 0; i < 9; i++) {
            assertTrue(unlimitedTabHistoryController.popFragments(1, mockTransactionOptions));
        }
        assertFalse(unlimitedTabHistoryController.popFragments(1, mockTransactionOptions));

        // Then
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockFragNavSwitchController, times(9)).switchTab(argumentCaptor.capture(), eq(mockTransactionOptions));
        List<Integer> allValues = argumentCaptor.getAllValues();

        // The history is [2, 3, 4, 5, 5, 4, 3, 2, 1]
        for (int i = 1; i < 5; i++) {
            assertEquals((int) allValues.get(i - 1), i + 1);
        }
        for (int i = 4; i < 9; i++) {
            assertEquals((int) allValues.get(i), 9 - i);
        }
    }

    @Test
    public void testHistoryIsSavedAndRestoredWhenSaveCalledNewInstanceCreatedRestoreCalled() {
        // Given
        for (int i = 5; i > 0; i--) {
            unlimitedTabHistoryController.switchTab(i);
        }

        // When
        unlimitedTabHistoryController.onSaveInstanceState(mockBundle);
        UnlimitedTabHistoryController newUniqueTabHistoryController = new UnlimitedTabHistoryController(
                mockFragNavPopController,
                mockFragNavSwitchController);
        mockNavSwitchController(newUniqueTabHistoryController);
        newUniqueTabHistoryController.restoreFromBundle(mockBundle);
        for (int i = 0; i < 4; i++) {
            assertTrue(newUniqueTabHistoryController.popFragments(1, mockTransactionOptions));
        }

        // Then
        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockFragNavSwitchController, times(4)).switchTab(argumentCaptor.capture(), eq(mockTransactionOptions));
        List<Integer> allValues = argumentCaptor.getAllValues();

        // The history is [2, 3, 4, 5]
        for (int i = 1; i < 5; i++) {
            assertEquals((int) allValues.get(i - 1), i + 1);
        }
    }

    private void mockNavSwitchController(final UnlimitedTabHistoryController uniqueTabHistoryController) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                uniqueTabHistoryController.switchTab((Integer) invocation.getArgument(0));
                return null;
            }
        }).when(mockFragNavSwitchController).switchTab(anyInt(), nullable(FragNavTransactionOptions.class));
    }

    @SuppressWarnings("unchecked")
    private void mockBundle() {
        final ArrayList<Integer> storage = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                storage.clear();
                storage.addAll(((ArrayList<Integer>) invocation.getArgument(1)));
                return null;
            }
        }).when(mockBundle).putIntegerArrayList(anyString(), any(ArrayList.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (storage.size() > 0) {
                    return storage;
                }
                return null;
            }
        }).when(mockBundle).getIntegerArrayList(anyString());
    }
}