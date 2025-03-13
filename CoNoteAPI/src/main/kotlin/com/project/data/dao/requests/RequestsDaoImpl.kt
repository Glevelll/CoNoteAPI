package com.project.data.dao.requests

import com.project.data.tables.Requests
import com.project.domain.executeQuery
import com.project.domain.executeUpdate

class RequestsDaoImpl : RequestsDao {

    override fun createRequest(request: Requests): Boolean {
        val sql = """
            INSERT INTO Requests (id_request, file, from_user, to_users)
            VALUES (?, ?, ?, ?)
        """
        return executeUpdate(sql, request.idRequest, request.file, request.fromUser, request.toUsers.joinToString(",")) > 0
    }

    override fun getRequestsByUser(login: String): List<Requests> {
        val sql = """
            SELECT r.id_request, f.title, r.from_user, r.to_users
            FROM Requests r
            JOIN Files f ON r.file = f.id_file
            WHERE r.to_users LIKE ?
        """
        val requests = mutableListOf<Requests>()
        executeQuery(sql, "%$login%") { resultSet ->
            while (resultSet.next()) {
                val idRequest = resultSet.getString("id_request")
                val fileName = resultSet.getString("title")
                val fromUser = resultSet.getString("from_user")
                val toUsers = resultSet.getString("to_users").split(",")
                requests.add(Requests(idRequest, fileName, fromUser, toUsers))
            }
        }
        return requests
    }

    override fun declineRequest(requestId: String, login: String): Boolean {
        val sqlSelect = "SELECT to_users FROM Requests WHERE id_request = ?"
        val currentToUsers = mutableListOf<String>()
        executeQuery(sqlSelect, requestId) { resultSet ->
            if (resultSet.next()) {
                currentToUsers.addAll(resultSet.getString("to_users").split(","))
            }
        }

        val updatedToUsers = currentToUsers.filter { it != login }.joinToString(",")
        val sqlUpdate = "UPDATE Requests SET to_users = ? WHERE id_request = ?"
        val isUpdated = executeUpdate(sqlUpdate, updatedToUsers, requestId) > 0

        // Проверяем, стал ли to_users пустым, и удаляем запись, если это так
        if (updatedToUsers.isEmpty()) {
            cleanupEmptyRequests()
        }

        return isUpdated
    }

    override fun confirmRequest(requestId: String, login: String): Boolean {
        val sqlSelectRequests = "SELECT file, to_users FROM Requests WHERE id_request = ?"
        var fileId: String? = null
        val currentToUsers = mutableListOf<String>()
        executeQuery(sqlSelectRequests, requestId) { resultSet ->
            if (resultSet.next()) {
                fileId = resultSet.getString("file")
                currentToUsers.addAll(resultSet.getString("to_users").split(","))
            }
        }

        if (fileId == null) return false // Если заявка не найдена

        val updatedToUsers = currentToUsers.filter { it != login }.joinToString(",")
        val sqlUpdateRequests = "UPDATE Requests SET to_users = ? WHERE id_request = ?"
        val requestsUpdated = executeUpdate(sqlUpdateRequests, updatedToUsers, requestId) > 0

        if (!requestsUpdated) return false

        // Проверяем, стал ли to_users пустым, и удаляем запись, если это так
        if (updatedToUsers.isEmpty()) {
            cleanupEmptyRequests()
        }

        // Добавляем логин пользователя в collaborators в таблице Files
        val sqlSelectFiles = "SELECT collaborators FROM Files WHERE id_file = ?"
        val currentCollaborators = mutableListOf<String>()
        executeQuery(sqlSelectFiles, fileId!!) { resultSet ->
            if (resultSet.next()) {
                currentCollaborators.addAll(resultSet.getString("collaborators").split(","))
            }
        }

        if (login !in currentCollaborators) {
            currentCollaborators.add(login)
            val updatedCollaborators = currentCollaborators.joinToString(",")
            val sqlUpdateFiles = "UPDATE Files SET collaborators = ? WHERE id_file = ?"
            return executeUpdate(sqlUpdateFiles, updatedCollaborators, fileId!!) > 0
        }

        return true
    }

    private fun cleanupEmptyRequests() {
        val sql = "DELETE FROM Requests WHERE to_users = '' OR to_users IS NULL"
        executeUpdate(sql)
    }
}