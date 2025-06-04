package com.project.data.dao.requests

import com.project.data.tables.Requests
import com.project.domain.executeQuery
import com.project.domain.executeUpdate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RequestsDaoImpl : RequestsDao {

    override fun createRequest(request: Requests): Boolean {
        val sql = """
            INSERT INTO Requests (id_request, id_file, title, from_user, to_users)
            VALUES (?, ?, ?, ?, ?)
        """
        return executeUpdate(
            sql,
            request.id,
            request.idFile,
            request.title,
            request.fromUser,
            request.toUsers.joinToString(",") { it.trim() }
        ) > 0
    }

    override fun getRequestsByUser(login: String): List<Requests> {
        val sql = """
            SELECT r.id_request, r.id_file, r.title, r.from_user, r.to_users
            FROM Requests r
            WHERE ',' || r.to_users || ',' LIKE '%,' || ? || ',%'
        """
        val requests = mutableListOf<Requests>()
        val trimmedLogin = login.trim()

        executeQuery(sql, trimmedLogin) { resultSet ->
            while (resultSet.next()) {
                val idRequest = resultSet.getString("id_request")
                val idFile = resultSet.getString("id_file")
                val title = resultSet.getString("title")
                val fromUser = resultSet.getString("from_user")
                val toUsersRaw = resultSet.getString("to_users")

                val toUsers = toUsersRaw.split(",").map { it.trim() }

                requests.add(Requests(idRequest, idFile, title, fromUser, toUsers))
            }
        }
        return requests
    }

    override fun declineRequest(requestId: String, login: String): Boolean {
        // 1. Получаем текущий список получателей
        val sqlSelect = "SELECT to_users FROM Requests WHERE id_request = ?"
        val currentToUsers = mutableListOf<String>()

        executeQuery(sqlSelect, requestId) { resultSet ->
            if (resultSet.next()) {
                val toUsersString = resultSet.getString("to_users") ?: ""
                currentToUsers.addAll(toUsersString.split(",").map { it.trim().replace(" ", "") })
            }
        }

        // 2. Нормализуем входящий логин
        val normalizedLogin = login.replace(" ", "")

        // 3. Удаляем текущего пользователя из списка
        val updatedToUsers = currentToUsers.filter { it != normalizedLogin }

        // 4. Обновляем запись в базе
        val sqlUpdate = "UPDATE Requests SET to_users = ? WHERE id_request = ?"
        val updatedUsersString = updatedToUsers.joinToString(",")
        val isUpdated = executeUpdate(sqlUpdate, updatedUsersString, requestId) > 0

        // 5. Если получателей не осталось - удаляем заявку
        if (updatedToUsers.isEmpty()) {
            executeUpdate("DELETE FROM Requests WHERE id_request = ?", requestId)
            return true // Возвращаем true даже если запись удалена
        }

        return isUpdated
    }

    override fun confirmRequest(requestId: String, login: String): Boolean {
        // 1. Получаем данные о заявке
        val sqlSelectRequests = """
            SELECT id_file, to_users 
            FROM Requests 
            WHERE id_request = ?
        """
        var fileId: String? = null
        val currentToUsers = mutableListOf<String>()

        executeQuery(sqlSelectRequests, requestId) { resultSet ->
            if (resultSet.next()) {
                fileId = resultSet.getString("id_file")
                val toUsersRaw = resultSet.getString("to_users")
                currentToUsers.addAll(toUsersRaw.split(",").map { it.trim() })
            }
        }

        if (fileId == null) return false

        // 2. Обновляем to_users (удаляем подтвердившего пользователя)
        val updatedToUsers = currentToUsers.filter { it != login }.joinToString(",")
        val sqlUpdateRequests = """
            UPDATE Requests 
            SET to_users = ? 
            WHERE id_request = ?
        """
        val requestsUpdated = executeUpdate(sqlUpdateRequests, updatedToUsers, requestId) > 0
        if (!requestsUpdated) return false

        // 3. Удаляем заявку если не осталось получателей
        if (updatedToUsers.isEmpty()) {
            cleanupEmptyRequests()
        }

        // 4. Обновляем collaborators в файле
        val sqlSelectFiles = "SELECT collaborators FROM Files WHERE id_file = ?"
        val currentCollaborators = mutableListOf<String>()

        executeQuery(sqlSelectFiles, fileId!!) { resultSet ->
            if (resultSet.next()) {
                val collabRaw = resultSet.getString("collaborators")
                if (!collabRaw.isNullOrEmpty()) {
                    try {
                        currentCollaborators.addAll(Json.decodeFromString<List<String>>(collabRaw))
                    } catch (e: Exception) {
                        // Если JSON некорректный, считаем collaborators пустым
                    }
                }
            }
        }

        // 5. Добавляем пользователя если его еще нет
        if (login !in currentCollaborators) {
            currentCollaborators.add(login)
            val updatedCollaborators = Json.encodeToString(currentCollaborators)
            val sqlUpdateFiles = """
                UPDATE Files 
                SET collaborators = ? 
                WHERE id_file = ?
            """
            return executeUpdate(sqlUpdateFiles, updatedCollaborators, fileId!!) > 0
        }

        return true
    }

    override fun removeCollaboratorFromRequests(fileId: String, collaborator: String): Boolean {
        val sqlSelect = """
        SELECT id_request, to_users 
        FROM Requests 
        WHERE id_file = ?
    """
        val requestsToUpdate = mutableListOf<Pair<String, List<String>>>()

        executeQuery(sqlSelect, fileId) { resultSet ->
            while (resultSet.next()) {
                val requestId = resultSet.getString("id_request")
                val toUsersRaw = resultSet.getString("to_users") ?: ""
                val toUsers = toUsersRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != collaborator }
                requestsToUpdate.add(Pair(requestId, toUsers))
            }
        }

        var allSucceeded = true
        for ((requestId, updatedUsers) in requestsToUpdate) {
            if (updatedUsers.isEmpty()) {
                allSucceeded = allSucceeded && (executeUpdate("DELETE FROM Requests WHERE id_request = ?", requestId) > 0)
            } else {
                val updatedString = updatedUsers.joinToString(",")
                allSucceeded = allSucceeded && (executeUpdate("UPDATE Requests SET to_users = ? WHERE id_request = ?", updatedString, requestId) > 0)
            }
        }

        return allSucceeded
    }


    private fun cleanupEmptyRequests() {
        val sql = "DELETE FROM Requests WHERE to_users = '' OR to_users IS NULL"
        executeUpdate(sql)
    }
}
