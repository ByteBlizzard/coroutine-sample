package org.example

import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder
import java.lang.Exception
import java.util.concurrent.CompletableFuture

val server = System.getProperty("t.server") ?: throw IllegalArgumentException("test.sever is required")

class BlockingClient(
    private val client: CloseableHttpClient
) {
    fun getRandom(top: Int = 1000): Int {
        val req = ClassicRequestBuilder.get("http://${server}/random?top=$top").build()
        return client.execute(req) { resp ->
            EntityUtils.toString(resp.entity).toIntOrNull() ?: 0
        }
    }

    fun add(numbers: List<Int>): Int {
        val body = numbers.joinToString(",")
        val req = ClassicRequestBuilder.post("http://${server}/add")
            .setEntity(body)
            .build()
        return client.execute(req) {
            EntityUtils.toString(it.entity).toIntOrNull() ?: 0
        }
    }
}

class NonBlockingClient(
    private val client: CloseableHttpAsyncClient
) {
    fun getRandom(top: Int = 1000): CompletableFuture<Int> {
        val result = CompletableFuture<Int>()

        val req = SimpleRequestBuilder.get("http://${server}/random?top=$top").build()
        client.execute(
            SimpleRequestProducer.create(req),
            SimpleResponseConsumer.create(),
            object: FutureCallback<SimpleHttpResponse> {
                override fun completed(resp: SimpleHttpResponse) {
                    result.complete(resp.bodyText.toIntOrNull() ?: 0)
                }

                override fun failed(ex: Exception) {
                    result.completeExceptionally(ex)
                }

                override fun cancelled() {
                    result.cancel(true)
                }
            }
        )

        return result
    }

    fun add(numbers: List<Int>): CompletableFuture<Int> {
        val result = CompletableFuture<Int>()

        val body = numbers.joinToString(",")
        val req = SimpleRequestBuilder.post("http://${server}/add")
            .setBody(body, ContentType.TEXT_PLAIN)
            .build()
        client.execute(
            SimpleRequestProducer.create(req),
            SimpleResponseConsumer.create(),
            object: FutureCallback<SimpleHttpResponse> {
                override fun completed(resp: SimpleHttpResponse) {
                    result.complete(resp.bodyText.toIntOrNull() ?: 0)
                }

                override fun failed(ex: Exception) {
                    result.completeExceptionally(ex)
                }

                override fun cancelled() {
                    result.cancel(true)
                }
            }
        )

        return result
    }
}

class CoroutineNonBlockingClient(
    private val client: NonBlockingClient
) {
    fun getRandom(top: Int = 1000): CompletableFuture<Int> {
        return client.getRandom(top)
    }

    fun add(numbers: List<Int>): CompletableFuture<Int> {
        return client.add(numbers)
    }
}
