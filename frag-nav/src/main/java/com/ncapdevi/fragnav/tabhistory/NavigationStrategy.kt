package com.ncapdevi.fragnav.tabhistory

import com.ncapdevi.fragnav.FragNavSwitchController

sealed class NavigationStrategy

class CurrentTabStrategy : NavigationStrategy()

class UnlimitedTabHistoryStrategy(val fragNavSwitchController: FragNavSwitchController) : NavigationStrategy()

class UniqueTabHistoryStrategy(val fragNavSwitchController: FragNavSwitchController) : NavigationStrategy()