package com.example.data.repository

import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

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
