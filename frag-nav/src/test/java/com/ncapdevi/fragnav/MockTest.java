package com.ncapdevi.fragnav;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings("ResourceType")
@RunWith(MockitoJUnitRunner.class)
public class MockTest {


    @Mock
    Context mMockContext;

    @Mock
    FragmentManager mFragmentManager;


    @Mock
    Bundle mBundle;

    @Mock
    FragmentTransaction mFragmentTransaction;

    private List<Fragment> mFragmentList = new ArrayList<>(5);
    private FragNavController mFragNavController;

    @Before
    public void initMocks() {
        mockFragmentManager();
        mockFragmentTransaction();
        mFragNavController = new FragNavController(mBundle, mFragmentManager, 1, mock(Fragment.class));
    }

    private void mockFragmentTransaction() {
       when(mFragmentTransaction.add(anyInt(), any(Fragment.class), anyString())).then(new Answer() {
           @Override
           public Object answer(InvocationOnMock invocation) throws Throwable {
               Object[] args = invocation.getArguments();
               mFragmentList.add((Fragment) args[1]);
               return mFragmentTransaction;
           }
       });
    }

    @SuppressLint("CommitTransaction")
    private void mockFragmentManager() {
        when(mFragmentManager.getFragments())
                .thenReturn(mFragmentList);

        when(mFragmentManager.beginTransaction())
                .thenReturn(mFragmentTransaction);

    }

    @Test
    public void pushPopClear() {
        assertNotNull(mFragNavController.getCurrentStack());
        int size = mFragNavController.getCurrentStack().size();

        mFragNavController.push(mock(Fragment.class));
        assertTrue(mFragNavController.getCurrentStack().size() == ++size);

        mFragNavController.push(mock(Fragment.class));
        assertTrue(mFragNavController.getCurrentStack().size()==++size);

        mFragNavController.push(mock(Fragment.class));
        assertTrue(mFragNavController.getCurrentStack().size()==++size);

        mFragNavController.pop();
        assertTrue(mFragNavController.getCurrentStack().size()==--size);

        mFragNavController.clearStack();
        assertTrue(mFragNavController.getCurrentStack().size()==1);
        assertTrue(mFragNavController.isRootFragment());
    }



}