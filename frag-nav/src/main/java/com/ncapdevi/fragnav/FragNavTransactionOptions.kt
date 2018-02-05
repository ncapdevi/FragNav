package com.ncapdevi.fragnav

import android.support.annotation.AnimRes
import android.support.annotation.StyleRes
import android.support.v4.app.FragmentTransaction
import android.support.v4.util.Pair
import android.view.View


class FragNavTransactionOptions private constructor(builder: Builder) {
    val sharedElements: List<Pair<View, String>> = builder.sharedElements
    @FragNavController.Transit
    val transition = builder.transition
    @AnimRes
    val enterAnimation = builder.enterAnimation
    @AnimRes
    val exitAnimation = builder.exitAnimation
    @AnimRes
    val popEnterAnimation = builder.popEnterAnimation
    @AnimRes
    val popExitAnimation = builder.popExitAnimation
    @StyleRes
    val transitionStyle = builder.transitionStyle
    val breadCrumbTitle: String? = builder.breadCrumbTitle
    val breadCrumbShortTitle: String? = builder.breadCrumbShortTitle
    val allowStateLoss: Boolean = builder.allowStateLoss


    class Builder {
        var sharedElements: MutableList<Pair<View, String>> = mutableListOf()
        var transition: Int = FragmentTransaction.TRANSIT_NONE
        var enterAnimation: Int = 0
        var exitAnimation: Int = 0
        var transitionStyle: Int = 0
        var popEnterAnimation: Int = 0
        var popExitAnimation: Int = 0
        var breadCrumbTitle: String? = null
        var breadCrumbShortTitle: String? = null
        var allowStateLoss = false

        fun addSharedElement(element: Pair<View, String>): Builder {
            sharedElements.add(element)
            return this
        }

        fun sharedElements(elements: MutableList<Pair<View, String>>): Builder {
            sharedElements = elements
            return this
        }

        fun transition(@FragNavController.Transit transition: Int): Builder {
            this.transition = transition
            return this
        }

        fun customAnimations(@AnimRes enterAnimation: Int, @AnimRes exitAnimation: Int): Builder {
            this.enterAnimation = enterAnimation
            this.exitAnimation = exitAnimation
            return this
        }

        fun customAnimations(@AnimRes enterAnimation: Int, @AnimRes exitAnimation: Int, @AnimRes popEnterAnimation: Int, @AnimRes popExitAnimation: Int): Builder {
            this.popEnterAnimation = popEnterAnimation
            this.popExitAnimation = popExitAnimation
            return customAnimations(enterAnimation, exitAnimation)
        }


        fun transitionStyle(@StyleRes transitionStyle: Int): Builder {
            this.transitionStyle = transitionStyle
            return this
        }

        fun breadCrumbTitle(breadCrumbTitle: String): Builder {
            this.breadCrumbTitle = breadCrumbTitle
            return this
        }

        fun breadCrumbShortTitle(breadCrumbShortTitle: String): Builder {
            this.breadCrumbShortTitle = breadCrumbShortTitle
            return this
        }

        fun allowStateLoss(allow: Boolean): Builder {
            allowStateLoss = allow
            return this
        }

        fun build(): FragNavTransactionOptions {
            return FragNavTransactionOptions(this)
        }
    }

    companion object {
        fun newBuilder(): Builder {
            return Builder()
        }
    }
}

