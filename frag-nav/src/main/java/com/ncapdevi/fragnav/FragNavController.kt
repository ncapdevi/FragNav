package com.ncapdevi.fragnav

import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.annotation.IdRes
import android.support.annotation.IntDef
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import com.ncapdevi.fragnav.tabhistory.CurrentTabHistoryController
import com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController
import com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController.Companion.CURRENT_TAB
import com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController.Companion.UNIQUE_TAB_HISTORY
import com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController.Companion.UNLIMITED_TAB_HISTORY
import com.ncapdevi.fragnav.tabhistory.UniqueTabHistoryController
import com.ncapdevi.fragnav.tabhistory.UnlimitedTabHistoryController
import org.json.JSONArray
import java.util.*

/**
 * The class is used to manage navigation through multiple stacks of fragments, as well as coordinate
 * fragments that may appear on screen
 *
 *
 * https://github.com/ncapdevi/FragNav
 * Nic Capdevila
 * Nic.Capdevila@gmail.com
 *
 *
 * Originally Created March 2016
 */
class FragNavController private constructor(builder: Builder, savedInstanceState: Bundle?) {

    @IdRes
    private val containerId: Int = builder.containerId
    private val fragmentStacks: MutableList<Stack<Fragment>> = ArrayList(builder.numberOfTabs)
    private val fragmentManger: FragmentManager = builder.fragmentManager
    private val defaultTransactionOptions: FragNavTransactionOptions? = builder.defaultTransactionOptions
    @FragNavTabHistoryController.PopStrategy
    private val popStrategy: Int = builder.popStrategy
    private val fragNavLogger: FragNavLogger? = builder.fragNavLogger
    private val rootFragmentListener: RootFragmentListener? = builder.rootFragmentListener
    private val transactionListener: TransactionListener? = builder.transactionListener

    private val fragmentHideStrategy = builder.fragmentHideStrategy
    private val createEager = builder.createEager

    @TabIndex
    @get:CheckResult
    @get:TabIndex
    var currentStackIndex: Int = builder.selectedTabIndex
        private set
    private var tagCount: Int = 0
    private var mCurrentFrag: Fragment? = null
    private var mCurrentDialogFrag: DialogFragment? = null

    private var executingTransaction: Boolean = false
    private var fragNavTabHistoryController: FragNavTabHistoryController


    init {

        fragNavTabHistoryController = when (popStrategy) {
            CURRENT_TAB -> CurrentTabHistoryController(DefaultFragNavPopController())
            UNIQUE_TAB_HISTORY -> UniqueTabHistoryController(DefaultFragNavPopController(), builder.fragNavSwitchController!!)
            UNLIMITED_TAB_HISTORY -> UnlimitedTabHistoryController(DefaultFragNavPopController(), builder.fragNavSwitchController!!)
            else -> CurrentTabHistoryController(DefaultFragNavPopController())
        }
        fragNavTabHistoryController.switchTab(currentStackIndex)

        //Attempt to restore from bundle, if not, initialize
        if (!restoreFromBundle(savedInstanceState, builder.rootFragments)) {
            for (i in 0 until builder.numberOfTabs) {
                val stack = Stack<Fragment>()
                if (builder.rootFragments != null) {
                    stack.add(builder.rootFragments!![i])
                }
                fragmentStacks.add(stack)
            }

            initialize(builder.selectedTabIndex)
        } else {
            fragNavTabHistoryController.restoreFromBundle(savedInstanceState)
        }
    }


    //region Public helper functions

    /**
     * Helper function to attempt to get current fragment
     *
     * @return Fragment the current frag to be returned
     */
    val currentFrag: Fragment?
        get() {
            //Attempt to used stored current fragment
            if (mCurrentFrag?.isAdded == true && mCurrentFrag?.isDetached?.not() == true) {
                return mCurrentFrag
            } else if (currentStackIndex == NO_TAB) {
                return null
            }
            //if not, try to pull it from the stack

            val fragmentStack = fragmentStacks[currentStackIndex]
            if (!fragmentStack.isEmpty()) {
                val fragmentByTag = fragmentManger.findFragmentByTag(fragmentStacks[currentStackIndex].peek().tag)
                if (fragmentByTag != null) {
                    mCurrentFrag = fragmentByTag
                }
            }
            return mCurrentFrag
        }

    /**
     * @return Current DialogFragment being displayed. Null if none
     */
    val currentDialogFrag: DialogFragment?
        @CheckResult
        get() {
            if (mCurrentDialogFrag != null) {
                return mCurrentDialogFrag
            } else {
                //Else try to find one in the FragmentManager
                val fragmentManager: FragmentManager = this.currentFrag?.childFragmentManager ?: this.fragmentManger
                mCurrentDialogFrag = fragmentManager.fragments?.firstOrNull { it is DialogFragment } as DialogFragment?
            }
            return mCurrentDialogFrag
        }


    /**
     * Get the number of fragment stacks
     *
     * @return the number of fragment stacks
     */
    val size: Int
        @CheckResult
        get() = fragmentStacks.size

    /**
     * Get a copy of the current stack that is being displayed
     *
     * @return Current stack
     */
    val currentStack: Stack<Fragment>?
        @CheckResult
        get() = getStack(currentStackIndex)

    /**
     * @return If true, you are at the bottom of the stack
     * (Consider using replaceFragment if you need to change the root fragment for some reason)
     * else you can popFragment as needed as your are not at the root
     */
    val isRootFragment: Boolean
        @CheckResult
        get() {
            val stack = currentStack
            return stack == null || stack.size == 1
        }

    /**
     * Helper function to get wether the fragmentManger has gone through a stateSave, if this is true, you probably want to commit  allowing stateloss
     *
     * @return if fragmentManger isStateSaved
     */
    val isStateSaved: Boolean
        get() = fragmentManger.isStateSaved


    /**
     * Helper function to make sure that we are starting with a clean slate and to perform our first fragment interaction.
     *
     * @param index the tab index to initialize to
     */
    fun initialize(@TabIndex index: Int) {
        currentStackIndex = index
        if (currentStackIndex > fragmentStacks.size) {
            throw IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks")
        }

        currentStackIndex = index
        clearFragmentManager()
        clearDialogFragment()

        if (index == NO_TAB) {
            return
        }

        val ft = createTransactionWithOptions(null, false)

        val lowerBound = if (createEager) 0 else index
        val upperBound = if (createEager) fragmentStacks.size else index + 1
        for (i in lowerBound until upperBound) {
            currentStackIndex = i
            val fragment = getRootFragment(i)
            ft.add(containerId, fragment, generateTag(fragment))
            if (i != index) {
                if (shouldDetachAttachOnSwitch()) {
                    ft.detach(fragment)
                } else {
                    ft.hide(fragment)
                }
            } else {
                mCurrentFrag = fragment
            }
        }
        currentStackIndex = index

        commitTransaction(ft, null)

        transactionListener?.onTabTransaction(currentFrag, currentStackIndex)
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
    @Throws(IndexOutOfBoundsException::class)
    @JvmOverloads
    fun switchTab(@TabIndex index: Int, transactionOptions: FragNavTransactionOptions? = null) {
        switchTabInternal(index, transactionOptions)
    }

    @Throws(IndexOutOfBoundsException::class)
    private fun switchTabInternal(@TabIndex index: Int, transactionOptions: FragNavTransactionOptions?) {
        //Check to make sure the tab is within range
        if (index >= fragmentStacks.size) {
            throw IndexOutOfBoundsException("Can't switch to a tab that hasn't been initialized, " +
                    "Index : " + index + ", current stack size : " + fragmentStacks.size +
                    ". Make sure to create all of the tabs you need in the Constructor or provide a way for them to be created via RootFragmentListener.")
        }
        if (currentStackIndex != index) {
            currentStackIndex = index
            fragNavTabHistoryController.switchTab(index)

            val ft = createTransactionWithOptions(transactionOptions, false)

            removeCurrentFragment(ft, shouldDetachAttachOnSwitch())

            var fragment: Fragment? = null
            if (index == NO_TAB) {
                commitTransaction(ft, transactionOptions)
            } else {
                //Attempt to reattach previous fragment
                fragment = addPreviousFragment(ft, shouldDetachAttachOnSwitch())
                if (fragment != null) {
                    commitTransaction(ft, transactionOptions)
                } else {
                    fragment = getRootFragment(currentStackIndex)
                    ft.add(containerId, fragment, generateTag(fragment))
                    commitTransaction(ft, transactionOptions)
                }
            }


            mCurrentFrag = fragment
            transactionListener?.onTabTransaction(currentFrag, currentStackIndex)
        }
    }

    /**
     * Push a fragment onto the current stack
     *
     * @param fragment           The fragment that is to be pushed
     * @param transactionOptions Transaction options to be displayed
     */
    @JvmOverloads
    fun pushFragment(fragment: Fragment?, transactionOptions: FragNavTransactionOptions? = null) {
        if (fragment != null && currentStackIndex != NO_TAB) {
            val ft = createTransactionWithOptions(transactionOptions, false)

            removeCurrentFragment(ft, shouldDetachAttachOnPushPop())
            ft.add(containerId, fragment, generateTag(fragment))

            commitTransaction(ft, transactionOptions)

            fragmentStacks[currentStackIndex].push(fragment)

            mCurrentFrag = fragment
            transactionListener?.onFragmentTransaction(currentFrag, TransactionType.PUSH)
        }
    }

    /**
     * Pop the current fragment from the current tab
     *
     * @param transactionOptions Transaction options to be displayed
     */
    @Throws(UnsupportedOperationException::class)
    @JvmOverloads
    fun popFragment(transactionOptions: FragNavTransactionOptions? = null): Boolean {
        return popFragments(1, transactionOptions)
    }

    /**
     * Pop the current stack until a given tag is found. If the tag is not found, the stack will popFragment until it is at
     * the root fragment
     *
     * @param transactionOptions Transaction options to be displayed
     * @return true if any any fragment has been popped
     */
    @Throws(UnsupportedOperationException::class)
    fun popFragments(popDepth: Int, transactionOptions: FragNavTransactionOptions?): Boolean {
        return fragNavTabHistoryController.popFragments(popDepth, transactionOptions)
    }

    @Throws(UnsupportedOperationException::class)
    private fun tryPopFragmentsFromCurrentStack(popDepth: Int, transactionOptions: FragNavTransactionOptions?): Int {
        if (popStrategy == CURRENT_TAB && isRootFragment) {
            throw UnsupportedOperationException(
                    "You can not popFragment the rootFragment. If you need to change this fragment, use replaceFragment(fragment)")
        } else if (popDepth < 1) {
            throw UnsupportedOperationException("popFragments parameter needs to be greater than 0")
        } else if (currentStackIndex == NO_TAB) {
            throw UnsupportedOperationException("You can not pop fragments when no tab is selected")
        }

        //If our popDepth is big enough that it would just clear the stack, then call that.
        val poppableSize = fragmentStacks[currentStackIndex].size - 1
        if (popDepth >= poppableSize) {
            clearStack(transactionOptions)
            return poppableSize
        }

        var fragment: Fragment?
        val ft = createTransactionWithOptions(transactionOptions, true)

        //Pop the number of the fragments on the stack and remove them from the FragmentManager
        for (i in 0 until popDepth) {
            fragment = fragmentManger.findFragmentByTag(fragmentStacks[currentStackIndex].pop().tag)
            if (fragment != null) {
                ft.remove(fragment)
            }
        }

        //Attempt to reattach previous fragment
        fragment = addPreviousFragment(ft, shouldDetachAttachOnPushPop())

        var bShouldPush = false
        //If we can't reattach, either pull from the stack, or create a new root fragment
        if (fragment != null) {
            commitTransaction(ft, transactionOptions)
        } else {
            if (!fragmentStacks[currentStackIndex].isEmpty()) {
                fragment = fragmentStacks[currentStackIndex].peek()
                ft.add(containerId, fragment, fragment!!.tag)
                commitTransaction(ft, transactionOptions)
            } else {
                fragment = getRootFragment(currentStackIndex)
                ft.add(containerId, fragment, generateTag(fragment))
                commitTransaction(ft, transactionOptions)

                bShouldPush = true
            }
        }

        //Need to have this down here so that that tag has been
        // committed to the fragment before we add to the stack
        if (bShouldPush) {
            fragmentStacks[currentStackIndex].push(fragment)
        }

        mCurrentFrag = fragment
        transactionListener?.onFragmentTransaction(currentFrag, TransactionType.POP)
        return popDepth
    }

    /**
     * Pop the current fragment from the current tab
     */
    @Throws(UnsupportedOperationException::class)
    fun popFragments(popDepth: Int) {
        popFragments(popDepth, null)
    }

    /**
     * Clears the current tab's stack to get to just the bottom Fragment. This will reveal the root fragment
     *
     * @param transactionOptions Transaction options to be displayed
     */
    @JvmOverloads
    fun clearStack(transactionOptions: FragNavTransactionOptions? = null) {
        if (currentStackIndex == NO_TAB) {
            return
        }

        //Grab Current stack
        val fragmentStack = fragmentStacks[currentStackIndex]

        // Only need to start popping and reattach if the stack is greater than 1
        if (fragmentStack.size > 1) {
            var fragment: Fragment?
            val ft = createTransactionWithOptions(transactionOptions, true)

            //Pop all of the fragments on the stack and remove them from the FragmentManager
            while (fragmentStack.size > 1) {
                fragment = fragmentManger.findFragmentByTag(fragmentStack.pop().tag)
                if (fragment != null) {
                    ft.remove(fragment)
                }
            }

            //Attempt to reattach previous fragment
            fragment = addPreviousFragment(ft, shouldDetachAttachOnPushPop())

            var bShouldPush = false
            //If we can't reattach, either pull from the stack, or create a new root fragment
            if (fragment != null) {
                commitTransaction(ft, transactionOptions)
            } else {
                if (!fragmentStack.isEmpty()) {
                    fragment = fragmentStack.peek()
                    ft.add(containerId, fragment, fragment!!.tag)
                    commitTransaction(ft, transactionOptions)
                } else {
                    fragment = getRootFragment(currentStackIndex)
                    ft.add(containerId, fragment, generateTag(fragment))
                    commitTransaction(ft, transactionOptions)

                    bShouldPush = true
                }
            }

            if (bShouldPush) {
                fragmentStacks[currentStackIndex].push(fragment)
            }

            //Update the stored version we have in the list
            fragmentStacks[currentStackIndex] = fragmentStack

            mCurrentFrag = fragment
            transactionListener?.onFragmentTransaction(currentFrag, TransactionType.POP)
        }
    }

    /**
     * Replace the current fragment
     *
     * @param fragment           the fragment to be shown instead
     * @param transactionOptions Transaction options to be displayed
     */
    @JvmOverloads
    fun replaceFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions? = null) {
        val poppingFrag = currentFrag

        if (poppingFrag != null) {
            val ft = createTransactionWithOptions(transactionOptions, false)

            //overly cautious fragment popFragment
            val fragmentStack = fragmentStacks[currentStackIndex]
            if (!fragmentStack.isEmpty()) {
                fragmentStack.pop()
            }

            val tag = generateTag(fragment)
            ft.replace(containerId, fragment, tag)

            //Commit our transactions
            commitTransaction(ft, transactionOptions)

            fragmentStack.push(fragment)
            mCurrentFrag = fragment

            transactionListener?.onFragmentTransaction(currentFrag, TransactionType.REPLACE)
        }
    }

    /**
     * Clear any DialogFragments that may be shown
     */
    fun clearDialogFragment() {
        if (mCurrentDialogFrag != null) {
            mCurrentDialogFrag!!.dismiss()
            mCurrentDialogFrag = null
        } else {
            val fragmentManager: FragmentManager
            val currentFrag = currentFrag
            if (currentFrag != null) {
                fragmentManager = currentFrag.childFragmentManager
            } else {
                fragmentManager = this.fragmentManger
            }

            if (fragmentManager.fragments != null) {
                for (fragment in fragmentManager.fragments) {
                    (fragment as? DialogFragment)?.dismiss()
                }
            }
        }// If we don't have the current dialog, try to find and dismiss it
    }

    /**
     * Display a DialogFragment on the screen
     *
     * @param dialogFragment The Fragment to be Displayed
     */
    fun showDialogFragment(dialogFragment: DialogFragment?) {
        if (dialogFragment != null) {
            val fragmentManager: FragmentManager
            val currentFrag = currentFrag
            if (currentFrag != null) {
                fragmentManager = currentFrag.childFragmentManager
            } else {
                fragmentManager = this.fragmentManger
            }

            //Clear any current dialog fragments
            if (fragmentManager.fragments != null) {
                for (fragment in fragmentManager.fragments) {
                    if (fragment is DialogFragment) {
                        fragment.dismiss()
                        mCurrentDialogFrag = null
                    }
                }
            }

            mCurrentDialogFrag = dialogFragment
            try {
                dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
            } catch (e: IllegalStateException) {
                logError("Could not show dialog", e)
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
     * constructor, or because your RootFragmentListener.getRootFragment(index) isn't returning a fragment for this index.
     */
    @CheckResult
    @Throws(IllegalStateException::class)
    private fun getRootFragment(index: Int): Fragment {
        var fragment: Fragment? = null
        if (!fragmentStacks[index].isEmpty()) {
            fragment = fragmentStacks[index].peek()
        } else if (rootFragmentListener != null) {
            fragment = rootFragmentListener.getRootFragment(index)

            if (currentStackIndex != NO_TAB) {
                fragmentStacks[currentStackIndex].push(fragment)
            }
        }
        if (fragment == null) {
            throw IllegalStateException("Either you haven't past in a fragment at this index in your constructor, or you haven't " + "provided a way to create it while via your RootFragmentListener.getRootFragment(index)")
        }

        return fragment
    }

    /**
     * Will attempt to reattach a previous fragment in the FragmentManager, or return null if not able to.
     *
     * @param ft current fragment transaction
     * @return Fragment if we were able to find and reattach it
     */
    private fun addPreviousFragment(ft: FragmentTransaction, isAttach: Boolean): Fragment? {
        val fragmentStack = fragmentStacks[currentStackIndex]
        var fragment: Fragment? = null
        if (!fragmentStack.isEmpty()) {
            fragment = fragmentManger.findFragmentByTag(fragmentStack.peek().tag)
            if (fragment != null) {
                if (isAttach) {
                    ft.attach(fragment)
                } else {
                    ft.show(fragment)
                }
            }
        }
        return fragment
    }

    /**
     * Attempts to detach any current fragment if it exists, and if none is found, returns.
     *
     * @param ft the current transaction being performed
     */
    private fun removeCurrentFragment(ft: FragmentTransaction, isDetach: Boolean) {
        val oldFrag = currentFrag
        if (oldFrag != null) {
            if (isDetach) {
                ft.detach(oldFrag)
            } else {
                ft.hide(oldFrag)
            }

        }
    }

    /**
     * Create a unique fragment tag so that we can grab the fragment later from the FragmentManger
     *
     * @param fragment The fragment that we're creating a unique tag for
     * @return a unique tag using the fragment's class name
     */
    @CheckResult
    private fun generateTag(fragment: Fragment): String {
        return fragment.javaClass.name + ++tagCount
    }


    /**
     * Private helper function to clear out the fragment manager on initialization. All fragment management should be done via FragNav.
     */
    private fun clearFragmentManager() {
        if (fragmentManger.fragments != null) {
            val ft = createTransactionWithOptions(null, false)
            for (fragment in fragmentManger.fragments) {
                if (fragment != null) {
                    ft.remove(fragment)
                }
            }
            commitTransaction(ft, null)
        }
    }

    /**
     * Setup a fragment transaction with the given option
     *
     * @param transactionOptions The options that will be set for this transaction
     * @param isPopping
     */
    @CheckResult
    private fun createTransactionWithOptions(transactionOptions: FragNavTransactionOptions?, isPopping: Boolean): FragmentTransaction {
        var transactionOptions = transactionOptions
        val ft = fragmentManger.beginTransaction()
        if (transactionOptions == null) {
            transactionOptions = defaultTransactionOptions
        }
        if (transactionOptions != null) {
            if (isPopping) {
                ft.setCustomAnimations(transactionOptions.popEnterAnimation, transactionOptions.popExitAnimation)
            } else {
                ft.setCustomAnimations(transactionOptions.enterAnimation, transactionOptions.exitAnimation)
            }
            ft.setTransitionStyle(transactionOptions.transitionStyle)

            ft.setTransition(transactionOptions.transition)

            if (transactionOptions.sharedElements != null) {
                for (sharedElement in transactionOptions.sharedElements) {
                    ft.addSharedElement(sharedElement.first, sharedElement.second)
                }
            }

            if (transactionOptions.breadCrumbTitle != null) {
                ft.setBreadCrumbTitle(transactionOptions.breadCrumbTitle)
            }

            if (transactionOptions.breadCrumbShortTitle != null) {
                ft.setBreadCrumbShortTitle(transactionOptions.breadCrumbShortTitle)
            }
        }
        return ft
    }

    /**
     * Helper function to commit fragment transaction with transaction option - allowStateLoss
     *
     * @param fragmentTransaction
     * @param transactionOptions
     */
    private fun commitTransaction(fragmentTransaction: FragmentTransaction, transactionOptions: FragNavTransactionOptions?) {
        if (transactionOptions != null && transactionOptions.allowStateLoss) {
            fragmentTransaction.commitAllowingStateLoss()
        } else {
            fragmentTransaction.commit()
        }
    }

    private fun logError(message: String, throwable: Throwable) {
        fragNavLogger?.error(message, throwable)
    }

    private fun shouldDetachAttachOnPushPop(): Boolean {
        return fragmentHideStrategy != HIDE
    }

    private fun shouldDetachAttachOnSwitch(): Boolean {
        return fragmentHideStrategy == DETACH
    }

    /**
     * Get a copy of the stack at a given index
     *
     * @return requested stack
     */
    @CheckResult
    fun getStack(@TabIndex index: Int): Stack<Fragment>? {
        if (index == NO_TAB) {
            return null
        }
        if (index >= fragmentStacks.size) {
            throw IndexOutOfBoundsException("Can't get an index that's larger than we've setup")
        }
        return fragmentStacks[index].clone() as Stack<Fragment>
    }

    /**
     * Use this if you need to make sure that pending transactions occur immediately. This call is safe to
     * call as often as you want as there's a check to prevent multiple executePendingTransactions at once
     */
    fun executePendingTransactions() {
        if (!executingTransaction) {
            executingTransaction = true
            fragmentManger.executePendingTransactions()
            executingTransaction = false
        }
    }

    //endregion

    //region SavedInstanceState

    /**
     * Call this in your Activity's onSaveInstanceState(Bundle outState) method to save the instance's state.
     *
     * @param outState The Bundle to save state information to
     */
    fun onSaveInstanceState(outState: Bundle) {

        // Write tag count
        outState.putInt(EXTRA_TAG_COUNT, tagCount)

        // Write select tab
        outState.putInt(EXTRA_SELECTED_TAB_INDEX, currentStackIndex)

        // Write current fragment
        val currentFrag = currentFrag
        if (currentFrag != null) {
            outState.putString(EXTRA_CURRENT_FRAGMENT, currentFrag.tag)
        }

        // Write stacks
        try {
            val stackArrays = JSONArray()

            for (stack in fragmentStacks) {
                val stackArray = JSONArray()

                for (fragment in stack) {
                    stackArray.put(fragment.tag)
                }

                stackArrays.put(stackArray)
            }

            outState.putString(EXTRA_FRAGMENT_STACK, stackArrays.toString())
        } catch (t: Throwable) {
            logError("Could not save fragment stack", t)
            // Nothing we can do
        }

        fragNavTabHistoryController.onSaveInstanceState(outState)
    }

    /**
     * Restores this instance to the state specified by the contents of savedInstanceState
     *
     * @param savedInstanceState The bundle to restore from
     * @param rootFragments      List of root fragments from which to initialize empty stacks. If null, pull fragments from RootFragmentListener.
     * @return true if successful, false if not
     */
    private fun restoreFromBundle(savedInstanceState: Bundle?, rootFragments: List<Fragment>?): Boolean {
        if (savedInstanceState == null) {
            return false
        }

        // Restore tag count
        tagCount = savedInstanceState.getInt(EXTRA_TAG_COUNT, 0)

        // Restore current fragment
        mCurrentFrag = fragmentManger.findFragmentByTag(savedInstanceState.getString(EXTRA_CURRENT_FRAGMENT))

        // Restore fragment stacks
        try {
            val stackArrays = JSONArray(savedInstanceState.getString(EXTRA_FRAGMENT_STACK))

            for (x in 0 until stackArrays.length()) {
                val stackArray = stackArrays.getJSONArray(x)
                val stack = Stack<Fragment>()

                if (stackArray.length() == 1) {
                    val tag = stackArray.getString(0)
                    val fragment: Fragment?

                    if (tag == null || "null".equals(tag, ignoreCase = true)) {
                        if (rootFragments != null) {
                            fragment = rootFragments[x]
                        } else {
                            fragment = getRootFragment(x)
                        }
                    } else {
                        fragment = fragmentManger.findFragmentByTag(tag)
                    }

                    if (fragment != null) {
                        stack.add(fragment)
                    }
                } else {
                    for (y in 0 until stackArray.length()) {
                        val tag = stackArray.getString(y)

                        if (tag != null && !"null".equals(tag, ignoreCase = true)) {
                            val fragment = fragmentManger.findFragmentByTag(tag)

                            if (fragment != null) {
                                stack.add(fragment)
                            }
                        }
                    }
                }

                fragmentStacks.add(stack)
            }
            // Restore selected tab if we have one
            val selectedTabIndex = savedInstanceState.getInt(EXTRA_SELECTED_TAB_INDEX)
            if (selectedTabIndex >= 0 && selectedTabIndex < MAX_NUM_TABS) {
                switchTab(selectedTabIndex, FragNavTransactionOptions.newBuilder().build())
            }


            //Successfully restored state
            return true
        } catch (ex: Throwable) {
            tagCount = 0
            mCurrentFrag = null
            fragmentStacks.clear()
            logError("Could not restore fragment state", ex)
            return false
        }

    }
    //endregion

    enum class TransactionType {
        PUSH,
        POP,
        REPLACE
    }

    //Declare the TabIndex annotation
    @IntDef(NO_TAB.toLong(), TAB1.toLong(), TAB2.toLong(), TAB3.toLong(), TAB4.toLong(), TAB5.toLong(), TAB6.toLong(), TAB7.toLong(), TAB8.toLong(), TAB9.toLong(), TAB10.toLong(), TAB11.toLong(), TAB12.toLong(), TAB13.toLong(), TAB14.toLong(), TAB15.toLong(), TAB16.toLong(), TAB17.toLong(), TAB18.toLong(), TAB19.toLong(), TAB20.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class TabIndex


    // Declare Transit Styles
    @IntDef(FragmentTransaction.TRANSIT_NONE.toLong(), FragmentTransaction.TRANSIT_FRAGMENT_OPEN.toLong(), FragmentTransaction.TRANSIT_FRAGMENT_CLOSE.toLong(), FragmentTransaction.TRANSIT_FRAGMENT_FADE.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    internal annotation class Transit

    /**
     * Define what happens when we try to pop on a tab where root fragment is at the top
     */
    @IntDef(DETACH.toLong(), HIDE.toLong(), DETACH_ON_NAVIGATE_HIDE_ON_SWITCH.toLong())
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    internal annotation class FragmentHideStrategy

    interface RootFragmentListener {
        /**
         * Dynamically create the Fragment that will go on the bottom of the stack
         *
         * @param index the index that the root of the stack Fragment needs to go
         * @return the new Fragment
         */
        fun getRootFragment(index: Int): Fragment
    }

    interface TransactionListener {

        fun onTabTransaction(fragment: Fragment?, index: Int)

        fun onFragmentTransaction(fragment: Fragment?, transactionType: TransactionType)
    }

    class Builder(private val savedInstanceState: Bundle?, val fragmentManager: FragmentManager, val containerId: Int) {
        internal var rootFragmentListener: RootFragmentListener? = null
        internal var transactionListener: TransactionListener? = null
        internal var defaultTransactionOptions: FragNavTransactionOptions? = null
        internal var numberOfTabs = 0
        internal var rootFragments: MutableList<Fragment>? = null

        @TabIndex
        internal var selectedTabIndex = TAB1

        @FragmentHideStrategy
        internal var fragmentHideStrategy = DETACH

        @FragNavTabHistoryController.PopStrategy
        internal var popStrategy = CURRENT_TAB

        internal var fragNavSwitchController: FragNavSwitchController? = null
        internal var createEager = false

        internal var fragNavLogger: FragNavLogger? = null

        /**
         * @param selectedTabIndex The initial tab index to be used must be in range of rootFragments size
         */
        fun selectedTabIndex(@TabIndex selectedTabIndex: Int): Builder {
            this.selectedTabIndex = selectedTabIndex
            if (rootFragments != null && selectedTabIndex > numberOfTabs) {
                throw IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks")
            }
            return this
        }

        /**
         * @param rootFragment A single root fragment. This library can still be helpful when managing a single stack of fragments
         */
        fun rootFragment(rootFragment: Fragment): Builder {
            rootFragments = ArrayList(1)
            rootFragments!!.add(rootFragment)
            numberOfTabs = 1
            return rootFragments(rootFragments!!)
        }

        /**
         * @param rootFragments a list of root fragments. root Fragments are the root fragments that exist on any tab structure. If only one fragment is sent in, fragnav will still manage
         * transactions
         */
        fun rootFragments(rootFragments: List<Fragment>): Builder {
            this.rootFragments = rootFragments.toMutableList()
            numberOfTabs = rootFragments.size
            if (numberOfTabs > MAX_NUM_TABS) {
                throw IllegalArgumentException("Number of root fragments cannot be greater than " + MAX_NUM_TABS)
            }
            return this
        }

        /**
         * @param transactionOptions The default transaction options to be used unless otherwise defined.
         */
        fun defaultTransactionOptions(transactionOptions: FragNavTransactionOptions): Builder {
            defaultTransactionOptions = transactionOptions
            return this
        }

        /**
         * @param rootFragmentListener a listener that allows for dynamically creating root fragments
         * @param numberOfTabs         the number of tabs that will be switched between
         */
        fun rootFragmentListener(rootFragmentListener: RootFragmentListener, numberOfTabs: Int): Builder {
            this.rootFragmentListener = rootFragmentListener
            this.numberOfTabs = numberOfTabs
            if (this.numberOfTabs > MAX_NUM_TABS) {
                throw IllegalArgumentException("Number of tabs cannot be greater than " + MAX_NUM_TABS)
            }
            return this
        }

        /**
         * @param transactionListener A listener to be implemented (typically within the main activity) to fragment transactions (including tab switches)
         */
        fun transactionListener(transactionListener: TransactionListener): Builder {
            this.transactionListener = transactionListener
            return this
        }

        /**
         * @param popStrategy Switch between different approaches of handling tab history while popping fragments on current tab
         */
        fun popStrategy(@FragNavTabHistoryController.PopStrategy popStrategy: Int): Builder {
            if (popStrategy != UNIQUE_TAB_HISTORY || popStrategy != UNLIMITED_TAB_HISTORY) {
                throw IllegalStateException("UNIQUE_TAB_HISTORY and UNLIMITED_TAB_HISTORY require FragNavSwitchController, please use `switchController` instead ")
            }
            this.popStrategy = popStrategy
            return this
        }

        /**
         * @param fragmentHideStrategy Switch between different approaches of hiding inactive and showing active fragments
         */
        fun fragmentHideStrategy(@FragmentHideStrategy fragmentHideStrategy: Int): Builder {
            this.fragmentHideStrategy = fragmentHideStrategy
            return this
        }

        /**
         * @param createEager Should initially create all tab's topmost fragment
         */
        fun eager(createEager: Boolean): Builder {
            this.createEager = createEager
            return this
        }

        /**
         * @param fragNavSwitchController Handles switch requests
         */
        fun switchController(fragNavSwitchController: FragNavSwitchController, @FragNavTabHistoryController.PopStrategy popStrategy: Int): Builder {
            this.fragNavSwitchController = fragNavSwitchController
            this.popStrategy = popStrategy
            return this
        }

        /**
         * @param fragNavLogger Reports errors for the client
         */
        fun logger(fragNavLogger: FragNavLogger): Builder {
            this.fragNavLogger = fragNavLogger
            return this
        }

        fun build(): FragNavController {
            if (rootFragmentListener == null && rootFragments == null) {
                throw IndexOutOfBoundsException("Either a root fragment(s) needs to be set, or a fragment listener")
            }
            return FragNavController(this, savedInstanceState)
        }
    }

    inner class DefaultFragNavPopController : com.ncapdevi.fragnav.FragNavPopController {
        @Throws(UnsupportedOperationException::class)
        override fun tryPopFragments(popDepth: Int, transactionOptions: FragNavTransactionOptions?): Int {
            return this@FragNavController.tryPopFragmentsFromCurrentStack(popDepth, transactionOptions)
        }
    }

    companion object {
        // Declare the constants. A maximum of 5 tabs is recommended for bottom navigation, this is per Material Design's Bottom Navigation's design spec.
        const val NO_TAB = -1
        const val TAB1 = 0
        const val TAB2 = 1
        const val TAB3 = 2
        const val TAB4 = 3
        const val TAB5 = 4
        const val TAB6 = 5
        const val TAB7 = 6
        const val TAB8 = 7
        const val TAB9 = 8
        const val TAB10 = 9
        const val TAB11 = 10
        const val TAB12 = 11
        const val TAB13 = 12
        const val TAB14 = 13
        const val TAB15 = 14
        const val TAB16 = 15
        const val TAB17 = 16
        const val TAB18 = 17
        const val TAB19 = 18
        const val TAB20 = 19

        private val MAX_NUM_TABS = 20

        // Extras used to store savedInstanceState
        private val EXTRA_TAG_COUNT = FragNavController::class.java.name + ":EXTRA_TAG_COUNT"
        private val EXTRA_SELECTED_TAB_INDEX = FragNavController::class.java.name + ":EXTRA_SELECTED_TAB_INDEX"
        private val EXTRA_CURRENT_FRAGMENT = FragNavController::class.java.name + ":EXTRA_CURRENT_FRAGMENT"
        private val EXTRA_FRAGMENT_STACK = FragNavController::class.java.name + ":EXTRA_FRAGMENT_STACK"

        fun newBuilder(savedInstanceState: Bundle?, fragmentManager: FragmentManager, containerId: Int): Builder {
            return Builder(savedInstanceState, fragmentManager, containerId)
        }

        /**
         * Using attach and detach methods of Fragment transaction to switch between fragments
         */
        const val DETACH = 0

        /**
         * Using show and hide methods of Fragment transaction to switch between fragments
         */
        const val HIDE = 1

        /**
         * Using attach and detach methods of Fragment transaction to navigate between fragments on the current tab but
         * using show and hide methods to switch between tabs
         */
        const val DETACH_ON_NAVIGATE_HIDE_ON_SWITCH = 2
    }
}
