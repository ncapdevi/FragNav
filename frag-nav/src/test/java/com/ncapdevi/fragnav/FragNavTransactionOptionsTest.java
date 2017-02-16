package com.ncapdevi.fragnav;

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
        FragNavTransactionOptions fragNavTransactionOptions = FragNavTransactionOptions.newBuilder()
                .breadCrumbShortTitle("Short Title")
                .breadCrumbTitle("Long Title")
                .addSharedElement(new Pair<View, String>(null, "test"))
                .addSharedElement(new Pair<View, String>(null, "test2")).build();

        assertTrue(fragNavTransactionOptions.sharedElements.size() == 2);


    }
}
