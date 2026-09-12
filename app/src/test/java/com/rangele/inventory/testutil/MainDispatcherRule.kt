package com.rangele.inventory.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Points [Dispatchers.Main] at an unconfined test dispatcher for the duration of a test, so
 * viewModelScope coroutines run eagerly. [dispatcher] is exposed so tests can pass it to
 * `runTest(dispatcher)` — sharing one scheduler between the test body and viewModelScope is
 * required for `stateIn`'s WhileSubscribed sharing coroutine (launched on Main) to actually
 * produce values for a collector started from the test's own coroutine scope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
