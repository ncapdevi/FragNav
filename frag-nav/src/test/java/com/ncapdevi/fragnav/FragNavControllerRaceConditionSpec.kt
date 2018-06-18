package com.ncapdevi.fragnav

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

/**
 * To be able to run these tests in AS one have to install Spek Idea plugin.
 */
object FragNavControllerRaceConditionSpec : Spek({
    given("A fragment navigation controller with two tabs (A, B) starting on A tab") {
        on("switching from current tab A to a new Tab B, then switching swiftly to A then back B") {
            it("should only add 1 B fragment") {

            }
            it("should call attach and detach cycle correctly") {
            }
        }
    }
    given("A fragment navigation controller with two three (A, B, C) with eager initialization") {
        on("swiftly switching to tab B") {
            it("should only add 1 B fragment") {

            }
            it("should call attach and detach cycle correctly") {

            }
        }
    }
})