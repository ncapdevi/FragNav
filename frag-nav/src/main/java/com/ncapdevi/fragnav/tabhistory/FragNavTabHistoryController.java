package com.ncapdevi.fragnav.tabhistory;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ncapdevi.fragnav.FragNavTransactionOptions;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface FragNavTabHistoryController {
    /**
     * Define what happens when we try to pop on a tab where root fragment is at the top
     */
    @Retention(SOURCE)
    @IntDef({CURRENT_TAB, UNIQUE_TAB_HISTORY, UNLIMITED_TAB_HISTORY})
    @interface PopStrategy {
    }

    /**
     * We only pop fragments from current tab, don't switch between tabs
     */
    int CURRENT_TAB = 0;

    /**
     * We keep a history of tabs (each tab is present only once) and we switch to previous tab in history when we pop on root fragment
     */
    int UNIQUE_TAB_HISTORY = 1;

    /**
     * We keep an uncapped history of tabs and we switch to previous tab in history when we pop on root fragment
     */
    int UNLIMITED_TAB_HISTORY = 2;

    boolean popFragments(int popDepth, FragNavTransactionOptions transactionOptions);

    void switchTab(int index);

    void onSaveInstanceState(@NonNull Bundle outState);

    void restoreFromBundle(@Nullable Bundle savedInstanceState);
}
