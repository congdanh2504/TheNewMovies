package com.practice.thenewmovies.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Records the classes and methods worth AOT-compiling at install time.
 *
 * Run with `./gradlew :app:generateBaselineProfile` on a rooted emulator or a userdebug device;
 * the result lands in `app/src/main/baseline-prof.txt` and is packaged automatically.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        // Only the startup path is journeyed here. The signed-out shell is what every cold start
        // renders first, so it is the part that always pays for AOT compilation; adding the
        // signed-in tabs would need credentials this profile run does not have.
        device.waitForIdle()
    }
}
