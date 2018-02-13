package com.ncapdevi.fragnav

import android.support.annotation.AnimRes
import android.support.annotation.StyleRes
import android.support.v4.app.FragmentTransaction
import android.support.v4.util.Pair
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by niccapdevila on 2/15/17.
 */

class FragNavTransactionOptionsTest {

    @Test
    fun buildTransactionOptions() {
        val breadCrumbShortTitle = "Short Title"
        val breadCrumbTitle = "Long Title"

        @AnimRes
        val enterAnim = 1
        @AnimRes
        val exitAnim = 2
        @AnimRes
        val popEnterAnim = 3
        @AnimRes
        val popExitAnim = 4

        @StyleRes
        val transitionStyle = 5

        val fragNavTransactionOptions = FragNavTransactionOptions.newBuilder()
            .breadCrumbShortTitle(breadCrumbShortTitle)
            .breadCrumbTitle(breadCrumbTitle)
            .transition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .customAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
            .transitionStyle(transitionStyle)
            .addSharedElement(Pair(null, "test"))
            .addSharedElement(Pair(null, "test2")).build()

        assertTrue(breadCrumbShortTitle.equals(fragNavTransactionOptions.breadCrumbShortTitle, ignoreCase = true))
        assertTrue(breadCrumbTitle.equals(fragNavTransactionOptions.breadCrumbTitle, ignoreCase = true))

        assertTrue(transitionStyle == fragNavTransactionOptions.transitionStyle)

        assertTrue(FragmentTransaction.TRANSIT_FRAGMENT_FADE == fragNavTransactionOptions.transition)


        assertTrue(enterAnim == fragNavTransactionOptions.enterAnimation)
        assertTrue(exitAnim == fragNavTransactionOptions.exitAnimation)
        assertTrue(popEnterAnim == fragNavTransactionOptions.popEnterAnimation)
        assertTrue(popExitAnim == fragNavTransactionOptions.popExitAnimation)


        assertTrue(fragNavTransactionOptions.sharedElements.size == 2)
    }
}
