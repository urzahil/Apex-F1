package com.example.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CancellationException

class ApiCallTest {
    @Test
    fun apiCall_rethrowsCancellationException() = runTest {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                apiCall<String> { throw CancellationException("cancelled") }
            }
        }
    }
}
