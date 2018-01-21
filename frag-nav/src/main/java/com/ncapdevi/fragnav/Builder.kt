package com.ncapdevi.fragnav

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.ncapdevi.fragnav.tabhistory.FragNavTabHistoryController

class Builder(private val savedInstanceState: Bundle?, val fragmentManager: FragmentManager, val containerId: Int) {
    internal var rootFragmentListener: FragNavController.RootFragmentListener? = null
    internal var transactionListener: FragNavController.TransactionListener? = null
    internal var defaultTransactionOptions: FragNavTransactionOptions? = null
    internal var numberOfTabs = 0
    internal val rootFragments: MutableList<Fragment> = mutableListOf()

    @FragNavController.TabIndex
    internal var selectedTabIndex = FragNavController.TAB1

    @FragNavController.FragmentHideStrategy
    internal var fragmentHideStrategy = FragNavController.DETACH

    @FragNavTabHistoryController.PopStrategy
    internal var popStrategy = FragNavTabHistoryController.CURRENT_TAB

    internal var fragNavSwitchController: FragNavSwitchController? = null
    internal var createEager = false

    internal var fragNavLogger: FragNavLogger? = null

    /**
     * @param selectedTabIndex The initial tab index to be used must be in range of rootFragments size
     */
    fun selectedTabIndex(@FragNavController.TabIndex selectedTabIndex: Int): Builder {
        this.selectedTabIndex = selectedTabIndex
        if (selectedTabIndex > numberOfTabs) {
            throw IndexOutOfBoundsException("Starting index cannot be larger than the number of stacks")
        }
        return this
    }

    /**
     * @param rootFragment A single root fragment. This library can still be helpful when managing a single stack of fragments
     */
    fun rootFragment(rootFragment: Fragment): Builder {
        return rootFragments(listOf(rootFragment))
    }

    /**
     * @param rootFragments a list of root fragments. root Fragments are the root fragments that exist on any tab structure. If only one fragment is sent in, fragnav will still manage
     * transactions
     */
    fun rootFragments(rootFragments: List<Fragment>): Builder {
        this.rootFragments.addAll(rootFragments)
        numberOfTabs = rootFragments.size
        if (numberOfTabs > FragNavController.MAX_NUM_TABS) {
            throw IllegalArgumentException("Number of root fragments cannot be greater than " + FragNavController.MAX_NUM_TABS)
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
    fun rootFragmentListener(rootFragmentListener: FragNavController.RootFragmentListener, numberOfTabs: Int): Builder {
        this.rootFragmentListener = rootFragmentListener
        this.numberOfTabs = numberOfTabs
        if (this.numberOfTabs > FragNavController.MAX_NUM_TABS) {
            throw IllegalArgumentException("Number of tabs cannot be greater than " + FragNavController.MAX_NUM_TABS)
        }
        return this
    }

    /**
     * @param transactionListener A listener to be implemented (typically within the main activity) to fragment transactions (including tab switches)
     */
    fun transactionListener(transactionListener: FragNavController.TransactionListener): Builder {
        this.transactionListener = transactionListener
        return this
    }

    /**
     * @param popStrategy Switch between different approaches of handling tab history while popping fragments on current tab
     */
    fun popStrategy(@FragNavTabHistoryController.PopStrategy popStrategy: Int): Builder {
        if (popStrategy != FragNavTabHistoryController.UNIQUE_TAB_HISTORY || popStrategy != FragNavTabHistoryController.UNLIMITED_TAB_HISTORY) {
            throw IllegalStateException("UNIQUE_TAB_HISTORY and UNLIMITED_TAB_HISTORY require FragNavSwitchController, please use `switchController` instead ")
        }
        this.popStrategy = popStrategy
        return this
    }

    /**
     * @param fragmentHideStrategy Switch between different approaches of hiding inactive and showing active fragments
     */
    fun fragmentHideStrategy(@FragNavController.FragmentHideStrategy fragmentHideStrategy: Int): Builder {
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
        if (rootFragmentListener == null && rootFragments.isNotEmpty()) {
            throw IndexOutOfBoundsException("Either a root fragment(s) needs to be set, or a fragment listener")
        }
        return FragNavController(this, savedInstanceState)
    }
}