package com.ncapdevi.fragnav

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.ncapdevi.fragnav.tabhistory.UniqueTabHistoryStrategy
import com.nhaarman.mockitokotlin2.*
import org.amshove.kluent.shouldBeTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val SIMULATE_FRAGMENT_MANAGER_BEHAVIOR = true

/**
 * To be able to run these tests in AS one have to install Spek 2 Idea plugin.
 * https://plugins.jetbrains.com/plugin/10915-spek-framework
 */
object FragNavControllerRaceConditionSpec : Spek({
    Feature("FragNavController") {
        Scenario("A fragment navigation controller with two tabs (A, B) starting on A tab") {
            val fakeFragmentManager = FakeFragmentManager()
            val fragmentA = getFragmentMock(fakeFragmentManager)
            val fragmentB = getFragmentMock(fakeFragmentManager)
            val rootFragmentListenerMock = getRootFragmentListenerMock(listOf(fragmentA, fragmentB))
            val fragNavController = FragNavController(fakeFragmentManager.create(), 0)
                .apply {
                    rootFragmentListener = rootFragmentListenerMock
                }
            When("switching from current tab A to a new Tab B, then switching swiftly to A then back B") {
                fragNavController.initialize()
                fragNavController.switchTab(1)
                fragNavController.switchTab(0)
                waitIfNecessary()
            }
            Then("should only add 1 A and 1 B fragment") {
                verify(rootFragmentListenerMock).getRootFragment(eq(0))
                verify(rootFragmentListenerMock).getRootFragment(eq(1))
            }
            And("should call attach and detach cycle correctly") {
                fakeFragmentManager.verify(
                    listOf(
                        Add(fragmentA, "androidx.fragment.app.Fragment1"),
                        Commit,
                        Detach(fragmentA),
                        Add(fragmentB, "androidx.fragment.app.Fragment2"),
                        Commit,
                        Detach(fragmentB),
                        Attach(fragmentA),
                        Commit
                    )
                )
            }
        }
        Scenario("A fragment navigation controller with two three (A, B, C) with eager initialization") {
            val fakeFragmentManager = FakeFragmentManager()
            val fragmentA = getFragmentMock(fakeFragmentManager)
            val fragmentB = getFragmentMock(fakeFragmentManager)
            val fragmentC = getFragmentMock(fakeFragmentManager)
            val rootFragmentListenerMock =
                getRootFragmentListenerMock(listOf(fragmentA, fragmentB, fragmentC))
            val fragNavController = FragNavController(fakeFragmentManager.create(), 0)
                .apply {
                    rootFragmentListener = rootFragmentListenerMock
                    createEager = true
                    fragmentHideStrategy = FragNavController.DETACH_ON_NAVIGATE_HIDE_ON_SWITCH
                    navigationStrategy = UniqueTabHistoryStrategy(mock())
                }
            When("swiftly switching to tab B") {
                fragNavController.initialize()
                fragNavController.switchTab(1)
                waitIfNecessary()
            }
            Then("should only add 1 A 1 B and 1 C fragment") {
                verify(rootFragmentListenerMock).getRootFragment(eq(0))
                verify(rootFragmentListenerMock).getRootFragment(eq(1))
                verify(rootFragmentListenerMock).getRootFragment(eq(2))
            }
            And("should call attach and detach cycle correctly") {
                fakeFragmentManager.verify(
                    listOf(
                        Add(fragmentA, "androidx.fragment.app.Fragment1"),
                        Add(fragmentB, "androidx.fragment.app.Fragment2"),
                        Hide(fragmentB),
                        Add(fragmentC, "androidx.fragment.app.Fragment3"),
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
        on { getRootFragment(any()) } doAnswer { invocationOnMock ->
            fragments[invocationOnMock.getArgument<Int>(0)]
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
            on { findFragmentByTag(any()) } doAnswer { invocationOnMock -> activeFragments[invocationOnMock.getArgument<String>(0)] }
        }.apply {
            doAnswer { FakeFragmentTransaction(this@FakeFragmentManager).create() }.whenever(this)
                .beginTransaction()
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
            on { add(any(), any(), any()) } doAnswer { invocationOnMock ->
                pendingActions.add(Add(invocationOnMock.getArgument<Fragment>(1), invocationOnMock.getArgument<String>(2)))
                this.mock
            }
            on { remove(any()) } doAnswer { invocationOnMock ->
                pendingActions.add(Remove(invocationOnMock.getArgument<Fragment>(0)))
                this.mock
            }
            on { attach(any()) } doAnswer { invocationOnMock ->
                pendingActions.add(Attach(invocationOnMock.getArgument<Fragment>(0)))
                this.mock
            }
            on { detach(any()) } doAnswer { invocationOnMock ->
                pendingActions.add(Detach(invocationOnMock.getArgument<Fragment>(0)))
                this.mock
            }
            on { show(any()) } doAnswer { invocationOnMock ->
                pendingActions.add(Show(invocationOnMock.getArgument<Fragment>(0)))
                this.mock
            }
            on { hide(any()) } doAnswer { invocationOnMock ->
                pendingActions.add(Hide(invocationOnMock.getArgument<Fragment>(0)))
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