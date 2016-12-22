package com.ncapdevi.fragnav;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@SuppressWarnings("ResourceType")
@RunWith(MockitoJUnitRunner.class)
public class MockTest {


    @Mock
    Context mMockContext;

    @Mock
    FragmentManager mFragmentManager;

    @Mock
    Fragment mFragment;

    @Mock
    Bundle mBundle;

    @Mock
    FragmentTransaction mFragmentTransaction;

    @Mock
    Fragment mFragment2;

    private List<Fragment> mFragmentList = new ArrayList<>(5);
    private FragNavController mFragNavController;

    @Before
    public void initMocks() {
        mockFragmentManager();
        mockFragmentTransaction();
        mFragNavController = new FragNavController(mBundle, mFragmentManager, 1, mFragment);
    }

    private void mockFragmentTransaction() {
        when(mFragmentTransaction.add(any(Fragment.class),anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                mFragmentList.add((Fragment) AdditionalAnswers.returnsFirstArg());
                return this;
            }
        }).thenReturn(mFragmentTransaction);
    }

    private void mockFragmentManager() {
        when(mFragmentManager.getFragments())
                .thenReturn(mFragmentList);

        when(mFragmentManager.beginTransaction())
                .thenReturn(mFragmentTransaction);

    }

    @Test
    public void clearedStackIsRoot() {
        mFragNavController.push(mFragment);
        mFragNavController.push(mFragment);
        mFragNavController.push(mFragment);

        mFragNavController.clearStack();
        assertTrue(mFragNavController.isRootFragment());
    }

    @Test
    public void pushPopFragment() {
        int size = mFragNavController.getSize();

        mFragNavController.push(mFragment2);
        assertTrue(mFragNavController.getCurrentStack().size()== ++size);

        mFragNavController.pop();
        assertTrue(mFragNavController.getCurrentStack().size()== --size);
    }
}