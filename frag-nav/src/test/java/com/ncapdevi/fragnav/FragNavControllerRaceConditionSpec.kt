package com.ncapdevi.fragnav

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import com.ncapdevi.fragnav.tabhistory.UniqueTabHistoryStrategy
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val SIMULATE_FRAGMENT_MANAGER_BEHAVIOR = true

/**
 * To be able to run these tests in AS one have to install Spek Idea plugin.
 */
object FragNavControllerRaceConditionSpec : Spek({
    given("A fragment navigation controller with two tabs (A, B) starting on A tab") {
        val fakeFragmentManager = FakeFragmentManager()
        val fragmentA = getFragmentMock(fakeFragmentManager)
        val fragmentB = getFragmentMock(fakeFragmentManager)
        val rootFragmentListenerMock = getRootFragmentListenerMock(listOf(fragmentA, fragmentB))
        val fragNavController = FragNavController(fakeFragmentManager.create(), 0)
            .apply {
                rootFragmentListener = rootFragmentListenerMock
            }
        on("switching from current tab A to a new Tab B, then switching swiftly to A then back B") {
            fragNavController.initialize()
            fragNavController.switchTab(1)
            fragNavController.switchTab(0)
            waitIfNecessary()
            it("should only add 1 A and 1 B fragment") {
                verify(rootFragmentListenerMock).getRootFragment(eq(0))
                verify(rootFragmentListenerMock).getRootFragment(eq(1))
            }
            it("should call attach and detach cycle correctly") {
                fakeFragmentManager.verify(
                    listOf(
                        Add(fragmentA, "android.support.v4.app.Fragment1"),
                        Commit,
                        Detach(fragmentA),
                        Add(fragmentB, "android.support.v4.app.Fragment2"),
                        Commit,
                        Detach(fragmentB),
                        Attach(fragmentA),
                        Commit
                    )
                )
            }
        }
    }
    given("A fragment navigation controller with two three (A, B, C) with eager initialization") {
        val fakeFragmentManager = FakeFragmentManager()
        val fragmentA = getFragmentMock(fakeFragmentManager)
        val fragmentB = getFragmentMock(fakeFragmentManager)
        val fragmentC = getFragmentMock(fakeFragmentManager)
        val rootFragmentListenerMock = getRootFragmentListenerMock(listOf(fragmentA, fragmentB, fragmentC))
        val fragNavController = FragNavController(fakeFragmentManager.create(), 0)
            .apply {
                rootFragmentListener = rootFragmentListenerMock
                createEager = true
                fragmentHideStrategy = FragNavController.DETACH_ON_NAVIGATE_HIDE_ON_SWITCH
                navigationStrategy = UniqueTabHistoryStrategy(mock())
            }
        on("swiftly switching to tab B") {
            fragNavController.initialize()
            fragNavController.switchTab(1)
            waitIfNecessary()
            it("should only add 1 A 1 B and 1 C fragment") {
                verify(rootFragmentListenerMock).getRootFragment(eq(0))
                verify(rootFragmentListenerMock).getRootFragment(eq(1))
                verify(rootFragmentListenerMock).getRootFragment(eq(2))
            }
            it("should call attach and detach cycle correctly") {
                fakeFragmentManager.verify(
                    listOf(
                        Add(fragmentA, "android.support.v4.app.Fragment1"),
                        Add(fragmentB, "android.support.v4.app.Fragment2"),
                        Hide(fragmentB),
                        Add(fragmentC, "android.support.v4.app.Fragment3"),
                        Hide(fragmentC),
                        Commit,
                        Hide(fragmentA),
                        Show(fragmentB),
                        Commit
                    )
                )
            }
        }
    }
})

private fun waitIfNecessary() {
    if (SIMULATE_FRAGMENT_MANAGER_BEHAVIOR) {
        // Make sure we wait for all commits to be done
        Thread.sleep(1000)
    }
}

private fun getFragmentMock(fakeFragmentManager: FakeFragmentManager) = mock<Fragment> {
    on { isAdded } doAnswer { fakeFragmentManager.activeFragments.containsValue(this.mock) }
    on { isDetached } doAnswer { fakeFragmentManager.detachedFragments.contains(this.mock) }
}

private fun getRootFragmentListenerMock(fragments: List<Fragment>): FragNavController.RootFragmentListener {
    return mock {
        on { numberOfRootFragments } doReturn fragments.size
        on { getRootFragment(any()) } doAnswer {
            fragments[it.getArgument<Int>(0)]
        }
    }
}

class FakeFragmentManager {
    private val operations = mutableListOf<FakeFragmentOperation>()
    val activeFragments = mutableMapOf<String, Fragment>()
    val detachedFragments = mutableSetOf<Fragment>()

    fun create(): FragmentManager {
        operations.clear()
        activeFragments.clear()
        detachedFragments.clear()
        return mock<FragmentManager> {
            on { findFragmentByTag(any()) } doAnswer { activeFragments[it.getArgument<String>(0)] }
        }.apply {
            doAnswer { FakeFragmentTransaction(this@FakeFragmentManager).create() }.whenever(this).beginTransaction()
        }
    }

    fun add(tag: String, fragment: Fragment) {
        activeFragments += tag to fragment
    }

    fun remove(fragment: Fragment) {
        activeFragments.values.removeAll {
            it == fragment
        }
        detachedFragments -= fragment
    }

    fun operation(fakeFragmentOperation: FakeFragmentOperation) {
        operations += fakeFragmentOperation
    }

    fun verify(expectedOperations: List<FakeFragmentOperation>) {
        (expectedOperations == operations).shouldBeTrue()
    }

    fun attach(fragment: Fragment) {
        detachedFragments -= fragment
    }

    fun detach(fragment: Fragment) {
        detachedFragments += fragment
    }
}

class FakeFragmentTransaction(private val parent: FakeFragmentManager) {
    private val pendingActions = mutableListOf<FakeFragmentOperation>()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun create(): FragmentTransaction {
        return mock {
            on { add(any(), any(), any()) } doAnswer {
                pendingActions.add(Add(it.getArgument<Fragment>(1), it.getArgument<String>(2)))
                this.mock
            }
            on { remove(any()) } doAnswer {
                pendingActions.add(Remove(it.getArgument<Fragment>(0)))
                this.mock
            }
            on { attach(any()) } doAnswer {
                pendingActions.add(Attach(it.getArgument<Fragment>(0)))
                this.mock
            }
            on { detach(any()) } doAnswer {
                pendingActions.add(Detach(it.getArgument<Fragment>(0)))
                this.mock
            }
            on { show(any()) } doAnswer {
                pendingActions.add(Show(it.getArgument<Fragment>(0)))
                this.mock
            }
            on { hide(any()) } doAnswer {
                pendingActions.add(Hide(it.getArgument<Fragment>(0)))
                this.mock
            }
            on { commit() } doAnswer {
                commit()
                0
            }
            on { commitAllowingStateLoss() } doAnswer {
                commit()
                0
            }
        }
    }

    private fun commit() {
        executeIfNecessary {
            pendingActions.forEach {
                parent.operation(it)
                when (it) {
                    is Add -> parent.add(it.tag, it.fragment)
                    is Remove -> parent.remove(it.fragment)
                    is Attach -> parent.attach(it.fragment)
                    is Detach -> parent.detach(it.fragment)
                }
            }
            parent.operation(Commit)
            pendingActions.clear()
        }
    }

    private fun executeIfNecessary(block: () -> Unit) {
        if (SIMULATE_FRAGMENT_MANAGER_BEHAVIOR) {
            executor.apply {
                schedule({
                    block()
                    shutdown()
                }, 100, TimeUnit.MILLISECONDS)
            }
        } else {
            block()
        }
    }
}

sealed class FakeFragmentOperation
data class Add(val fragment: Fragment, val tag: String) : FakeFragmentOperation()
data class Attach(val fragment: Fragment) : FakeFragmentOperation()
data class Detach(val fragment: Fragment) : FakeFragmentOperation()
data class Remove(val fragment: Fragment) : FakeFragmentOperation()
data class Hide(val fragment: Fragment) : FakeFragmentOperation()
data class Show(val fragment: Fragment) : FakeFragmentOperation()
object Commit : FakeFragmentOperation()