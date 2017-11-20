package com.ncapdevi.fragnav;

public interface FragNavPopController {
    int tryPopFragments(int popDepth, FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException;
}
