package com.ncapdevi.fragnav;

import android.support.annotation.AnimRes;
import android.support.annotation.StyleRes;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.view.View;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by niccapdevila on 2/15/17.
 */

public class FragNavTransactionOptionsTest {

    @Test
    public void buildTransactionOptions() {
        String breadCrumbShortTitle = "Short Title";
        String breadCrumbTitle = "Long Title";

        @AnimRes
        int enterAnim = 1;
        @AnimRes
        int exitAnim = 2;
        @AnimRes
        int popEnterAnim = 3;
        @AnimRes
        int popExitAnim = 4;

        @StyleRes
        int transitionStyle = 5;

        FragNavTransactionOptions fragNavTransactionOptions = FragNavTransactionOptions.Companion.newBuilder()
                .breadCrumbShortTitle(breadCrumbShortTitle)
                .breadCrumbTitle(breadCrumbTitle)
                .transition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .customAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                .transitionStyle(transitionStyle)
                .addSharedElement(new Pair<View, String>(null, "test"))
                .addSharedElement(new Pair<View, String>(null, "test2")).build();

        assertTrue(breadCrumbShortTitle.equalsIgnoreCase(fragNavTransactionOptions.getBreadCrumbShortTitle()));
        assertTrue(breadCrumbTitle.equalsIgnoreCase(fragNavTransactionOptions.getBreadCrumbTitle()));

        assertTrue(transitionStyle == fragNavTransactionOptions.getTransitionStyle());

        assertTrue(FragmentTransaction.TRANSIT_FRAGMENT_FADE == fragNavTransactionOptions.getTransition());


        assertTrue(enterAnim == fragNavTransactionOptions.getEnterAnimation());
        assertTrue(exitAnim == fragNavTransactionOptions.getExitAnimation());
        assertTrue(popEnterAnim == fragNavTransactionOptions.getPopEnterAnimation());
        assertTrue(popExitAnim == fragNavTransactionOptions.getPopExitAnimation());


        assertTrue(fragNavTransactionOptions.getSharedElements().size() == 2);


    }
}
