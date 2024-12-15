package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
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

fun main() {
    val clientType = System.getProperty("t.ct") ?: throw IllegalArgumentException("test.clientType is required")
    val times = System.getProperty("t.times")?.toIntOrNull() ?: 100
    val concurrency = System.getProperty("t.cc")?.toIntOrNull() ?: 100

    println("clientType: $clientType, times: $times, concurrency: $concurrency")


    when (clientType) {
        "b1" -> {
            println("blocking one thread:  ")
            blockingOneThread(times)
            println("------------------------\n")
        }
        "bp" -> {
            println("blocking thread pool:")
            blockingThreadPool(times, concurrency)
            println("------------------------\n")
        }
        "n1" -> {
            println("nonblocking one thread: ")
            nonblockingOneThread(times, concurrency)
            println("------------------------\n")
        }
        "cb" -> {
            println("coroutine blocking:")
            coroutineBlocking(times, concurrency)
            println("------------------------\n")
        }
        "cn" -> {
            println("coroutine nonblocking:")
            coroutineNonBlocking(times, concurrency)
            println("------------------------\n")
        }
        "vt" -> {
            println("virtual thread:")
            virtualThreadBlocking(times, concurrency)
            println("------------------------\n")
        }
        else -> throw IllegalArgumentException("unknown clientType: $clientType")
    }

}

fun blockingOneThread(times: Int) {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)

        printTimeCost {
            repeat(times) {
                val a = client.getRandom()
                val b = client.getRandom()
                client.add(listOf(a, b))
            }
        }
    }
}

fun blockingThreadPool(times: Int, poolSize: Int = 10) {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)
        val executor = Executors.newFixedThreadPool(poolSize)

        printTimeCost {
            (0..times).map {
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

fun nonblockingOneThread(times: Int, concurrency: Int) {
    makeHttpAsyncClient().use { httpClient ->
        val client = NonBlockingClient(httpClient)
        val semaphore = Semaphore(concurrency)

        printTimeCost {
            (0..times).map { index ->
                semaphore.acquire()

                val a = client.getRandom()
                val b = client.getRandom()

                CompletableFuture.allOf(a, b).thenApply {
                    client.add(listOf(a.get(), b.get())).thenApply {
                        semaphore.release()
                    }
                }
            }.forEach {
                it.get().join()
            }
        }
    }
}

fun coroutineBlocking(times: Int, concurrency: Int) {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)

        // 默认的 Dispatchers.IO 大小只有64
        val myDispatcher = Dispatchers.IO.limitedParallelism(concurrency)
        printTimeCost {
            runBlocking {
                (0..times).map {
                    val a = async(myDispatcher) { client.getRandom() }
                    val b = async(myDispatcher) { client.getRandom() }
                    val c = async(myDispatcher) { client.add(listOf(a.await(), b.await())) }
                    c
                }.joinAll()
            }
        }
    }
}

fun coroutineNonBlocking(times: Int, concurrency: Int) {
    makeHttpAsyncClient().use { httpClient ->
        val client = CoroutineNonBlockingClient(NonBlockingClient(httpClient))

        val semaphore = Semaphore(concurrency)
        printTimeCost {
            runBlocking {
                (0..times).map {
                    async {
                        semaphore.acquire()
                        val a = client.getRandom()
                        val b = client.getRandom()
                        val c = client.add(listOf(a.await(), b.await())).asDeferred()
                        c.invokeOnCompletion { semaphore.release() }
                        c
                    }
                }.joinAll()
            }
        }
    }
}

fun virtualThreadBlocking(times: Int,concurrency: Int) {
    makeHttpClient().use { httpClient ->
        val client = BlockingClient(httpClient)
        val semaphore = Semaphore(concurrency)
        printTimeCost {
            val threads = (0..times).map {
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
        .setMaxConnTotal(5000)
        .setMaxConnPerRoute(5000)
        .build()
    val client = HttpAsyncClients.custom().setConnectionManager(connManger).build()
    client.start()
    return client
}


