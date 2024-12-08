package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.system.measureTimeMillis

const val TIMES = 800

fun main() {
    /*
    println("blocking one thread:  ")
    blockingOneThread()
    println("------------------------\n")
     */

    /*
    println("blocking thread pool:")
    blockingThreadPool(100)
    println("------------------------\n")
     */

    println("nonblocking one thread: ")
    nonblockingOneThread()
    println("------------------------\n")

    /*
    println("coroutine blocking:")
    coroutineBlocking()
    println("------------------------\n")

    println("coroutine none blocking:")
    coroutineNonBlocking()
    println("------------------------\n")
     */

    /*
    println("virtual thread:")
    virtualThreadBlocking()
    println("------------------------\n")
     */
}

fun blockingOneThread() {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)

        printTimeCost {
            repeat(TIMES) {
                val a = client.getRandom()
                val b = client.getRandom()
                client.add(listOf(a, b))
            }
        }
    }
}

fun blockingThreadPool(poolSize: Int = 10) {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)
        val executor = Executors.newFixedThreadPool(poolSize)

        printTimeCost {
            (0..TIMES).map {
                executor.submit {
                    val a = client.getRandom()
                    val b = client.getRandom()
                    client.add(listOf(a, b))
                }
            }.forEach { it.get() }
        }

        executor.shutdown()
    }
}

fun nonblockingOneThread() {
    makeHttpAsyncClient().use { httpClient ->
        val client = NonBlockingClient(httpClient)
        val semaphore = Semaphore(100)

        printTimeCost {
            (0..TIMES).map { index ->
                try {
                    semaphore.acquire()

                    val a = client.getRandom()
                    val b = client.getRandom()

                    CompletableFuture.allOf(a, b).thenApply {
                        println("index: $index")
                        client.add(listOf(a.get(), b.get()))
                    }
                } finally {
                    semaphore.release()
                }
            }.forEach { it.get().join() }
        }
    }
}

fun coroutineBlocking() {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)

        printTimeCost {
            runBlocking {
                (0..TIMES).map {
                    val a = async(Dispatchers.IO) { client.getRandom() }
                    val b = async(Dispatchers.IO) { client.getRandom() }
                    async(Dispatchers.IO) { client.add(listOf(a.await(), b.await())) }
                }.joinAll()
            }
        }
    }
}

fun coroutineNonBlocking() {
    makeHttpAsyncClient().use { httpClient ->
        val client = CoroutineNonBlockingClient(NonBlockingClient(httpClient))

        printTimeCost {
            runBlocking(Dispatchers.Default) {
                (0..TIMES).map {
                    async {
                        val a = async { client.getRandom() }
                        val b = async { client.getRandom() }
                        async { client.add(listOf(a.await().await(), b.await().await())).await() }
                    }
                }.forEach { it.join() }
            }
        }
    }
}

fun virtualThreadBlocking() {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)
        val semaphore = Semaphore(4000)
        printTimeCost {
            val threads = (0..TIMES).map {
                Thread.ofVirtual().name("vt-$it").start {
                    try {
                        semaphore.acquire()
                        val a = client.getRandom()
                        val b = client.getRandom()
                        client.add(listOf(a, b))
                    } finally {
                        semaphore.release()
                    }
                }
            }
            threads.forEach { it.join() }
        }
    }
}

fun printTimeCost(block: () -> Unit) {
    val time = measureTimeMillis(block)
    println("Cost: $time ms")
}

fun makeHttpClient(): CloseableHttpClient {
    val connManger = PoolingHttpClientConnectionManagerBuilder.create()
    connManger.setMaxConnTotal(1000)
    connManger.setMaxConnPerRoute(1000)
    return HttpClients.custom().setConnectionManager(connManger.build()).build()
}

fun makeHttpAsyncClient(): CloseableHttpAsyncClient {
    val connManger = PoolingAsyncClientConnectionManagerBuilder.create()
        .setMaxConnTotal(500)
        .setMaxConnPerRoute(500)
        .build()
    val client = HttpAsyncClients.custom().setConnectionManager(connManger).build()
    client.start()
    return client
}


