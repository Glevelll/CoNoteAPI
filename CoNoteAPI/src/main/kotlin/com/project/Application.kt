package com.project

import com.project.data.dao.files.FilesDao
import com.project.data.dao.files.FilesDaoImpl
import com.project.data.dao.requests.RequestsDao
import com.project.data.dao.requests.RequestsDaoImpl
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import java.sql.Connection
import java.sql.DriverManager

val config: Config = ConfigFactory.load("application.conf")
val URL_DB: String = config.getString("database.url")
val USER_DB: String = config.getString("database.user")
val PASSWORD_DB: String = config.getString("database.password")

object ConnectionFactory {
    fun getConnection(url: String, user: String, password: String): Connection {
        return DriverManager.getConnection(url, user, password)
    }
}

fun main() {
    val filesDao: FilesDao = FilesDaoImpl()
    val requestsDao: RequestsDao = RequestsDaoImpl()

    embeddedServer(Netty, port = 8080, module = {
        module(filesDao, requestsDao)
    }).start(wait = true)
}

fun Application.module(filesDao: FilesDao, requestsDao: RequestsDao) {
    install(ContentNegotiation) {
        json()
    }

    configureRouting(filesDao, requestsDao)
}
