package com.ncapdevi.fragnav.tabhistory;

import android.support.annotation.NonNull;

import com.ncapdevi.fragnav.FragNavPopController;
import com.ncapdevi.fragnav.FragNavSwitchController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class UniqueTabHistoryController extends CollectionFragNavTabHistoryController {
    private Set<Integer> tabHistory = new LinkedHashSet<>();

    public UniqueTabHistoryController(FragNavPopController fragNavPopController,
                                      FragNavSwitchController fragNavSwitchController) {
        super(fragNavPopController, fragNavSwitchController);
    }

    @Override
    int getCollectionSize() {
        return tabHistory.size();
    }

    @Override
    int getAndRemoveIndex() {
        ArrayList<Integer> tabList = getHistory();
        int currentPage = tabList.get(tabHistory.size() - 1);
        int targetPage = tabList.get(tabHistory.size() - 2);
        tabHistory.remove(currentPage);
        tabHistory.remove(targetPage);
        return targetPage;
    }

    @Override
    public void switchTab(int index) {
        tabHistory.remove(index);
        tabHistory.add(index);
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
