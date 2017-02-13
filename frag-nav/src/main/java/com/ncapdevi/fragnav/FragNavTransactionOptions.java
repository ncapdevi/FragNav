package com.ncapdevi.fragnav;

import android.support.annotation.AnimRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pair;
import android.view.View;

import java.util.List;

/**
 * Created by niccapdevila on 2/12/17.
 */


public class FragNavTransactionOptions {
    List<Pair<View, String>> sharedElements;
    @FragNavController.Transit
    Integer transition;
    @AnimRes
    Integer enterAnimationId;
    @AnimRes
    Integer exitAnimationId;
    @StyleRes
    Integer transitionStyle;
}