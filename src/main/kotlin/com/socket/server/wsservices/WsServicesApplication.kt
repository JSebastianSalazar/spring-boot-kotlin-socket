package com.socket.server.wsservices

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.util.function.Consumer

@SpringBootApplication
class WsServicesApplication

fun main(args: Array<String>) {
    runApplication<WsServicesApplication>(*args)
}

@Configuration
class WebSocketConfiguration{
    @Bean
    fun wsha() = WebSocketHandlerAdapter()

    @Bean
    fun hadlerMapping(): HandlerMapping {
        val suhm = SimpleUrlHandlerMapping()
        suhm.order = 10
        suhm.urlMap = mapOf("/ws/files" to wsh())
        return suhm
    }

    @Bean fun wsh (): WebSocketHandler{
        return WebSocketHandler { session ->
            val om = ObjectMapper()
            val publisher = Flux.generate(Consumer<SynchronousSink<FileEvent>>{
                sink -> sink.next(FileEvent("${System.currentTimeMillis()}", "/a/b/c"))
                                })
                    .map { om.writeValueAsString(it) }
                    .map { session.textMessage(it) }
                    .delayElements(Duration.ofSeconds(1L))
            session.send(publisher)

        }

    }
}
data class FileEvent (val sessionId: String , val path: String)