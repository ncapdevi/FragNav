package com.ncapdevi.fragnav;

import static org.junit.Assert.assertTrue;

import android.support.annotation.AnimRes;
import android.support.annotation.StyleRes;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.view.View;

import org.junit.Test;

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

        FragNavTransactionOptions fragNavTransactionOptions = FragNavTransactionOptions.newBuilder()
                .breadCrumbShortTitle(breadCrumbShortTitle)
                .breadCrumbTitle(breadCrumbTitle)
                .transition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .customAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                .transitionStyle(transitionStyle)
                .addSharedElement(new Pair<View, String>(null, "test"))
                .addSharedElement(new Pair<View, String>(null, "test2")).build();

        assertTrue(breadCrumbShortTitle.equalsIgnoreCase(fragNavTransactionOptions.breadCrumbShortTitle));
        assertTrue(breadCrumbTitle.equalsIgnoreCase(fragNavTransactionOptions.breadCrumbTitle));

        assertTrue(transitionStyle == fragNavTransactionOptions.transitionStyle);

        assertTrue(FragmentTransaction.TRANSIT_FRAGMENT_FADE == fragNavTransactionOptions.transition);


        assertTrue(enterAnim == fragNavTransactionOptions.enterAnimation);
        assertTrue(exitAnim == fragNavTransactionOptions.exitAnimation);
        assertTrue(popEnterAnim == fragNavTransactionOptions.popEnterAnimation);
        assertTrue(popExitAnim == fragNavTransactionOptions.popExitAnimation);


        assertTrue(fragNavTransactionOptions.sharedElements.size() == 2);


    }
}
