@file:Suppress("MemberVisibilityCanBePrivate")

package com.ncapdevi.fragnav

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.ncapdevi.fragnav.tabhistory.*
import org.json.JSONArray
import java.lang.ref.WeakReference
import java.util.*

@Suppress("unused")
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
class FragNavController constructor(private val fragmentManger: FragmentManager, @IdRes private val containerId: Int) {

    //region Public properties
    var rootFragments: List<Fragment>? = null
        set(value) {
            if (value != null) {
                if (rootFragmentListener != null) {
                    throw IllegalStateException("Root fragments and root fragment listener can not be set the same time")
                }

                if (value.size > FragNavController.MAX_NUM_TABS) {
                    throw IllegalArgumentException("Number of root fragments cannot be greater than " + FragNavController.MAX_NUM_TABS)
                }
            }

            field = value
        }
    var defaultTransactionOptions: FragNavTransactionOptions? = null
    var fragNavLogger: FragNavLogger? = null
    var rootFragmentListener: RootFragmentListener? = null

    var transactionListener: TransactionListener? = null
    var navigationStrategy: NavigationStrategy = CurrentTabStrategy()
        set(value) {
            field = value
            fragNavTabHistoryController = when (value) {
                is UniqueTabHistoryStrategy -> UniqueTabHistoryController(DefaultFragNavPopController(), value.fragNavSwitchController)
                is UnlimitedTabHistoryStrategy -> UnlimitedTabHistoryController(DefaultFragNavPopController(), value.fragNavSwitchController)
                else -> CurrentTabHistoryController(DefaultFragNavPopController())
            }

        }

    var fragmentHideStrategy = FragNavController.DETACH
    var createEager = false

    @TabIndex
    @get:CheckResult
    @get:TabIndex
    var currentStackIndex: Int = FragNavController.TAB1
        private set
    //endregion

    //region Private properties
    private val fragmentStacksTags: MutableList<Stack<String>> = ArrayList()
    private var tagCount: Int = 0
    private var mCurrentFrag: Fragment? = null
    private var mCurrentDialogFrag: DialogFragment? = null

    private var executingTransaction: Boolean = false
    private var fragNavTabHistoryController: FragNavTabHistoryController = CurrentTabHistoryController(DefaultFragNavPopController())
    private val fragmentCache = mutableMapOf<String, WeakReference<Fragment>>()
    //endregion


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
            } else if (fragmentStacksTags.isEmpty()) {
                return null
            }
            //if not, try to pull it from the stack
            val fragmentStack = fragmentStacksTags[currentStackIndex]
            if (!fragmentStack.isEmpty()) {
                val fragmentByTag = getFragment(fragmentStack.peek())
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
                val fragmentManager: FragmentManager = getFragmentManagerForDialog()
                mCurrentDialogFrag = fragmentManager.fragments.firstOrNull { it is DialogFragment } as DialogFragment?
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
        get() = fragmentStacksTags.size

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
        get() = fragmentStacksTags.getOrNull(currentStackIndex)?.size == 1


    /**
     * Helper function to get whether the fragmentManger has gone through a stateSave, if this is true, you probably want to commit  allowing state loss
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


    fun initialize(@TabIndex index: Int = TAB1, savedInstanceState: Bundle? = null) {
        if (rootFragmentListener == null && rootFragments == null) {
            throw IndexOutOfBoundsException("Either a root fragment(s) needs to be set, or a fragment listener")
        } else if (rootFragmentListener != null && rootFragments != null) {
            throw java.lang.IllegalStateException("Shouldn't have both a rootFragmentListener and rootFragments set, this is clearly a mistake")
        }

        val numberOfTabs: Int = rootFragmentListener?.numberOfRootFragments ?: rootFragments?.size
        ?: 0

        //Attempt to restore from bundle, if not, initialize
        if (!restoreFromBundle(savedInstanceState)) {
            fragmentStacksTags.clear()
            for (i in 0 until numberOfTabs) {
                fragmentStacksTags.add(Stack())
            }


            currentStackIndex = index
            if (currentStackIndex > fragmentStacksTags.size) {
                throw IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks")
            }
            fragNavTabHistoryController.switchTab(index)

            currentStackIndex = index
            clearFragmentManager()
            clearDialogFragment()

            if (index == NO_TAB) {
                return
            }

            val ft = createTransactionWithOptions(defaultTransactionOptions, false, false)

            val lowerBound = if (createEager) 0 else index
            val upperBound = if (createEager) fragmentStacksTags.size else index + 1
            for (i in lowerBound until upperBound) {
                currentStackIndex = i
                val fragment = getRootFragment(i)
                val fragmentTag = generateTag(fragment)
                fragmentStacksTags[currentStackIndex].push(fragmentTag)
                ft.addSafe(containerId, fragment, fragmentTag)
                if (i != index) {
                    when {
                        shouldDetachAttachOnSwitch() -> ft.detach(fragment)
                        shouldRemoveAttachOnSwitch() -> ft.remove(fragment)
                        else -> ft.hide(fragment)
                    }
                } else {
                    mCurrentFrag = fragment
                }
            }
            currentStackIndex = index

            commitTransaction(ft, defaultTransactionOptions)

            transactionListener?.onTabTransaction(currentFrag, currentStackIndex)
        } else {
            fragNavTabHistoryController.restoreFromBundle(savedInstanceState)
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
    @Throws(IndexOutOfBoundsException::class)
    @JvmOverloads
    fun switchTab(@TabIndex index: Int, transactionOptions: FragNavTransactionOptions? = defaultTransactionOptions) {
        switchTabInternal(index, transactionOptions)
    }

    @Throws(IndexOutOfBoundsException::class)
    private fun switchTabInternal(@TabIndex index: Int, transactionOptions: FragNavTransactionOptions?) {
        //Check to make sure the tab is within range
        if (index >= fragmentStacksTags.size) {
            throw IndexOutOfBoundsException("Can't switch to a tab that hasn't been initialized, " +
                    "Index : " + index + ", current stack size : " + fragmentStacksTags.size +
                    ". Make sure to create all of the tabs you need in the Constructor or provide a way for them to be created via RootFragmentListener.")
        }
        if (currentStackIndex != index) {
            val ft = createTransactionWithOptions(transactionOptions, index < currentStackIndex)
            removeCurrentFragment(ft, shouldDetachAttachOnSwitch(), shouldRemoveAttachOnSwitch())

            currentStackIndex = index

            fragNavTabHistoryController.switchTab(index)

            var fragment: Fragment? = null
            if (index == NO_TAB) {
                commitTransaction(ft, transactionOptions)
            } else {
                //Attempt to reattach previous fragment
                fragment = addPreviousFragment(ft, shouldDetachAttachOnSwitch() || shouldRemoveAttachOnSwitch())
                commitTransaction(ft, transactionOptions)
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
    fun pushFragment(fragment: Fragment?, transactionOptions: FragNavTransactionOptions? = defaultTransactionOptions) {
        if (fragment != null && currentStackIndex != NO_TAB) {
            val ft = createTransactionWithOptions(transactionOptions, false)

            removeCurrentFragment(ft, shouldDetachAttachOnPushPop(), shouldRemoveAttachOnSwitch())

            val fragmentTag = generateTag(fragment)
            fragmentStacksTags[currentStackIndex].push(fragmentTag)
            ft.addSafe(containerId, fragment, fragmentTag)

            commitTransaction(ft, transactionOptions)

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
    fun popFragment(transactionOptions: FragNavTransactionOptions? = defaultTransactionOptions): Boolean {
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
        if (navigationStrategy is CurrentTabStrategy && isRootFragment) {
            throw UnsupportedOperationException(
                    "You can not popFragment the rootFragment. If you need to change this fragment, use replaceFragment(fragment)")
        } else if (popDepth < 1) {
            throw UnsupportedOperationException("popFragments parameter needs to be greater than 0")
        } else if (currentStackIndex == NO_TAB) {
            throw UnsupportedOperationException("You can not pop fragments when no tab is selected")
        }

        //If our popDepth is big enough that it would just clear the stack, then call that.
        val currentStack = fragmentStacksTags[currentStackIndex]
        val poppableSize = currentStack.size - 1
        if (popDepth >= poppableSize) {
            clearStack(transactionOptions)
            return poppableSize
        }

        val ft = createTransactionWithOptions(transactionOptions, true)

        //Pop the number of the fragments on the stack and remove them from the FragmentManager
        for (i in 0 until popDepth) {
            val fragment = getFragment(currentStack.pop())
            if (fragment != null) {
                ft.removeSafe(fragment)
            }
        }

        // Attempt to reattach previous fragment
        val fragment = addPreviousFragment(ft, shouldDetachAttachOnPushPop())

        commitTransaction(ft, transactionOptions)
        mCurrentFrag = fragment
        transactionListener?.onFragmentTransaction(currentFrag, TransactionType.POP)
        return popDepth
    }

    /**
     * Pop the current fragment from the current tab
     */
    @Throws(UnsupportedOperationException::class)
    fun popFragments(popDepth: Int) {
        popFragments(popDepth, defaultTransactionOptions)
    }

    /**
     * Clears the current tab's stack to get to just the bottom Fragment. This will reveal the root fragment
     *
     * @param transactionOptions Transaction options to be displayed
     */
    @JvmOverloads
    fun clearStack(transactionOptions: FragNavTransactionOptions? = defaultTransactionOptions) {
        clearStack(currentStackIndex,transactionOptions)
    }

    /**
     * Clears the passed tab's stack to get to just the bottom Fragment. This will reveal the root fragment
     *
     * @param tabIndex Index of tab that needs to be cleared
     * @param transactionOptions Transaction options to be displayed
     */
    @JvmOverloads
    fun clearStack(tabIndex: Int, transactionOptions: FragNavTransactionOptions? = defaultTransactionOptions) {
        if (tabIndex == NO_TAB) {
            return
        }

        //Grab Current stack
        val fragmentStack = fragmentStacksTags[tabIndex]

        // Only need to start popping and reattach if the stack is greater than 1
        if (fragmentStack.size > 1) {
            //Only animate if we're clearing the current stack
            val shouldAnimate = tabIndex == currentStackIndex
            val ft = createTransactionWithOptions(transactionOptions,true, shouldAnimate)

            //Pop all of the fragments on the stack and remove them from the FragmentManager
            while (fragmentStack.size > 1) {
                val fragment = getFragment(fragmentStack.pop())
                if (fragment != null) {
                    ft.removeSafe(fragment)
                }
            }

            // Attempt to reattach previous fragment
            val fragment = addPreviousFragment(ft, shouldDetachAttachOnPushPop())

            commitTransaction(ft, transactionOptions)
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
    fun replaceFragment(fragment: Fragment, transactionOptions: FragNavTransactionOptions? = defaultTransactionOptions) {
        val poppingFrag = currentFrag

        if (poppingFrag != null) {
            val ft = createTransactionWithOptions(transactionOptions, false)

            //overly cautious fragment popFragment

            val fragmentTag = generateTag(fragment)
            ft.replace(containerId, fragment, fragmentTag)
            commitTransaction(ft, transactionOptions)

            fragmentStacksTags[currentStackIndex].apply {
                if (isNotEmpty()) {
                    pop()
                }
                push(fragmentTag)
            }
            mCurrentFrag = fragment

            transactionListener?.onFragmentTransaction(currentFrag, TransactionType.REPLACE)
        }
    }

    /**
     * Clear any DialogFragments that may be shown
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun clearDialogFragment() {
        val currentDialogFrag = mCurrentDialogFrag
        if (currentDialogFrag != null) {
            currentDialogFrag.dismiss()
            mCurrentDialogFrag = null
        } else {
            val fragmentManager: FragmentManager = getFragmentManagerForDialog()
            fragmentManager.fragments.forEach {
                if (it is DialogFragment) {
                    it.dismiss()
                }
            }
        }
    }

    /**
     * Display a DialogFragment on the screen
     *
     * @param dialogFragment The Fragment to be Displayed
     */
    fun showDialogFragment(dialogFragment: DialogFragment?) {
        //Clear any current dialog fragments
        clearDialogFragment()

        if (dialogFragment != null) {
            val fragmentManager: FragmentManager = getFragmentManagerForDialog()
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

        if (fragment == null) {
            fragment = rootFragmentListener?.getRootFragment(index)
        }

        if (fragment == null) {
            fragment = rootFragments?.getOrNull(index)
        }


        if (fragment == null) {
            throw IllegalStateException("Either you haven't past in a fragment at this index in your constructor, or you haven't " + "provided a way to create it while via your RootFragmentListener.getRootFragment(index)")
        }

        return fragment
    }

    /**
     * Adds fragment to the fragment transaction, also add it to local cache so we can obtain it even before transaction has been committed.
     */
    private fun FragmentTransaction.addSafe(containerViewId: Int, fragment: Fragment, tag: String) {
        fragmentCache[tag] = WeakReference(fragment)
        add(containerViewId, fragment, tag)
    }

    /**
     * Remove the fragment from transaction and also from cache if found.
     */
    private fun FragmentTransaction.removeSafe(fragment: Fragment) {
        val tag = fragment.tag
        if (tag != null) {
            fragmentCache.remove(tag)
        }
        remove(fragment)
    }

    /**
     * Will attempt to reattach a previous fragment or fragments in fragment stack until it succeeds or replace with root fragment.
     *
     * @param ft current fragment transaction
     * @return Fragment if we were able to find and reattach it
     */
    private fun addPreviousFragment(ft: FragmentTransaction, isAttach: Boolean): Fragment {
        val fragmentStack = fragmentStacksTags[currentStackIndex]
        var currentFragment: Fragment? = null
        var currentTag: String? = null
        var index = 0
        val initialSize = fragmentStack.size
        while (currentFragment == null && fragmentStack.isNotEmpty()) {
            index++
            currentTag = fragmentStack.pop()
            currentFragment = getFragment(currentTag)
        }
        return if (currentFragment != null) {
            if (index > 1) {
                val message = "Could not restore top fragment on current stack"
                logError(message, IllegalStateException(message))
            }
            fragmentStack.push(currentTag)
            if (isAttach) {
                ft.attach(currentFragment)
            } else {
                ft.show(currentFragment)
            }
            currentFragment
        } else {
            if (initialSize > 0) {
                val message = "Could not restore any fragment on current stack, adding new root fragment"
                logError(message, IllegalStateException(message))
            }
            val rootFragment = getRootFragment(currentStackIndex)
            val rootTag = generateTag(rootFragment)
            fragmentStack.push(rootTag)
            ft.addSafe(containerId, rootFragment, rootTag)
            rootFragment
        }
    }

    /**
     * Attempts to detach any current fragment if it exists, and if none is found, returns.
     *
     * @param ft the current transaction being performed
     */
    private fun removeCurrentFragment(ft: FragmentTransaction, isDetach: Boolean, isRemove: Boolean) {
        currentFrag?.let {
            when {
                isDetach -> ft.detach(it)
                isRemove -> ft.remove(it)
                else -> ft.hide(it)
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

    private fun getFragment(tag: String): Fragment? {
        val weakReference = fragmentCache[tag]
        if (weakReference != null) {
            val fragment = weakReference.get()
            if (fragment != null) {
                return fragment
            }
            fragmentCache.remove(tag)
        }
        return fragmentManger.findFragmentByTag(tag)
    }


    /**
     * Private helper function to clear out the fragment manager on initialization. All fragment management should be done via FragNav.
     */
    private fun clearFragmentManager() {
        val currentFragments = fragmentManger.fragments.filterNotNull()
        if (currentFragments.isNotEmpty()) {
            with(createTransactionWithOptions(defaultTransactionOptions, false)) {
                currentFragments.forEach { removeSafe(it) }
                commitTransaction(this, defaultTransactionOptions)
            }
        }
    }

    /**
     * Setup a fragment transaction with the given option
     *
     * @param transactionOptions The options that will be set for this transaction
     */
    @SuppressLint("CommitTransaction")
    @CheckResult
    private fun createTransactionWithOptions(transactionOptions: FragNavTransactionOptions?, isPopping: Boolean, animated: Boolean = true): FragmentTransaction {
        return fragmentManger.beginTransaction().apply {
            transactionOptions?.also { options ->
                // Not using standard pop support since we handle backstack manually
                if (animated) {
                    if (isPopping) {
                        setCustomAnimations(
                            transactionOptions.popEnterAnimation,
                            transactionOptions.popExitAnimation
                        )
                    } else {
                        setCustomAnimations(
                            transactionOptions.enterAnimation,
                            transactionOptions.exitAnimation
                        )
                    }
                }

                setTransitionStyle(options.transitionStyle)

                setTransition(options.transition)

                options.sharedElements.forEach { sharedElement ->
                    sharedElement.first?.let {
                        sharedElement.second?.let { it1 ->
                            addSharedElement(
                                    it,
                                    it1
                            )
                        }
                    }
                }

                when {
                    options.breadCrumbTitle != null -> setBreadCrumbTitle(options.breadCrumbTitle)
                    options.breadCrumbShortTitle != null -> setBreadCrumbShortTitle(options.breadCrumbShortTitle)
                }

                setReorderingAllowed(options.reordering)
            }
        }
    }

    /**
     * Helper function to commit fragment transaction with transaction option - allowStateLoss
     *
     * @param fragmentTransaction
     * @param transactionOptions
     */
    private fun commitTransaction(fragmentTransaction: FragmentTransaction, transactionOptions: FragNavTransactionOptions?) {
        if (transactionOptions?.allowStateLoss == true) {
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

    private fun shouldRemoveAttachOnSwitch(): Boolean {
        return fragmentHideStrategy == REMOVE
    }

    /**
     * Get a copy of the stack at a given index
     *
     * @return requested stack
     */
    @CheckResult
    @Throws(IndexOutOfBoundsException::class)
    fun getStack(@TabIndex index: Int): Stack<Fragment>? {
        if (index == NO_TAB) {
            return null
        }
        return fragmentStacksTags[index].mapNotNullTo(Stack()) { s -> getFragment(s) }
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

    fun getFragmentManagerForDialog(): FragmentManager {
        val currentFrag = this.currentFrag
        return if (currentFrag?.isAdded == true) {
            currentFrag.childFragmentManager
        } else {
            this.fragmentManger
        }
    }

    //endregion

    //region SavedInstanceState

    /**
     * Call this in your Activity's onSaveInstanceState(Bundle outState) method to save the instance's state.
     *
     * @param outState The Bundle to save state information to
     */
    fun onSaveInstanceState(outState: Bundle?) {
        if (outState == null) {
            return
        }
        // Write tag count
        outState.putInt(EXTRA_TAG_COUNT, tagCount)

        // Write select tab
        outState.putInt(EXTRA_SELECTED_TAB_INDEX, currentStackIndex)

        // Write current fragment
        val currentFrag = currentFrag
        if (currentFrag != null) {
            outState.putString(EXTRA_CURRENT_FRAGMENT, currentFrag.tag)
        }


        // Write tag stacks

        try {
            val stackArrays = JSONArray()
            fragmentStacksTags.forEach { stack ->
                val stackArray = JSONArray()
                stack.forEach { stackArray.put(it) }
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
     * @return true if successful, false if not
     */
    private fun restoreFromBundle(savedInstanceState: Bundle?): Boolean {
        if (savedInstanceState == null) {
            return false
        }

        // Restore tag count
        tagCount = savedInstanceState.getInt(EXTRA_TAG_COUNT, 0)

        // Restore current fragment
        val tag = savedInstanceState.getString(EXTRA_CURRENT_FRAGMENT)
        if (tag !=null) {
            mCurrentFrag = getFragment(tag)
        }

        // Restore fragment stacks
        try {
            val stackArrays = JSONArray(savedInstanceState.getString(EXTRA_FRAGMENT_STACK))

            for (x in 0 until stackArrays.length()) {
                val stackArray = stackArrays.getJSONArray(x)
                val stack = Stack<String>()
                (0 until stackArray.length())
                        .map { stackArray.getString(it) }
                        .filter { !it.isNullOrEmpty() && !"null".equals(it, ignoreCase = true) }
                        .mapNotNullTo(stack) { it }

                fragmentStacksTags.add(stack)
            }
            // Restore selected tab if we have one
            val selectedTabIndex = savedInstanceState.getInt(EXTRA_SELECTED_TAB_INDEX)
            if (selectedTabIndex in 0..(MAX_NUM_TABS - 1)) {
                // Shortcut for switchTab. We  already restored fragment, so just notify history controller
                // We cannot use switchTab, because switchTab removes fragment, but we don't want it
                currentStackIndex = selectedTabIndex
                fragNavTabHistoryController.switchTab(selectedTabIndex)

                transactionListener?.onTabTransaction(mCurrentFrag, selectedTabIndex)
            }

            //Successfully restored state
            return true
        } catch (ex: Throwable) {
            tagCount = 0
            mCurrentFrag = null
            fragmentStacksTags.clear()
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
    @IntDef(NO_TAB, TAB1, TAB2, TAB3, TAB4, TAB5, TAB6, TAB7, TAB8, TAB9, TAB10, TAB11, TAB12, TAB13, TAB14, TAB15, TAB16, TAB17, TAB18, TAB19, TAB20)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class TabIndex


    // Declare Transit Styles
    @IntDef(FragmentTransaction.TRANSIT_NONE, FragmentTransaction.TRANSIT_FRAGMENT_OPEN, FragmentTransaction.TRANSIT_FRAGMENT_CLOSE, FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    internal annotation class Transit

    /**
     * Define what happens when we try to pop on a tab where root fragment is at the top
     */
    @IntDef(DETACH, HIDE, REMOVE, DETACH_ON_NAVIGATE_HIDE_ON_SWITCH)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class FragmentHideStrategy

    interface RootFragmentListener {
        val numberOfRootFragments: Int
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

        internal const val MAX_NUM_TABS = 20

        // Extras used to store savedInstanceState
        private val EXTRA_TAG_COUNT = FragNavController::class.java.name + ":EXTRA_TAG_COUNT"
        private val EXTRA_SELECTED_TAB_INDEX = FragNavController::class.java.name + ":EXTRA_SELECTED_TAB_INDEX"
        private val EXTRA_CURRENT_FRAGMENT = FragNavController::class.java.name + ":EXTRA_CURRENT_FRAGMENT"
        private val EXTRA_FRAGMENT_STACK = FragNavController::class.java.name + ":EXTRA_FRAGMENT_STACK"


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

        /**
         * Using create + attach and remove methods of Fragment transaction to switch between fragments
         */
        const val REMOVE = 3
    }
}
