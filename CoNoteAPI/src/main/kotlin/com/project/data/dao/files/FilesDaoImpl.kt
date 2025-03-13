package com.project.data.dao.files

import com.project.data.tables.FileUpdateRequests
import com.project.data.tables.Files
import com.project.domain.executeQuery
import com.project.domain.executeUpdate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class FilesDaoImpl : FilesDao {
    override fun createFile(file: Files): Boolean {
        val sql = """
        INSERT INTO Files (id_file, title, file, who_main, collaborators)
        VALUES (?, ?, ?, ?, ?)
    """
        val fileBytes = Base64.getDecoder().decode(file.file)
        val collaboratorsJson = Json.encodeToString(file.collaborators)
        return executeUpdate(sql, file.idFile, file.title, fileBytes, file.whoMain, collaboratorsJson) > 0
    }

    override fun getFilesByUser(login: String): List<Files> {
        val sql = """
        SELECT * FROM Files 
        WHERE who_main = ? OR collaborators LIKE ?
    """
        val files = mutableListOf<Files>()
        executeQuery(sql, login, "%$login%") { resultSet ->
            while (resultSet.next()) {
                val idFile = resultSet.getString("id_file")
                val title = resultSet.getString("title")
                val fileBytes = resultSet.getBytes("file")
                val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
                val whoMain = resultSet.getString("who_main")
                val collaboratorsJson = resultSet.getString("collaborators")
                val collaborators = Json.decodeFromString<Map<String, String>>(collaboratorsJson)
                files.add(Files(idFile, title, fileBase64, whoMain, collaborators))
            }
        }
        return files
    }

    override fun deleteFile(id: String): Boolean {
        val sql = "DELETE FROM Files WHERE id_file = ?"
        return executeUpdate(sql, id) > 0
    }

    override fun updateFile(id: String, updateRequest: FileUpdateRequests): Boolean {
        val sqlSelect = "SELECT collaborators FROM Files WHERE id_file = ?"
        val currentCollaborators = mutableMapOf<String, String>()
        executeQuery(sqlSelect, id) { resultSet ->
            if (resultSet.next()) {
                val collaboratorsJson = resultSet.getString("collaborators")
                currentCollaborators.putAll(Json.decodeFromString(collaboratorsJson))
            }
        }

        // Обновляем collaborators для текущего пользователя
        val updatedBy = updateRequest.updatedBy ?: "unknown"
        val currentTime = java.time.LocalDateTime.now().toString() // Серверное время
        currentCollaborators[updatedBy] = currentTime

        val sql = buildString {
            append("UPDATE Files SET ")
            if (updateRequest.title != null) append("title = ?, ")
            if (updateRequest.file != null) append("file = ?, ")
            append("collaborators = ? ")
            append("WHERE id_file = ?")
        }
        val params = mutableListOf<Any>()
        if (updateRequest.title != null) params.add(updateRequest.title)
        if (updateRequest.file != null) params.add(Base64.getDecoder().decode(updateRequest.file))
        params.add(Json.encodeToString(currentCollaborators))
        params.add(id)
        return executeUpdate(sql, *params.toTypedArray()) > 0
    }
}