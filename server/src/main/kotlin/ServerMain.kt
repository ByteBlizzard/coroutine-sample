package org.example

import io.vertx.core.Vertx
import io.vertx.core.http.Http2Settings
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import kotlin.random.Random

fun main(args: Array<String>) {
    val lag = args.getOrNull(0)?.toLongOrNull() ?: 30L
    val port = args.getOrNull(1)?.toIntOrNull() ?: 8080
    val vertx = Vertx.vertx()

    val router = Router.router(vertx)
    router.post("/add").handler(BodyHandler.create()).handler{ ctx ->
        val numbersString = ctx.body().asString()

        val result = numbersString.split(",").mapNotNull { it.toIntOrNull() }.sum()

        val resp = ctx.response()
        resp.putHeader("content-type", "text/plain")
        vertx.setTimer(lag) {
            resp.end(result.toString())
        }
    }

    val random = Random(System.currentTimeMillis())
    router.get("/random").handler{ ctx ->
        vertx.setTimer(lag) {
            ctx.response().end(random.nextInt(ctx.queryParam("top").getOrNull(0)?.toIntOrNull() ?: Integer.MAX_VALUE).toString())
        }
    }

    val httpServer = vertx.createHttpServer(
        HttpServerOptions()
            .setInitialSettings(Http2Settings().setMaxConcurrentStreams(1000_0000))
    )
    httpServer.requestHandler(router).listen(port)
        .onSuccess {
            println("Server started on port $port, lag: $lag")
        }
}