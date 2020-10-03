package com.example.demo

import org.h2.tools.Server
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Profile("local")
@Component
class H2Server {

    lateinit var server: Server

    @PostConstruct
    fun startH2TcpServer(): Server =
            Server.createTcpServer("-tcp", "-tcpPort", "15435", "-tcpAllowOthers").start()
                    .also { server = it }

    @PreDestroy
    fun stopH2TcpServer() = server.stop()
}