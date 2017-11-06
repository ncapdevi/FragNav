package com.ncapdevi.fragnav;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings("ResourceType")
@RunWith(MockitoJUnitRunner.class)
public class MockTest implements FragNavController.TransactionListener {

    @Mock
    private FragmentManager mFragmentManager;

    @Mock
    private Bundle mBundle;

    @Mock
    private FragmentTransaction mFragmentTransaction;

    private List<Fragment> mFragmentList = new ArrayList<>(5);
    private FragNavController mFragNavController;

    @Before
    public void initMocks() {
        mockFragmentManager();
        mockFragmentTransaction();
        mFragNavController = FragNavController.newBuilder(mBundle, mFragmentManager, 1)
                .transactionListener(this)
                .rootFragment(mock(Fragment.class))
                .build();

        assertEquals(FragNavController.TAB1, mFragNavController.getCurrentStackIndex());
        assertNotNull(mFragNavController.getCurrentStack());
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
        when(mFragmentManager.getFragments()).thenReturn(mFragmentList);

        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
    }

    @Test
    public void testConstructionWhenMultipleFragments() {
        List<Fragment> rootFragments = new ArrayList<>();
        rootFragments.add(new Fragment());
        rootFragments.add(new Fragment());

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .build();

        assertEquals(FragNavController.TAB1, mFragNavController.getCurrentStackIndex());
        assertNotNull(mFragNavController.getCurrentStack());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionWhenTooManyRootFragments() {
        List<Fragment> rootFragments = new ArrayList<>();

        for (int i = 0; i < 21; i++) {
            rootFragments.add(new Fragment());
        }

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .build();
    }

    @Test
    public void testConstructionWhenMultipleFragmentsAndNoTabSelected() {
        List<Fragment> rootFragments = new ArrayList<>();
        rootFragments.add(new Fragment());
        rootFragments.add(new Fragment());

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .selectedTabIndex(FragNavController.NO_TAB)
                .build();

        assertEquals(FragNavController.NO_TAB, mFragNavController.getCurrentStackIndex());
        assertNull(mFragNavController.getCurrentStack());
    }

    @Test
    public void testConstructionWhenRootFragmentListenerAndTabSelected() {
        FragNavController.RootFragmentListener rootFragmentListener = mock(FragNavController.RootFragmentListener.class);
        doReturn(new Fragment()).when(rootFragmentListener).getRootFragment(anyInt());

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragmentListener(rootFragmentListener, 5)
                .selectedTabIndex(FragNavController.TAB3)
                .build();

        assertEquals(FragNavController.TAB3, mFragNavController.getCurrentStackIndex());
        assertNotNull(mFragNavController.getCurrentStack());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionWhenRootFragmentListenerAndTooManyTabs() {
        FragNavController.RootFragmentListener rootFragmentListener = mock(FragNavController.RootFragmentListener.class);

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragmentListener(rootFragmentListener, 21)
                .selectedTabIndex(FragNavController.TAB3)
                .build();
    }

    @Test
    @Ignore // Install Robolectric in order to test restoring from Bundle.
    @SuppressWarnings("ConstantConditions")
    public void testConstructionWhenRestoringFromBundle() {
        List<Fragment> rootFragments = new ArrayList<>();
        rootFragments.add(new Fragment());
        rootFragments.add(new Fragment());

        mFragNavController = FragNavController.newBuilder(null, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .selectedTabIndex(FragNavController.TAB1)
                .build();

        mFragNavController.switchTab(FragNavController.TAB2);
        mFragNavController.pushFragment(new Fragment());
        mFragNavController.pushFragment(new Fragment());
        mFragNavController.pushFragment(new Fragment());

        Fragment currentFragment = mFragNavController.getCurrentFrag();

        Bundle bundle = new Bundle();

        mFragNavController.onSaveInstanceState(bundle);

        mFragNavController = FragNavController.newBuilder(bundle, mFragmentManager, 1)
                .rootFragments(rootFragments)
                .selectedTabIndex(FragNavController.TAB1)
                .build();

        assertEquals(FragNavController.TAB2, mFragNavController.getCurrentStackIndex());
        assertEquals(4, mFragNavController.getCurrentStack().size());
        assertEquals(currentFragment, mFragNavController.getCurrentFrag());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void pushPopClear() {

        int size = mFragNavController.getCurrentStack().size();

        mFragNavController.pushFragment(mock(Fragment.class));
        assertTrue(mFragNavController.getCurrentStack().size() == ++size);

        mFragNavController.pushFragment(mock(Fragment.class));
        assertTrue(mFragNavController.getCurrentStack().size() == ++size);

        mFragNavController.pushFragment(mock(Fragment.class));
        assertTrue(mFragNavController.getCurrentStack().size() == ++size);

        mFragNavController.popFragment();
        assertTrue(mFragNavController.getCurrentStack().size() == --size);

        mFragNavController.clearStack();
        assertTrue(mFragNavController.getCurrentStack().size() == 1);
        assertTrue(mFragNavController.isRootFragment());
    }

    @Override
    public void onTabTransaction(Fragment fragment, int index) {
        assertNotNull(fragment);
    }

    @Override
    public void onFragmentTransaction(Fragment fragment, FragNavController.TransactionType transactionType) {
        assertNotNull(mFragNavController);

    }
}