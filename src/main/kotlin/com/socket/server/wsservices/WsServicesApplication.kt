package com.socket.server.wsservices

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.file.dsl.Files
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

@SpringBootApplication
class WsServicesApplication

fun main(args: Array<String>) {
    runApplication<WsServicesApplication>(*args)
}

@Configuration
class WebSocketConfiguration{

    @Bean
    fun incomingFileFlow(@Value("C://file/example}") f : File) =
            IntegrationFlows
                    .from(Files.inboundAdapter(f)
                            .autoCreateDirectory(true),
            {p -> p.poller({pm ->
                pm.fixedRate(1000)})})
            .channel(incomingFileChannel())
                    .get()


    @Bean
    fun incomingFileChannel ()  = PublishSubscribeChannel()


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
        val om = ObjectMapper()
        val  connections  = ConcurrentHashMap<String, MessageHandler>()
        class  ForwardingMessageHandler(val session: WebSocketSession, val sink: FluxSink<WebSocketMessage>):MessageHandler{
        private val sessionId = session.id
        override fun handleMessage(message: Message<*>) {
            val payload = message.payload as File
            val fe  = FileEvent(sessionId = sessionId, path = payload.absolutePath)
            val str = om.writeValueAsString(fe);
            val tm = session.textMessage(str);
            sink.next(tm)
        }
    }
        return WebSocketHandler { session ->

            val publisher = Flux.create(Consumer<FluxSink<WebSocketMessage>>{sink ->
                connections[session.id] = ForwardingMessageHandler(session, sink )
                incomingFileChannel().subscribe(connections[session.id])
                                })
                    .doFinally{
                            incomingFileChannel().unsubscribe(connections[session.id])
                        connections.remove(session.id);
                    }
            session.send(publisher)

        }

    }
}
data class FileEvent (val sessionId: String , val path: String)