package com.ncapdevi.fragnav;

public interface FragNavPopController {
    int popFragments(int popDepth, FragNavTransactionOptions transactionOptions) throws UnsupportedOperationException;
}
