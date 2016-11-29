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
 *
 * https://github.com/ncapdevi/FragNav
 * Nic Capdevila
 * Nic.Capdevila@gmail.com
 *
 * Originally Created March 2016
 */
public class FragNavController {
    //Declare the constants  There is a maximum of 5 tabs, this is per Material Design's Bottom Navigation's design spec.
    public static final int TAB1 = 0;
    public static final int TAB2 = 1;
    public static final int TAB3 = 2;
    public static final int TAB4 = 3;
    public static final int TAB5 = 4;

    // Extras used to store savedInstanceState
    private static final String EXTRA_TAG_COUNT = FragNavController.class.getName() + ":EXTRA_TAG_COUNT";
    private static final String EXTRA_SELECTED_TAB_INDEX = FragNavController.class.getName() + ":EXTRA_SELECTED_TAB_INDEX";
    private static final String EXTRA_CURRENT_FRAGMENT = FragNavController.class.getName() + ":EXTRA_CURRENT_FRAGMENT";
    private static final String EXTRA_FRAGMENT_STACK = FragNavController.class.getName() + ":EXTRA_FRAGMENT_STACK";

    @IdRes
    private final int mContainerId;
    private final List<Stack<Fragment>> mFragmentStacks;
    private final FragmentManager mFragmentManager;

    @TabIndex
    private int mSelectedTabIndex = -1;
    private int mTagCount;
    private Fragment mCurrentFrag;
    private DialogFragment mCurrentDialogFrag;


    private RootFragmentListener mRootFragmentListener;
    private TransactionListener mTransactionListener;

    @Transit
    private int mTransitionMode = FragmentTransaction.TRANSIT_UNSET;

    private boolean mExecutingTransaction;

    //region Construction and setup

    /**
     * @param fragmentManager FragmentManager to be used
     * @param containerId     The resource ID of the layout in which the fragments will be placed
     * @param numberOfTabs    The number of different fragment stacks to be managed (maximum of five)
     */
    private FragNavController(@NonNull FragmentManager fragmentManager, @IdRes int containerId, int numberOfTabs) {
        mFragmentManager = fragmentManager;
        mContainerId = containerId;
        mFragmentStacks = new ArrayList<>(numberOfTabs);
    }

    /**
     * @param savedInstanceState savedInstanceState to allow for recreation of FragNavController and its fragments if possible
     * @param fragmentManager    FragmentManager to be used
     * @param containerId        The resource ID of the layout in which the fragments will be placed
     * @param rootFragment       A single root fragment. This library can still be helpful when mangiging a single stack of fragments.
     */

    public FragNavController(Bundle savedInstanceState, @NonNull FragmentManager fragmentManager, @IdRes int containerId, @NonNull Fragment rootFragment) {
        this(fragmentManager, containerId, 1);

        //Attempt to restore from bundle, if not, initialize
        List<Fragment> rootFragments = new ArrayList<>(1);
        rootFragments.add(rootFragment);

        if (!restoreFromBundle(savedInstanceState, rootFragments)) {
            Stack<Fragment> stack = new Stack<>();
            stack.add(rootFragment);
            mFragmentStacks.add(stack);
            initialize(TAB1);
        }
    }

    /**
     * @param savedInstanceState savedInstanceState to allow for recreation of FragNavController and its fragments if possible
     * @param fragmentManager    FragmentManager to be used
     * @param containerId        The resource ID of the layout in which the fragments will be placed
     * @param rootFragments      a list of root fragments. root Fragments are the root fragments that exist on any tab structure. If only one fragment is sent in,
     *                           fragnav will still manage transactions
     * @param startingIndex      The initial tab index to be used must be in range of rootFragments size
     */
    public FragNavController(Bundle savedInstanceState, @NonNull FragmentManager fragmentManager, @IdRes int containerId, @NonNull List<Fragment> rootFragments, @TabIndex int startingIndex) {
        this(fragmentManager, containerId, rootFragments.size());
        if (startingIndex > rootFragments.size()) {
            throw new IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks");
        }
        //Attempt to restore from bundle, if not, initialize
        if (!restoreFromBundle(savedInstanceState, rootFragments)) {
            for (Fragment fragment : rootFragments) {
                Stack<Fragment> stack = new Stack<>();
                stack.add(fragment);
                mFragmentStacks.add(stack);
            }
            initialize(startingIndex);
        }
    }

    /**
     * @param savedInstanceState savedInstanceState to allow for recreation of FragNavController and its fragments if possible
     * @param fragmentManager    FragmentManager to be used
     * @param containerId        The resource ID of the layout in which the fragments will be placed
     * @param rootFragmentListener        A listener to be implemented (typically within the main activity) to perform certain interactions.
     * @param numberOfTabs       The number of different fragment stacks to be managed (maximum of five)
     * @param startingIndex      The initial tab index to be used must be in range of rootFragments size
     */
    public FragNavController(Bundle savedInstanceState, @NonNull FragmentManager fragmentManager, @IdRes int containerId, RootFragmentListener rootFragmentListener, int numberOfTabs, @TabIndex int startingIndex) {
        this(fragmentManager, containerId, numberOfTabs);

        if (startingIndex > numberOfTabs) {
            throw new IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks");
        }

        setRootFragmentListener(rootFragmentListener);

        //Attempt to restore from bundle, if not, initialize
        if (!restoreFromBundle(savedInstanceState, null)) {
            for (int i = 0; i < numberOfTabs; i++) {
                mFragmentStacks.add(new Stack<Fragment>());
            }
            initialize(startingIndex);
        }
    }

    /**
     *
     * @param rootFragmentListener a listener that allows for dynamically creating root fragments
     */
    public void setRootFragmentListener(RootFragmentListener rootFragmentListener) {
        mRootFragmentListener = rootFragmentListener;
    }

    /**
     *
     * @param transactionListener        A listener to be implemented (typically within the main activity) to fragment transactions (including tab switches);
     */
    public void setTransactionListener(TransactionListener transactionListener) {
        mTransactionListener = transactionListener;
    }

    /**
     *
     * @param transitionMode The type of transition to be used during fragment transactions
     */
    public void setTransitionMode(@Transit int transitionMode) {
        mTransitionMode = transitionMode;
    }
    //endregion

    //region Transactions

     /**
     * Switch to a different tab. Should not be called on the current tab.
     *
     * @param index the index of the tab to switch to
     * @throws IndexOutOfBoundsException If the index to switch to is out of range
      */

    public void switchTab(@TabIndex int index) throws IndexOutOfBoundsException {
        //Check to make sure the tab is within range
        if (index >= mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Can't switch to a tab that hasn't been initialized, " +
                    "Index : " + index + ", current stack size : " + mFragmentStacks.size() +
                    ". Make sure to create all of the tabs you need in the Constructor or provide a way for them to be created via RootFragmentListener.");
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
                fragment = getRootFragment(mSelectedTabIndex);
                ft.add(mContainerId, fragment, generateTag(fragment));
                ft.commit();
            }

            executePendingTransactions();

            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onTabTransaction(mCurrentFrag, mSelectedTabIndex);
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
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag);
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
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag);
            }
        }
    }

    /**
     * Clears the current tab's stack to get to just the bottom Fragment. This will reveal the root fragment,
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
            //If we can't reattach, either pull from the stack, or create a new root fragment
            if (fragment != null) {
                ft.commit();
            } else {
                if (!fragmentStack.isEmpty()) {
                    fragment = fragmentStack.peek();
                    ft.add(mContainerId, fragment, fragment.getTag());
                    ft.commit();
                } else {
                    fragment = getRootFragment(mSelectedTabIndex);
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
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag);
            }
        }
    }

    /**
     * Replace the current fragment
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

            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(mCurrentFrag);

            }
        }
    }
    //endregion

    //region Private helper functions

    /**
     * Helper function to make sure that we are starting with a clean slate and to perform our first fragment interaction.
     * @param index the tab index to initialize to
     */
    private void initialize(@TabIndex int index) {
        mSelectedTabIndex = index;
        clearFragmentManager();
        clearDialogFragment();

        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.setTransition(mTransitionMode);

        Fragment fragment = getRootFragment(index);
        ft.add(mContainerId, fragment, generateTag(fragment));
        ft.commit();

        executePendingTransactions();

        mCurrentFrag = fragment;
        if (mTransactionListener != null) {
            mTransactionListener.onTabTransaction(mCurrentFrag, mSelectedTabIndex);
        }
    }

    /**
     * Helper function to get the root fragment for a given index. This is done by either passing them in the constructor, or dynamically via NavListner
     * @param index The tab index to get this fragment from
     * @return The root fragment at this index
     * @throws IllegalStateException This will be thrown if we can't find a rootFragment for this index. Either because you didn't provide it in the
     *                              constructor, or because your RootFragmentListener.getRootFragment(index) isn't returning a fragment for this index.

     */
    private Fragment getRootFragment(int index) throws IllegalStateException {
        Fragment fragment = null;
        if (!mFragmentStacks.get(index).isEmpty()) {
            fragment = mFragmentStacks.get(index).peek();
        } else if (mRootFragmentListener != null) {
            fragment = mRootFragmentListener.getRootFragment(index);
            mFragmentStacks.get(mSelectedTabIndex).push(fragment);

        }
        if (fragment == null) {
            throw new IllegalStateException("Either you haven't past in a fragment at this index in your constructor, or you haven't" +
                    "provided a way to create it while via your RootFragmentListener.getRootFragment(index)");
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

    /**
     * Helper function to attempt to get current fragment
     *
     * @return
     */
    @Nullable
    public Fragment getCurrentFrag() {
        //Attempt to used stored current fragment
        if (mCurrentFrag != null) {
            return mCurrentFrag;
        }
        //if not, try to pull it from the stack
        else {
            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                mCurrentFrag = mFragmentManager.findFragmentByTag(mFragmentStacks.get(mSelectedTabIndex).peek().getTag());
            }
        }
        return mCurrentFrag;
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

    /**
     * Private helper function to clear out the fragment manager on initialization. All fragment management should be done via FragNav
     */
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

    /**
     * Get the number of fragment stacks
     * @return the number of fragment stacks
     */
    public int getSize() {
        if (mFragmentStacks == null) {
            return 0;
        }
        return mFragmentStacks.size();
    }

    /**
     * Get the current stack that is being displayed
     * @return Current stack
     */
    public Stack<Fragment> getCurrentStack() {
        return mFragmentStacks.get(mSelectedTabIndex);
    }

    /**
     *
     * @return If you are able to pop the current stack. If false, you are at the bottom of the stack
     * (Consider using replace if you need to change the root fragment for some reason)
     */
    public boolean canPop() {
        return getCurrentStack().size() > 1;
    }

    /**
     *
     * @return Current DialogFragment being displayed. Null if none
     */
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

    /**
     * Clear any DialogFragments that may be shown
     */
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

    /**
     *  Display a DialogFragment on the screen
     * @param dialogFragment The Fragment to be Displayed
     */
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
            try {
                dialogFragment.show(fragmentManager, dialogFragment.getClass().getName());
            } catch(IllegalStateException e){
                // Activity was likely destroyed before we had a chance to show, nothing can be done here.
            }
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
     * @param rootFragments      List of root fragments from which to initialize empty stacks. If null, pull fragments from RootFragmentListener
     * @return true if successful, false if not
     */
    private boolean restoreFromBundle(Bundle savedInstanceState, @Nullable List<Fragment> rootFragments) {
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
                        if (rootFragments != null) {
                            fragment = rootFragments.get(x);
                        } else {
                            fragment = getRootFragment(x);
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

    public interface RootFragmentListener {
        /**
         * Dynamically create the Fragment that will go on the bottom of the stack
         *
         * @param index the index that the root of the stack Fragment needs to go
         * @return the new Fragment
         */
        Fragment getRootFragment(int index);
    }

    public interface TransactionListener{

        void onTabTransaction(Fragment fragment, int index);

        void onFragmentTransaction(Fragment fragment);
    }
}
