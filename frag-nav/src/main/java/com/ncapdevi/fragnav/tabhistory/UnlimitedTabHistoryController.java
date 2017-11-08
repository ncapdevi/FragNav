package com.ncapdevi.fragnav.tabhistory;

import android.support.annotation.NonNull;

import com.ncapdevi.fragnav.FragNavPopController;
import com.ncapdevi.fragnav.FragNavSwitchController;

import java.util.ArrayList;
import java.util.Stack;

public class UnlimitedTabHistoryController extends CollectionFragNavTabHistoryController {
    private Stack<Integer> tabHistory = new Stack<>();

    public UnlimitedTabHistoryController(FragNavPopController fragNavPopController,
                                         FragNavSwitchController fragNavSwitchController) {
        super(fragNavPopController, fragNavSwitchController);
    }

    @Override
    int getCollectionSize() {
        return tabHistory.size();
    }

    @Override
    int getAndRemoveIndex() {
        tabHistory.pop();
        return tabHistory.pop();
    }

    @Override
    public void switchTab(int index) {
        tabHistory.push(index);
    }

    @NonNull
    @Override
    ArrayList<Integer> getHistory() {
        return new ArrayList<>(tabHistory);
    }

    @Override
    void setHistory(@NonNull ArrayList<Integer> history) {
        tabHistory.clear();
        tabHistory.addAll(history);
    }
}
