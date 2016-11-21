package com.ncapdevi.fragnav;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.json.JSONArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * The class is used to manage navigation through multiple stacks of fragments, as well as coordinate
 * fragments that may appear on screen
 * Created by niccapdevila on 3/21/16.
 */
public class FragNavController {
    //Declare the constants
    public static final int TAB1 = 0;
    public static final int TAB2 = 1;
    public static final int TAB3 = 2;
    public static final int TAB4 = 3;
    public static final int TAB5 = 4;


    private static final String EXTRA_TAG_COUNT = FragNavController.class.getName() + ":EXTRA_TAG_COUNT";
    private static final String EXTRA_SELECTED_TAB_INDEX = FragNavController.class.getName() + ":EXTRA_SELECTED_TAB_INDEX";
    private static final String EXTRA_CURRENT_FRAGMENT = FragNavController.class.getName() + ":EXTRA_CURRENT_FRAGMENT";
    private static final String EXTRA_FRAGMENT_STACK = FragNavController.class.getName() + ":EXTRA_FRAGMENT_STACK";

    private final List<Stack<Fragment>> mFragmentStacks;
    private final FragmentManager mFragmentManager;
    @TabIndex
    private int mSelectedTabIndex = -1;
    private int mTagCount;
    private Fragment mCurrentFrag;
    private DialogFragment mCurrentDialogFrag;

    private NavListener mNavListener;
    @IdRes
    private int mContainerId;

    @Transit
    private int mTransitionMode = FragmentTransaction.TRANSIT_UNSET;
    private boolean mExecutingTransaction;

    //region Construction and setup
    public FragNavController(Bundle savedInstanceState, @NonNull FragmentManager fragmentManager, @IdRes int containerId, @NonNull List<Fragment> baseFragments, @TabIndex int startingIndex) {
        mFragmentManager = fragmentManager;
        mContainerId = containerId;
        mFragmentStacks = new ArrayList<>(baseFragments.size());

        //Initialize
        if (!restoreFromBundle(savedInstanceState,baseFragments)) {
            for (Fragment fragment : baseFragments) {
                Stack<Fragment> stack = new Stack<>();
                stack.add(fragment);
                mFragmentStacks.add(stack);
            }
            initialize(startingIndex);
        }
    }

    public FragNavController(Bundle savedInstanceState, @NonNull FragmentManager fragmentManager, @IdRes int containerId, NavListener navListener, int numberOfTabs, @TabIndex int startingIndex) {
        mFragmentManager = fragmentManager;
        mContainerId = containerId;
        mFragmentStacks = new ArrayList<>(numberOfTabs);

        setNavListener(navListener);
        if (!restoreFromBundle(savedInstanceState,null)) {
            for (int i = 0; i < numberOfTabs; i++) {
                mFragmentStacks.add(new Stack<Fragment>());
            }
            initialize(startingIndex);
        }
    }

    public void setNavListener(NavListener navListener) {
        mNavListener = navListener;
    }

    public void setTransitionMode(@Transit int mTransitionMode) {this.mTransitionMode = mTransitionMode; }
    //endregion

    //region Transactions

    /**
     * Switch to a different tab. Should not be called on the current tab.
     *
     * @param index the index of the tab to switch to
     */
    public void switchTab(@TabIndex int index) {
        //Check to make sure the tab is within range
        if (index >= mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Can't switch to a tab that hasn't been initialized, " +
                    "Index : " + index + ", current stack size : " + mFragmentStacks.size() +
                    ". Make sure to create all of the tabs you need in the Constructor");
        }
        if (mSelectedTabIndex != index) {
            mSelectedTabIndex = index;

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.setTransition(mTransitionMode);

            detachCurrentFragment(ft);

            //Attempt to reattach previous fragment
            Fragment fragment = reattachPreviousFragment(ft);
            if (fragment != null) {
                ft.commit();
            } else {
                fragment = getBaseFragment(mSelectedTabIndex);
                ft.add(mContainerId, fragment, generateTag(fragment));
                ft.commit();
                mFragmentStacks.get(mSelectedTabIndex).push(fragment);
            }

            executePendingTransactions();

            mCurrentFrag = fragment;
            if (mNavListener != null) {
                mNavListener.onTabTransaction(mCurrentFrag, mSelectedTabIndex);
            }
        }
    }

    /**
     * Push a fragment onto the current stack
     *
     * @param fragment The fragment that is to be pushed
     */
    public void push(Fragment fragment) {
        if (fragment != null) {

            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.setTransition(mTransitionMode);
            detachCurrentFragment(ft);
            ft.add(mContainerId, fragment, generateTag(fragment));
            ft.commit();

            executePendingTransactions();

            mFragmentStacks.get(mSelectedTabIndex).push(fragment);

            mCurrentFrag = fragment;
            if (mNavListener != null) {
                mNavListener.onFragmentTransaction(mCurrentFrag);
            }

        }
    }

    /**
     * Pop the current fragment from the current tab
     */
    public void pop() {
        Fragment poppingFrag = getCurrentFrag();
        if (poppingFrag != null) {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.setTransition(mTransitionMode);
            ft.remove(poppingFrag);

            //overly cautious fragment pop
            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                fragmentStack.pop();
            }

            //Attempt reattach, if we can't, try to pop from the stack and push that on
            Fragment fragment = reattachPreviousFragment(ft);
            if (fragment == null && !fragmentStack.isEmpty()) {
                fragment = fragmentStack.peek();
                ft.add(mContainerId, fragment, fragment.getTag());
            }

            //Commit our transactions
            ft.commit();

            executePendingTransactions();

            mCurrentFrag = fragment;
            if (mNavListener != null) {
                mNavListener.onFragmentTransaction(mCurrentFrag);
            }
        }
    }

    /**
     * Clears the current tab's stack to get to just the bottom Fragment.
     */
    public void clearStack() {
        //Grab Current stack
        Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);

        // Only need to start popping and reattach if the stack is greater than 1
        if (fragmentStack.size() > 1) {
            Fragment fragment;
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.setTransition(mTransitionMode);

            //Pop all of the fragments on the stack and remove them from the FragmentManager
            while (fragmentStack.size() > 1) {
                fragment = mFragmentManager.findFragmentByTag(fragmentStack.peek().getTag());
                if (fragment != null) {
                    fragmentStack.pop();
                    ft.remove(fragment);
                }
            }

            //Attempt to reattach previous fragment
            fragment = reattachPreviousFragment(ft);

            boolean bShouldPush = false;
            //If we can't reattach, either pull from the stack, or create a new base fragment
            if (fragment != null) {
                ft.commit();
            } else {
                if (!fragmentStack.isEmpty()) {
                    fragment = fragmentStack.peek();
                    ft.add(mContainerId, fragment, fragment.getTag());
                    ft.commit();
                } else {
                    fragment = getBaseFragment(mSelectedTabIndex);
                    ft.add(mContainerId, fragment, generateTag(fragment));
                    ft.commit();

                    bShouldPush = true;
                }
            }

            executePendingTransactions();

            if (bShouldPush) {
                mFragmentStacks.get(mSelectedTabIndex).push(fragment);
            }

            //Update the stored version we have in the list
            mFragmentStacks.set(mSelectedTabIndex, fragmentStack);

            mCurrentFrag = fragment;
            if (mNavListener != null) {
                mNavListener.onFragmentTransaction(mCurrentFrag);
            }
        }
    }

    /**
     * Replace the current freagment
     *
     * @param fragment
     */
    public void replace(Fragment fragment) {
        Fragment poppingFrag = getCurrentFrag();

        if (poppingFrag != null) {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.setTransition(mTransitionMode);

            //overly cautious fragment pop
            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                fragmentStack.pop();
            }

            String tag = generateTag(fragment);
            ft.replace(mContainerId, fragment, tag);

            //Commit our transactions
            ft.commit();

            executePendingTransactions();

            fragmentStack.push(fragment);
            mCurrentFrag = fragment;
            mNavListener.onFragmentTransaction(mCurrentFrag);
        }
    }
    //endregion

    //region Private helper functions

    private void initialize(@TabIndex int index){
        mSelectedTabIndex = index;
        clearFragmentManager();
        clearDialogFragment();

        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.setTransition(mTransitionMode);

        Fragment fragment = getBaseFragment(index);
        ft.add(mContainerId, fragment, generateTag(fragment));
        ft.commit();

        executePendingTransactions();

        mFragmentStacks.get(mSelectedTabIndex).push(fragment);

        mCurrentFrag = fragment;
        if (mNavListener != null) {
            mNavListener.onTabTransaction(mCurrentFrag, mSelectedTabIndex);
        }
    }

    private Fragment getBaseFragment(int index) throws IllegalStateException {
        Fragment fragment = null;
        if (!mFragmentStacks.get(index).isEmpty()) {
            fragment = mFragmentStacks.get(index).peek();
        } else if (mNavListener != null) {
            fragment = mNavListener.getBaseFragment(index);
        }
        if (fragment == null) {
            throw new IllegalStateException("You haven't provided a list of fragments or" +
                    " a way to create fragment for this index");
        }

        return fragment;
    }

    /**
     * Will attempt to reattach a previous fragment in the FragmentManager, or return null if not able to,
     *
     * @param ft current fragment transaction
     * @return Fragment if we were able to find and reattach it
     */
    @Nullable
    private Fragment reattachPreviousFragment(FragmentTransaction ft) {
        Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
        Fragment fragment = null;
        if (!fragmentStack.isEmpty()) {
            fragment = mFragmentManager.findFragmentByTag(fragmentStack.peek().getTag());
            if (fragment != null) {
                ft.attach(fragment);
            }
        }
        return fragment;
    }

    /**
     * Attemps to detach any current fragment if it exists, and if none is found, returns;
     *
     * @param ft the current transaction being performed
     */
    private void detachCurrentFragment(FragmentTransaction ft) {
        Fragment oldFrag = getCurrentFrag();
        if (oldFrag != null) {
            ft.detach(oldFrag);
        }
    }

    @Nullable
    private Fragment getCurrentFrag() {
        //Attempt to used stored current fragment
        if (mCurrentFrag != null) {
            return mCurrentFrag;
        }
        //if not, try to pull it from the stack
        else {
            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                return mFragmentManager.findFragmentByTag(mFragmentStacks.get(mSelectedTabIndex).peek().getTag());
            } else {
                return null;
            }
        }
    }

    /**
     * Create a unique fragment tag so that we can grab the fragment later from the FragmentManger
     *
     * @param fragment The fragment that we're creating a unique tag for
     * @return a unique tag using the fragment's class name
     */
    private String generateTag(Fragment fragment) {
        return fragment.getClass().getName() + ++mTagCount;
    }

    /**
     * This check is here to prevent recursive entries into executePendingTransactions
     */
    private void executePendingTransactions() {
        if (!mExecutingTransaction) {
            mExecutingTransaction = true;
            mFragmentManager.executePendingTransactions();
            mExecutingTransaction = false;
        }
    }


    private void clearFragmentManager() {
        if (mFragmentManager.getFragments() != null) {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            ft.setTransition(mTransitionMode);
            for (Fragment fragment : mFragmentManager.getFragments()) {
                ft.remove(fragment);
            }
            ft.commit();
            executePendingTransactions();
        }
    }
    //endregion

    //region Public helper functions

    public int getSize() {
        if (mFragmentStacks == null) {
            return 0;
        }
        return mFragmentStacks.size();
    }

    public Stack<Fragment> getCurrentStack() {
        return mFragmentStacks.get(mSelectedTabIndex);
    }

    public boolean canPop() {
        return getCurrentStack().size() > 1;
    }

    @Nullable
    public DialogFragment getCurrentDialogFrag() {
        if (mCurrentDialogFrag != null) {
            return mCurrentDialogFrag;
        }
        //Else try to find one in the fragmentmanager
        else {
            FragmentManager fragmentManager;
            if (mCurrentFrag != null) {
                fragmentManager = mCurrentFrag.getChildFragmentManager();
            } else {
                fragmentManager = mFragmentManager;
            }
            if (fragmentManager.getFragments() != null) {
                for (Fragment fragment : fragmentManager.getFragments()) {
                    if (fragment instanceof DialogFragment) {
                        mCurrentDialogFrag = (DialogFragment) fragment;
                        break;
                    }
                }
            }
        }
        return mCurrentDialogFrag;
    }

    public void clearDialogFragment() {
        if (mCurrentDialogFrag != null) {
            mCurrentDialogFrag.dismiss();
            mCurrentDialogFrag = null;
        }
        // If we don't have the current dialog, try to find and dismiss it
        else {
            FragmentManager fragmentManager;
            if (mCurrentFrag != null) {
                fragmentManager = mCurrentFrag.getChildFragmentManager();
            } else {
                fragmentManager = mFragmentManager;
            }

            if (fragmentManager.getFragments() != null) {
                for (Fragment fragment : fragmentManager.getFragments()) {
                    if (fragment instanceof DialogFragment) {
                        ((DialogFragment) fragment).dismiss();
                    }
                }
            }
        }
    }

    public void showDialogFragment(DialogFragment dialogFragment) {
        if (dialogFragment != null) {
            FragmentManager fragmentManager;
            if (mCurrentFrag != null) {
                fragmentManager = mCurrentFrag.getChildFragmentManager();
            } else {
                fragmentManager = mFragmentManager;
            }

            //Clear any current dialogfragments
            if (fragmentManager.getFragments() != null) {
                for (Fragment fragment : fragmentManager.getFragments()) {
                    if (fragment instanceof DialogFragment) {
                        ((DialogFragment) fragment).dismiss();
                        mCurrentDialogFrag = null;
                    }
                }
            }

            mCurrentDialogFrag = dialogFragment;
            dialogFragment.show(fragmentManager, dialogFragment.getClass().getName());
        }
    }

    //endregion
    
    //region SavedInstanceState
    /**
     * Call this in your Activity's onSaveInstanceState(Bundle outState) method to save the instance's state.
     *
     * @param outState The Bundle to save state information to
     */
    public void onSaveInstanceState(Bundle outState) {

        // Write tag count
        outState.putInt(EXTRA_TAG_COUNT, mTagCount);

        // Write select tab
        outState.putInt(EXTRA_SELECTED_TAB_INDEX, mSelectedTabIndex);

        // Write current fragment
        if (mCurrentFrag != null) {
            outState.putString(EXTRA_CURRENT_FRAGMENT, mCurrentFrag.getTag());
        }

        // Write stacks
        try {
            final JSONArray stackArrays = new JSONArray();

            for (Stack<Fragment> stack : mFragmentStacks) {
                final JSONArray stackArray = new JSONArray();

                for (Fragment fragment : stack) {
                    stackArray.put(fragment.getTag());
                }

                stackArrays.put(stackArray);
            }

            outState.putString(EXTRA_FRAGMENT_STACK, stackArrays.toString());
        } catch (Throwable t) {
            // Nothing we can do
        }
    }

    /**
     * Restores this instance to the state specified by the contents of savedInstanceState
     *
     * @param savedInstanceState The bundle to restore from
     * @param baseFragments      List of base fragments from which to initialize empty stacks
     * @return true if successful, false if not
     */
    private boolean restoreFromBundle(Bundle savedInstanceState, @Nullable List<Fragment> baseFragments) {
        if (savedInstanceState == null) {
            return false;
        }

        // Restore tag count
        mTagCount = savedInstanceState.getInt(EXTRA_TAG_COUNT, 0);

        // Restore current fragment
        mCurrentFrag = mFragmentManager.findFragmentByTag(savedInstanceState.getString(EXTRA_CURRENT_FRAGMENT));

        // Restore fragment stacks
        try {
            final JSONArray stackArrays = new JSONArray(savedInstanceState.getString(EXTRA_FRAGMENT_STACK));

            for (int x = 0; x < stackArrays.length(); x++) {
                final JSONArray stackArray = stackArrays.getJSONArray(x);
                final Stack<Fragment> stack = new Stack<>();

                if (stackArray.length() == 1) {
                    final String tag = stackArray.getString(0);
                    final Fragment fragment;

                    if (tag == null || "null".equalsIgnoreCase(tag)) {
                        if (baseFragments != null) {
                            fragment = baseFragments.get(x);
                        }else{
                            fragment = getBaseFragment(x);
                        }

                    } else {
                        fragment = mFragmentManager.findFragmentByTag(tag);
                    }

                    if (fragment != null) {
                        stack.add(fragment);
                    }
                } else {
                    for (int y = 0; y < stackArray.length(); y++) {
                        final String tag = stackArray.getString(y);

                        if (tag != null && !"null".equalsIgnoreCase(tag)) {
                            final Fragment fragment = mFragmentManager.findFragmentByTag(tag);

                            if (fragment != null) {
                                stack.add(fragment);
                            }
                        }
                    }
                }

                mFragmentStacks.add(stack);
            }
            // Restore selected tab if we have one
            switch (savedInstanceState.getInt(EXTRA_SELECTED_TAB_INDEX)) {
                case TAB1:
                    switchTab(TAB1);
                    break;
                case TAB2:
                    switchTab(TAB2);
                    break;
                case TAB3:
                    switchTab(TAB3);
                    break;
                case TAB4:
                    switchTab(TAB4);
                    break;
                case TAB5:
                    switchTab(TAB5);
                    break;
            }

            //Succesfully restored state
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    //endregion

    //Declare the TabIndex annotation
    @IntDef({TAB1, TAB2, TAB3, TAB4, TAB5})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TabIndex {
    }

    // Declare Transit Styles
    @IntDef({FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_FRAGMENT_OPEN, FragmentTransaction.TRANSIT_FRAGMENT_CLOSE, FragmentTransaction.TRANSIT_FRAGMENT_FADE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Transit {
    }

    public interface NavListener {
        void onTabTransaction(Fragment fragment, int index);

        void onFragmentTransaction(Fragment fragment);

        /**
         * Dynamically create the Fragment that will go on the bottom of the stack
         *
         * @param index the index that the base of the stack Fragment needs to go
         * @return the new Fragment
         */
        Fragment getBaseFragment(int index);
    }
}
