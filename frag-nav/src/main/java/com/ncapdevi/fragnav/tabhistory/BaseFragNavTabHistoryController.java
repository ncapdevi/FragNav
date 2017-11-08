package com.ncapdevi.fragnav.tabhistory;

import com.ncapdevi.fragnav.FragNavPopController;

abstract class BaseFragNavTabHistoryController implements FragNavTabHistoryController {
    FragNavPopController fragNavPopController;

    BaseFragNavTabHistoryController(FragNavPopController fragNavPopController) {
        this.fragNavPopController = fragNavPopController;
    }
}
