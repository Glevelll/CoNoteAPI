package com.project.domain

import com.project.ConnectionFactory
import com.project.PASSWORD_DB
import com.project.URL_DB
import com.project.USER_DB
import java.sql.ResultSet

fun executeUpdate(sql: String, vararg params: Any): Int {
    val connection = ConnectionFactory.getConnection(URL_DB, USER_DB, PASSWORD_DB)
    return connection.use { conn ->
        val preparedStatement = conn.prepareStatement(sql)
        params.forEachIndexed { index, param ->
            when (param) {
                is String -> preparedStatement.setString(index + 1, param)
                is ByteArray -> preparedStatement.setBytes(index + 1, param)
                else -> throw IllegalArgumentException("Unsupported parameter type: ${param::class.java}")
            }
        }
        preparedStatement.executeUpdate()
    }
}

fun executeQuery(sql: String, vararg params: Any, block: (ResultSet) -> Unit) {
    val connection = ConnectionFactory.getConnection(URL_DB, USER_DB, PASSWORD_DB)
    connection.use { conn ->
        val preparedStatement = conn.prepareStatement(sql)
        params.forEachIndexed { index, param ->
            when (param) {
                is String -> preparedStatement.setString(index + 1, param)
                else -> throw IllegalArgumentException("Unsupported parameter type: ${param::class.java}")
            }
        }
        val resultSet = preparedStatement.executeQuery()
        block(resultSet)
    }
}