package com.ncapdevi.fragnav;

import android.support.annotation.AnimRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pair;
import android.view.View;

import java.util.List;

/**
 *
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

    private FragNavTransactionOptions(Builder builder) {
        sharedElements = builder.sharedElements;
        transition = builder.transition;
        enterAnimationId = builder.enterAnimationId;
        exitAnimationId = builder.exitAnimationId;
        transitionStyle = builder.transitionStyle;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private List<Pair<View, String>> sharedElements;
        private Integer transition;
        private Integer enterAnimationId;
        private Integer exitAnimationId;
        private Integer transitionStyle;

        private Builder() {
        }

        public Builder sharedElements(List<Pair<View, String>> val) {
            sharedElements = val;
            return this;
        }

        public Builder transition(@FragNavController.Transit Integer val) {
            transition = val;
            return this;
        }

        public Builder enterAnimationId(@AnimRes Integer val) {
            enterAnimationId = val;
            return this;
        }

        public Builder exitAnimationId(@AnimRes Integer val) {
            exitAnimationId = val;
            return this;
        }

        public Builder transitionStyle(@StyleRes Integer val) {
            transitionStyle = val;
            return this;
        }

        public FragNavTransactionOptions build() {
            return new FragNavTransactionOptions(this);
        }
    }
}

