package com.ncapdevi.fragnav;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.view.View;

import com.ncapdevi.fragnav.tabhistory.CurrentTabHistoryController;
import com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController;
import com.ncapdevi.fragnav.tabhistory.UniqueTabHistoryController;
import com.ncapdevi.fragnav.tabhistory.UnlimitedTabHistoryController;

import org.json.JSONArray;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController.CURRENT_TAB;
import static com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController.UNIQUE_TAB_HISTORY;
import static com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController.UNLIMITED_TAB_HISTORY;

/**
 * The class is used to manage navigation through multiple stacks of fragments, as well as coordinate
 * fragments that may appear on screen
 * <p>
 * https://github.com/ncapdevi/FragNav
 * Nic Capdevila
 * Nic.Capdevila@gmail.com
 * <p>
 * Originally Created March 2016
 */
@SuppressWarnings("RestrictedApi")
public class FragNavController {
    // Declare the constants. A maximum of 5 tabs is recommended for bottom navigation, this is per Material Design's Bottom Navigation's design spec.
    public static final int NO_TAB = -1;
    public static final int TAB1 = 0;
    public static final int TAB2 = 1;
    public static final int TAB3 = 2;
    public static final int TAB4 = 3;
    public static final int TAB5 = 4;
    public static final int TAB6 = 5;
    public static final int TAB7 = 6;
    public static final int TAB8 = 7;
    public static final int TAB9 = 8;
    public static final int TAB10 = 9;
    public static final int TAB11 = 10;
    public static final int TAB12 = 11;
    public static final int TAB13 = 12;
    public static final int TAB14 = 13;
    public static final int TAB15 = 14;
    public static final int TAB16 = 15;
    public static final int TAB17 = 16;
    public static final int TAB18 = 17;
    public static final int TAB19 = 18;
    public static final int TAB20 = 19;

    private static final int MAX_NUM_TABS = 20;

    // Extras used to store savedInstanceState
    private static final String EXTRA_TAG_COUNT = FragNavController.class.getName() + ":EXTRA_TAG_COUNT";
    private static final String EXTRA_SELECTED_TAB_INDEX = FragNavController.class.getName() + ":EXTRA_SELECTED_TAB_INDEX";
    private static final String EXTRA_CURRENT_FRAGMENT = FragNavController.class.getName() + ":EXTRA_CURRENT_FRAGMENT";
    private static final String EXTRA_FRAGMENT_STACK = FragNavController.class.getName() + ":EXTRA_FRAGMENT_STACK";

    @IdRes
    private final int mContainerId;
    @NonNull
    private final List<Stack<Fragment>> mFragmentStacks;
    @NonNull
    private final FragmentManager mFragmentManager;
    private final FragNavTransactionOptions mDefaultTransactionOptions;
    @TabIndex
    private int mSelectedTabIndex;
    private int mTagCount;
    @Nullable
    private Fragment mCurrentFrag;
    @Nullable
    private DialogFragment mCurrentDialogFrag;
    @Nullable
    private RootFragmentListener mRootFragmentListener;
    @Nullable
    private TransactionListener mTransactionListener;
    private boolean mExecutingTransaction;
    private FragNavTabHistoryController mFragNavTabHistoryController;
    @FragNavTabHistoryController.PopStrategy
    private final int mPopStrategy;

    //region Construction and setup

    private FragNavController(Builder builder, @Nullable Bundle savedInstanceState) {
        mFragmentManager = builder.mFragmentManager;
        mContainerId = builder.mContainerId;
        mFragmentStacks = new ArrayList<>(builder.mNumberOfTabs);
        mRootFragmentListener = builder.mRootFragmentListener;
        mTransactionListener = builder.mTransactionListener;
        mDefaultTransactionOptions = builder.mDefaultTransactionOptions;
        mSelectedTabIndex = builder.mSelectedTabIndex;
        mPopStrategy = builder.mPopStrategy;

        DefaultFragNavPopController fragNavPopController = new DefaultFragNavPopController();
        switch (mPopStrategy) {
            case CURRENT_TAB:
                mFragNavTabHistoryController = new CurrentTabHistoryController(fragNavPopController);
                break;
            case UNIQUE_TAB_HISTORY:
                mFragNavTabHistoryController = new UniqueTabHistoryController(fragNavPopController,
                        builder.fragNavSwitchController);
                break;
            case UNLIMITED_TAB_HISTORY:
                mFragNavTabHistoryController = new UnlimitedTabHistoryController(fragNavPopController,
                        builder.fragNavSwitchController);
                break;
        }

        mFragNavTabHistoryController.switchTab(mSelectedTabIndex);

        //Attempt to restore from bundle, if not, initialize
        if (!restoreFromBundle(savedInstanceState, builder.mRootFragments)) {

            for (int i = 0; i < builder.mNumberOfTabs; i++) {
                Stack<Fragment> stack = new Stack<>();
                if (builder.mRootFragments != null) {
                    stack.add(builder.mRootFragments.get(i));
                }
                mFragmentStacks.add(stack);
            }

            initialize(builder.mSelectedTabIndex);
        } else {
            mFragNavTabHistoryController.restoreFromBundle(savedInstanceState);
        }
    }

    public static Builder newBuilder(@Nullable Bundle savedInstanceState, FragmentManager fragmentManager, int containerId) {
        return new Builder(savedInstanceState, fragmentManager, containerId);
    }

    /**
     * Helper function to make sure that we are starting with a clean slate and to perform our first fragment interaction.
     *
     * @param index the tab index to initialize to
     */
    public void initialize(@TabIndex int index) {
        mSelectedTabIndex = index;
        if (mSelectedTabIndex > mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks");
        }

        mSelectedTabIndex = index;
        clearFragmentManager();
        clearDialogFragment();

        if (index == NO_TAB) {
            return;
        }

        FragmentTransaction ft = createTransactionWithOptions(null, false);

        Fragment fragment = getRootFragment(index);
        ft.add(mContainerId, fragment, generateTag(fragment));

        commitTransaction(ft, null);

        mCurrentFrag = fragment;
        if (mTransactionListener != null) {
            mTransactionListener.onTabTransaction(getCurrentFrag(), mSelectedTabIndex);
        }
    }
    //endregion

    //region Transactions

    /**
     * Function used to switch to the specified fragment stack
     *
     * @param index              The given index to switch to
     * @param transactionOptions Transaction options to be displayed
     * @throws IndexOutOfBoundsException Thrown if trying to switch to an index outside given range
     */
    public void switchTab(@TabIndex int index, @Nullable FragNavTransactionOptions transactionOptions) throws IndexOutOfBoundsException {
        switchTabInternal(index, transactionOptions);
    }

    private void switchTabInternal(@TabIndex int index, @Nullable FragNavTransactionOptions transactionOptions) throws IndexOutOfBoundsException {
        //Check to make sure the tab is within range
        if (index >= mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Can't switch to a tab that hasn't been initialized, " +
                    "Index : " + index + ", current stack size : " + mFragmentStacks.size() +
                    ". Make sure to create all of the tabs you need in the Constructor or provide a way for them to be created via RootFragmentListener.");
        }
        if (mSelectedTabIndex != index) {
            mSelectedTabIndex = index;
            mFragNavTabHistoryController.switchTab(index);

            FragmentTransaction ft = createTransactionWithOptions(transactionOptions, false);

            detachCurrentFragment(ft);

            Fragment fragment = null;
            if (index == NO_TAB) {
                commitTransaction(ft, transactionOptions);
            } else {
                //Attempt to reattach previous fragment
                fragment = reattachPreviousFragment(ft);
                if (fragment != null) {
                    commitTransaction(ft, transactionOptions);
                } else {
                    fragment = getRootFragment(mSelectedTabIndex);
                    ft.add(mContainerId, fragment, generateTag(fragment));
                    commitTransaction(ft, transactionOptions);
                }
            }


            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onTabTransaction(getCurrentFrag(), mSelectedTabIndex);
            }
        }
    }

    /**
     * Function used to switch to the specified fragment stack
     *
     * @param index The given index to switch to
     * @throws IndexOutOfBoundsException Thrown if trying to switch to an index outside given range
     */
    public void switchTab(@TabIndex int index) throws IndexOutOfBoundsException {
        switchTab(index, null);
    }

    /**
     * Push a fragment onto the current stack
     *
     * @param fragment           The fragment that is to be pushed
     * @param transactionOptions Transaction options to be displayed
     */
    public void pushFragment(@Nullable Fragment fragment, @Nullable FragNavTransactionOptions transactionOptions) {
        if (fragment != null && mSelectedTabIndex != NO_TAB) {
            FragmentTransaction ft = createTransactionWithOptions(transactionOptions, false);

            detachCurrentFragment(ft);
            ft.add(mContainerId, fragment, generateTag(fragment));

            commitTransaction(ft, transactionOptions);

            mFragmentStacks.get(mSelectedTabIndex).push(fragment);

            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(getCurrentFrag(), TransactionType.PUSH);
            }
        }
    }

    /**
     * Push a fragment onto the current stack
     *
     * @param fragment The fragment that is to be pushed
     */
    public void pushFragment(@Nullable Fragment fragment) {
        pushFragment(fragment, null);
    }

    /**
     * Pop the current fragment from the current tab
     *
     * @param transactionOptions Transaction options to be displayed
     */
    public boolean popFragment(@Nullable FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException {
        return popFragments(1, transactionOptions);
    }

    /**
     * Pop the current fragment from the current tab
     */
    public boolean popFragment() throws UnsupportedOperationException {
        return popFragment(null);
    }

    /**
     * Pop the current stack until a given tag is found. If the tag is not found, the stack will popFragment until it is at
     * the root fragment
     *
     * @param transactionOptions Transaction options to be displayed
     * @return true if any any fragment has been popped
     */
    public boolean popFragments(int popDepth, @Nullable FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException {
        return mFragNavTabHistoryController.popFragments(popDepth, transactionOptions);
    }

    private int tryPopFragmentsFromCurrentStack(int popDepth, @Nullable FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException {
        if (mPopStrategy == CURRENT_TAB && isRootFragment()) {
            throw new UnsupportedOperationException(
                    "You can not popFragment the rootFragment. If you need to change this fragment, use replaceFragment(fragment)");
        } else if (popDepth < 1) {
            throw new UnsupportedOperationException("popFragments parameter needs to be greater than 0");
        } else if (mSelectedTabIndex == NO_TAB) {
            throw new UnsupportedOperationException("You can not pop fragments when no tab is selected");
        }

        //If our popDepth is big enough that it would just clear the stack, then call that.
        int poppableSize = mFragmentStacks.get(mSelectedTabIndex).size() - 1;
        if (popDepth >= poppableSize) {
            clearStack(transactionOptions);
            return poppableSize;
        }

        Fragment fragment;
        FragmentTransaction ft = createTransactionWithOptions(transactionOptions, true);

        //Pop the number of the fragments on the stack and remove them from the FragmentManager
        for (int i = 0; i < popDepth; i++) {
            fragment = mFragmentManager.findFragmentByTag(mFragmentStacks.get(mSelectedTabIndex).pop().getTag());
            if (fragment != null) {
                ft.remove(fragment);
            }
        }

        //Attempt to reattach previous fragment
        fragment = reattachPreviousFragment(ft);

        boolean bShouldPush = false;
        //If we can't reattach, either pull from the stack, or create a new root fragment
        if (fragment != null) {
            commitTransaction(ft, transactionOptions);
        } else {
            if (!mFragmentStacks.get(mSelectedTabIndex).isEmpty()) {
                fragment = mFragmentStacks.get(mSelectedTabIndex).peek();
                ft.add(mContainerId, fragment, fragment.getTag());
                commitTransaction(ft, transactionOptions);
            } else {
                fragment = getRootFragment(mSelectedTabIndex);
                ft.add(mContainerId, fragment, generateTag(fragment));
                commitTransaction(ft, transactionOptions);

                bShouldPush = true;
            }
        }

        //Need to have this down here so that that tag has been
        // committed to the fragment before we add to the stack
        if (bShouldPush) {
            mFragmentStacks.get(mSelectedTabIndex).push(fragment);
        }

        mCurrentFrag = fragment;
        if (mTransactionListener != null) {
            mTransactionListener.onFragmentTransaction(getCurrentFrag(), TransactionType.POP);
        }
        return popDepth;
    }

    /**
     * Pop the current fragment from the current tab
     */
    public void popFragments(int popDepth) throws UnsupportedOperationException {
        popFragments(popDepth, null);
    }

    /**
     * Clears the current tab's stack to get to just the bottom Fragment. This will reveal the root fragment
     *
     * @param transactionOptions Transaction options to be displayed
     */
    public void clearStack(@Nullable FragNavTransactionOptions transactionOptions) {
        if (mSelectedTabIndex == NO_TAB) {
            return;
        }

        //Grab Current stack
        Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);

        // Only need to start popping and reattach if the stack is greater than 1
        if (fragmentStack.size() > 1) {
            Fragment fragment;
            FragmentTransaction ft = createTransactionWithOptions(transactionOptions, true);

            //Pop all of the fragments on the stack and remove them from the FragmentManager
            while (fragmentStack.size() > 1) {
                fragment = mFragmentManager.findFragmentByTag(fragmentStack.pop().getTag());
                if (fragment != null) {
                    ft.remove(fragment);
                }
            }

            //Attempt to reattach previous fragment
            fragment = reattachPreviousFragment(ft);

            boolean bShouldPush = false;
            //If we can't reattach, either pull from the stack, or create a new root fragment
            if (fragment != null) {
                commitTransaction(ft, transactionOptions);
            } else {
                if (!fragmentStack.isEmpty()) {
                    fragment = fragmentStack.peek();
                    ft.add(mContainerId, fragment, fragment.getTag());
                    commitTransaction(ft, transactionOptions);
                } else {
                    fragment = getRootFragment(mSelectedTabIndex);
                    ft.add(mContainerId, fragment, generateTag(fragment));
                    commitTransaction(ft, transactionOptions);

                    bShouldPush = true;
                }
            }

            if (bShouldPush) {
                mFragmentStacks.get(mSelectedTabIndex).push(fragment);
            }

            //Update the stored version we have in the list
            mFragmentStacks.set(mSelectedTabIndex, fragmentStack);

            mCurrentFrag = fragment;
            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(getCurrentFrag(), TransactionType.POP);
            }
        }
    }

    /**
     * Clears the current tab's stack to get to just the bottom Fragment. This will reveal the root fragment.
     */
    public void clearStack() {
        clearStack(null);
    }

    /**
     * Replace the current fragment
     *
     * @param fragment           the fragment to be shown instead
     * @param transactionOptions Transaction options to be displayed
     */
    public void replaceFragment(@NonNull Fragment fragment, @Nullable FragNavTransactionOptions transactionOptions) {
        Fragment poppingFrag = getCurrentFrag();

        if (poppingFrag != null) {
            FragmentTransaction ft = createTransactionWithOptions(transactionOptions, false);

            //overly cautious fragment popFragment
            Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
            if (!fragmentStack.isEmpty()) {
                fragmentStack.pop();
            }

            String tag = generateTag(fragment);
            ft.replace(mContainerId, fragment, tag);

            //Commit our transactions
            commitTransaction(ft, transactionOptions);

            fragmentStack.push(fragment);
            mCurrentFrag = fragment;

            if (mTransactionListener != null) {
                mTransactionListener.onFragmentTransaction(getCurrentFrag(), TransactionType.REPLACE);
            }
        }
    }

    /**
     * Replace the current fragment
     *
     * @param fragment the fragment to be shown instead
     */
    public void replaceFragment(@NonNull Fragment fragment) {
        replaceFragment(fragment, null);
    }

    /**
     * @return Current DialogFragment being displayed. Null if none
     */
    @Nullable
    @CheckResult
    public DialogFragment getCurrentDialogFrag() {
        if (mCurrentDialogFrag != null) {
            return mCurrentDialogFrag;
        }
        //Else try to find one in the FragmentManager
        else {
            FragmentManager fragmentManager;
            Fragment currentFrag = getCurrentFrag();
            if (currentFrag != null) {
                fragmentManager = currentFrag.getChildFragmentManager();
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
            Fragment currentFrag = getCurrentFrag();
            if (currentFrag != null) {
                fragmentManager = currentFrag.getChildFragmentManager();
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
     * Display a DialogFragment on the screen
     *
     * @param dialogFragment The Fragment to be Displayed
     */
    public void showDialogFragment(@Nullable DialogFragment dialogFragment) {
        if (dialogFragment != null) {
            FragmentManager fragmentManager;
            Fragment currentFrag = getCurrentFrag();
            if (currentFrag != null) {
                fragmentManager = currentFrag.getChildFragmentManager();
            } else {
                fragmentManager = mFragmentManager;
            }

            //Clear any current dialog fragments
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
            } catch (IllegalStateException e) {
                // Activity was likely destroyed before we had a chance to show, nothing can be done here.
            }
        }
    }

    //endregion

    //region Private helper functions

    /**
     * Helper function to get the root fragment for a given index. This is done by either passing them in the constructor, or dynamically via NavListener.
     *
     * @param index The tab index to get this fragment from
     * @return The root fragment at this index
     * @throws IllegalStateException This will be thrown if we can't find a rootFragment for this index. Either because you didn't provide it in the
     *                               constructor, or because your RootFragmentListener.getRootFragment(index) isn't returning a fragment for this index.
     */
    @NonNull
    @CheckResult
    private Fragment getRootFragment(int index) throws IllegalStateException {
        Fragment fragment = null;
        if (!mFragmentStacks.get(index).isEmpty()) {
            fragment = mFragmentStacks.get(index).peek();
        } else if (mRootFragmentListener != null) {
            fragment = mRootFragmentListener.getRootFragment(index);

            if (mSelectedTabIndex != NO_TAB) {
                mFragmentStacks.get(mSelectedTabIndex).push(fragment);
            }
        }
        if (fragment == null) {
            throw new IllegalStateException("Either you haven't past in a fragment at this index in your constructor, or you haven't " +
                    "provided a way to create it while via your RootFragmentListener.getRootFragment(index)");
        }

        return fragment;
    }

    /**
     * Will attempt to reattach a previous fragment in the FragmentManager, or return null if not able to.
     *
     * @param ft current fragment transaction
     * @return Fragment if we were able to find and reattach it
     */
    @Nullable
    private Fragment reattachPreviousFragment(@NonNull FragmentTransaction ft) {
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
     * Attempts to detach any current fragment if it exists, and if none is found, returns.
     *
     * @param ft the current transaction being performed
     */
    private void detachCurrentFragment(@NonNull FragmentTransaction ft) {
        Fragment oldFrag = getCurrentFrag();
        if (oldFrag != null) {
            ft.detach(oldFrag);
        }
    }

    /**
     * Helper function to attempt to get current fragment
     *
     * @return Fragment the current frag to be returned
     */
    @Nullable
    @CheckResult
    public Fragment getCurrentFrag() {
        //Attempt to used stored current fragment
        if (mCurrentFrag != null && mCurrentFrag.isAdded() && !mCurrentFrag.isDetached()) {
            return mCurrentFrag;
        } else if (mSelectedTabIndex == NO_TAB) {
            return null;
        }
        //if not, try to pull it from the stack
        Stack<Fragment> fragmentStack = mFragmentStacks.get(mSelectedTabIndex);
        if (!fragmentStack.isEmpty()) {
            Fragment fragmentByTag = mFragmentManager.findFragmentByTag(mFragmentStacks.get(mSelectedTabIndex).peek().getTag());
            if (fragmentByTag != null) {
                mCurrentFrag = fragmentByTag;
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
    @NonNull
    @CheckResult
    private String generateTag(@NonNull Fragment fragment) {
        return fragment.getClass().getName() + ++mTagCount;
    }


    /**
     * Private helper function to clear out the fragment manager on initialization. All fragment management should be done via FragNav.
     */
    private void clearFragmentManager() {
        if (mFragmentManager.getFragments() != null) {
            FragmentTransaction ft = createTransactionWithOptions(null, false);
            for (Fragment fragment : mFragmentManager.getFragments()) {
                if (fragment != null) {
                    ft.remove(fragment);
                }
            }
            commitTransaction(ft, null);
        }
    }

    /**
     * Setup a fragment transaction with the given option
     *
     * @param transactionOptions The options that will be set for this transaction
     * @param isPopping
     */
    @CheckResult
    private FragmentTransaction createTransactionWithOptions(@Nullable FragNavTransactionOptions transactionOptions, boolean isPopping) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (transactionOptions == null) {
            transactionOptions = mDefaultTransactionOptions;
        }
        if (transactionOptions != null) {
            if (isPopping) {
                ft.setCustomAnimations(transactionOptions.popEnterAnimation, transactionOptions.popExitAnimation);
            } else {
                ft.setCustomAnimations(transactionOptions.enterAnimation, transactionOptions.exitAnimation);
            }
            ft.setTransitionStyle(transactionOptions.transitionStyle);

            ft.setTransition(transactionOptions.transition);

            if (transactionOptions.sharedElements != null) {
                for (Pair<View, String> sharedElement : transactionOptions.sharedElements) {
                    ft.addSharedElement(sharedElement.first, sharedElement.second);
                }
            }

            if (transactionOptions.breadCrumbTitle != null) {
                ft.setBreadCrumbTitle(transactionOptions.breadCrumbTitle);
            }

            if (transactionOptions.breadCrumbShortTitle != null) {
                ft.setBreadCrumbShortTitle(transactionOptions.breadCrumbShortTitle);
            }
        }
        return ft;
    }

    /**
     * Helper function to commit fragment transaction with transaction option - allowStateLoss
     *
     * @param fragmentTransaction
     * @param transactionOptions
     */
    private void commitTransaction(FragmentTransaction fragmentTransaction, @Nullable FragNavTransactionOptions transactionOptions) {
        if (transactionOptions != null && transactionOptions.allowStateLoss) {
            fragmentTransaction.commitAllowingStateLoss();
        } else {
            fragmentTransaction.commit();
        }
    }

    //endregion

    //region Public helper functions

    /**
     * Get the number of fragment stacks
     *
     * @return the number of fragment stacks
     */
    @CheckResult
    public int getSize() {
        return mFragmentStacks.size();
    }

    /**
     * Get a copy of the stack at a given index
     *
     * @return requested stack
     */
    @SuppressWarnings("unchecked")
    @CheckResult
    @Nullable
    public Stack<Fragment> getStack(@TabIndex int index) {
        if (index == NO_TAB) {
            return null;
        }
        if (index >= mFragmentStacks.size()) {
            throw new IndexOutOfBoundsException("Can't get an index that's larger than we've setup");
        }
        return (Stack<Fragment>) mFragmentStacks.get(index).clone();
    }

    /**
     * Get a copy of the current stack that is being displayed
     *
     * @return Current stack
     */
    @SuppressWarnings("unchecked")
    @CheckResult
    @Nullable
    public Stack<Fragment> getCurrentStack() {
        return getStack(mSelectedTabIndex);
    }

    /**
     * Get the index of the current stack that is being displayed
     *
     * @return Current stack index
     */
    @CheckResult
    @TabIndex
    public int getCurrentStackIndex() {
        return mSelectedTabIndex;
    }

    /**
     * @return If true, you are at the bottom of the stack
     * (Consider using replaceFragment if you need to change the root fragment for some reason)
     * else you can popFragment as needed as your are not at the root
     */
    @CheckResult
    public boolean isRootFragment() {
        Stack<Fragment> stack = getCurrentStack();

        return stack == null || stack.size() == 1;
    }

    /**
     * Helper function to get wether the fragmentManger has gone through a stateSave, if this is true, you probably want to commit  allowing stateloss
     *
     * @return if fragmentManger isStateSaved
     */
    public boolean isStateSaved() {
        return mFragmentManager.isStateSaved();
    }

    /**
     *  Use this if you need to make sure that pending transactions occur immediately. This call is safe to
     *  call as often as you want as there's a check to prevent multiple executePendingTransactions at once
     *
     */
    public void executePendingTransactions() {
        if (!mExecutingTransaction) {
            mExecutingTransaction = true;
            mFragmentManager.executePendingTransactions();
            mExecutingTransaction = false;
        }
    }


    //endregion

    //region SavedInstanceState

    /**
     * Call this in your Activity's onSaveInstanceState(Bundle outState) method to save the instance's state.
     *
     * @param outState The Bundle to save state information to
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {

        // Write tag count
        outState.putInt(EXTRA_TAG_COUNT, mTagCount);

        // Write select tab
        outState.putInt(EXTRA_SELECTED_TAB_INDEX, mSelectedTabIndex);

        // Write current fragment
        Fragment currentFrag = getCurrentFrag();
        if (currentFrag != null) {
            outState.putString(EXTRA_CURRENT_FRAGMENT, currentFrag.getTag());
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

        mFragNavTabHistoryController.onSaveInstanceState(outState);
    }

    /**
     * Restores this instance to the state specified by the contents of savedInstanceState
     *
     * @param savedInstanceState The bundle to restore from
     * @param rootFragments      List of root fragments from which to initialize empty stacks. If null, pull fragments from RootFragmentListener.
     * @return true if successful, false if not
     */
    private boolean restoreFromBundle(@Nullable Bundle savedInstanceState, @Nullable List<Fragment> rootFragments) {
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
            int selectedTabIndex = savedInstanceState.getInt(EXTRA_SELECTED_TAB_INDEX);
            if (selectedTabIndex >= 0 && selectedTabIndex < MAX_NUM_TABS) {
                switchTab(selectedTabIndex);
            }

            //Successfully restored state
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    //endregion

    public enum TransactionType {
        PUSH,
        POP,
        REPLACE
    }

    //Declare the TabIndex annotation
    @IntDef({NO_TAB, TAB1, TAB2, TAB3, TAB4, TAB5, TAB6, TAB7, TAB8, TAB9, TAB10, TAB11, TAB12,
            TAB13, TAB14, TAB15, TAB16, TAB17, TAB18, TAB19, TAB20})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TabIndex {
    }

    // Declare Transit Styles
    @IntDef({FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_FRAGMENT_OPEN, FragmentTransaction.TRANSIT_FRAGMENT_CLOSE, FragmentTransaction.TRANSIT_FRAGMENT_FADE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Transit {
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

    public interface TransactionListener {

        void onTabTransaction(@Nullable Fragment fragment, int index);

        void onFragmentTransaction(Fragment fragment, TransactionType transactionType);
    }

    public class DefaultFragNavPopController implements com.ncapdevi.fragnav.FragNavPopController {
        @Override
        public int tryPopFragments(int popDepth, FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException {
            return FragNavController.this.tryPopFragmentsFromCurrentStack(popDepth, transactionOptions);
        }
    }

    public static final class Builder {
        private final int mContainerId;
        private FragmentManager mFragmentManager;
        private RootFragmentListener mRootFragmentListener;
        @TabIndex
        private int mSelectedTabIndex = TAB1;
        private TransactionListener mTransactionListener;
        private FragNavTransactionOptions mDefaultTransactionOptions;
        private int mNumberOfTabs = 0;

        @FragNavTabHistoryController.PopStrategy
        private int mPopStrategy = CURRENT_TAB;
        private List<Fragment> mRootFragments;
        private Bundle mSavedInstanceState;
        
        @Nullable
        private FragNavSwitchController fragNavSwitchController;

        public Builder(@Nullable Bundle savedInstanceState, FragmentManager mFragmentManager, int mContainerId) {
            this.mSavedInstanceState = savedInstanceState;
            this.mFragmentManager = mFragmentManager;
            this.mContainerId = mContainerId;
        }

        /**
         * @param selectedTabIndex The initial tab index to be used must be in range of rootFragments size
         */
        public Builder selectedTabIndex(@TabIndex int selectedTabIndex) {
            mSelectedTabIndex = selectedTabIndex;
            if (mRootFragments != null && mSelectedTabIndex > mNumberOfTabs) {
                throw new IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks");
            }
            return this;
        }

        /**
         * @param rootFragment A single root fragment. This library can still be helpful when managing a single stack of fragments
         */
        public Builder rootFragment(Fragment rootFragment) {
            mRootFragments = new ArrayList<>(1);
            mRootFragments.add(rootFragment);
            mNumberOfTabs = 1;
            return rootFragments(mRootFragments);
        }

        /**
         * @param rootFragments a list of root fragments. root Fragments are the root fragments that exist on any tab structure. If only one fragment is sent in, fragnav will still manage
         *                      transactions
         */
        public Builder rootFragments(@NonNull List<Fragment> rootFragments) {
            mRootFragments = rootFragments;
            mNumberOfTabs = rootFragments.size();
            if (mNumberOfTabs > MAX_NUM_TABS) {
                throw new IllegalArgumentException("Number of root fragments cannot be greater than " + MAX_NUM_TABS);
            }
            return this;
        }

        /**
         * @param transactionOptions The default transaction options to be used unless otherwise defined.
         */
        public Builder defaultTransactionOptions(@NonNull FragNavTransactionOptions transactionOptions) {
            mDefaultTransactionOptions = transactionOptions;
            return this;
        }

        /**
         * @param rootFragmentListener a listener that allows for dynamically creating root fragments
         * @param numberOfTabs         the number of tabs that will be switched between
         */
        public Builder rootFragmentListener(RootFragmentListener rootFragmentListener, int numberOfTabs) {
            mRootFragmentListener = rootFragmentListener;
            mNumberOfTabs = numberOfTabs;
            if (mNumberOfTabs > MAX_NUM_TABS) {
                throw new IllegalArgumentException("Number of tabs cannot be greater than " + MAX_NUM_TABS);
            }
            return this;
        }

        /**
         * @param val A listener to be implemented (typically within the main activity) to fragment transactions (including tab switches)
         */
        public Builder transactionListener(TransactionListener val) {
            mTransactionListener = val;
            return this;
        }

        /**
         * @param popStrategy Switch between different approaches of handling tab history while popping fragments on current tab
         */
        public Builder popStrategy(@FragNavTabHistoryController.PopStrategy int popStrategy) {
            mPopStrategy = popStrategy;
            return this;
        }

        /**
         * @param fragNavSwitchController Handles switch requests
         */
        public Builder switchController(FragNavSwitchController fragNavSwitchController) {
            this.fragNavSwitchController = fragNavSwitchController;
            return this;
        }

        public FragNavController build() {
            if (mRootFragmentListener == null && mRootFragments == null) {
                throw new IndexOutOfBoundsException("Either a root fragment(s) needs to be set, or a fragment listener");
            }
            if ((mPopStrategy == UNIQUE_TAB_HISTORY || mPopStrategy == UNLIMITED_TAB_HISTORY) && fragNavSwitchController == null) {
                throw new IllegalStateException(
                        "Switch handler needs to be set for unique or unlimited tab history strategy");
            }
            return new FragNavController(this, mSavedInstanceState);
        }
    }
}
