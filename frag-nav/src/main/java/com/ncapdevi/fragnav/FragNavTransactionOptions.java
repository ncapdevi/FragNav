package com.ncapdevi.fragnav;

import android.support.annotation.AnimRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */


public class FragNavTransactionOptions {
    List<Pair<View, String>> sharedElements;
    @FragNavController.Transit
    Integer transition;
    @AnimRes
    Integer enterAnimation;
    @AnimRes
    Integer exitAnimation;
    @StyleRes
    Integer transitionStyle;
    @AnimRes
    Integer popEnterAnimation;
    @AnimRes
    Integer popExitAnimation;
    String breadCrumbTitle;
    String breadCrumbShortTitle;

    private FragNavTransactionOptions(Builder builder) {
        sharedElements = builder.sharedElements;
        transition = builder.transition;
        enterAnimation = builder.enterAnimation;
        exitAnimation = builder.exitAnimation;
        transitionStyle = builder.transitionStyle;
        popEnterAnimation = builder.popEnterAnimation;
        popExitAnimation = builder.popExitAnimation;
        breadCrumbTitle = builder.breadCrumbTitle;
        breadCrumbShortTitle = builder.breadCrumbShortTitle;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private List<Pair<View, String>> sharedElements;
        private Integer transition;
        private Integer enterAnimation;
        private Integer exitAnimation;
        private Integer transitionStyle;
        private Integer popEnterAnimation;
        private Integer popExitAnimation;
        private String breadCrumbTitle;
        private String breadCrumbShortTitle;

        private Builder() {
        }

        public Builder addSharedElement(Pair<View, String> val) {
            if (sharedElements == null) {
                sharedElements = new ArrayList<>(3);
            }
            sharedElements.add(val);
            return this;
        }

        public Builder sharedElements(List<Pair<View, String>> val) {
            sharedElements = val;
            return this;
        }

        public Builder transition(@FragNavController.Transit Integer val) {
            transition = val;
            return this;
        }

        public Builder enterAnimation(@AnimRes Integer val) {
            enterAnimation = val;
            return this;
        }

        public Builder exitAnimation(@AnimRes Integer val) {
            exitAnimation = val;
            return this;
        }

        public Builder transitionStyle(@StyleRes Integer val) {
            transitionStyle = val;
            return this;
        }

        public Builder popEnterAnimation(Integer val) {
            popEnterAnimation = val;
            return this;
        }

        public Builder popExitAnimation(Integer val) {
            popExitAnimation = val;
            return this;
        }

        public Builder breadCrumbTitle(String val) {
            breadCrumbTitle = val;
            return this;
        }

        public Builder breadCrumbShortTitle(String val) {
            breadCrumbShortTitle = val;
            return this;
        }

        public FragNavTransactionOptions build() {
            return new FragNavTransactionOptions(this);
        }
    }
}

