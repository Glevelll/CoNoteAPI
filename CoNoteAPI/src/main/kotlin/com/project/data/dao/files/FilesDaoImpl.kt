package com.project.data.dao.files

import com.project.data.tables.Files
import com.project.domain.executeQuery
import com.project.domain.executeUpdate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.ResultSet
import java.util.*

class FilesDaoImpl : FilesDao {

    override fun createFile(file: Files): Boolean {
        val sql = """
            INSERT INTO Files (id_file, title, file, who_main, collaborators)
            VALUES (?, ?, ?, ?, ?)
        """
        val fileBytes = Base64.getDecoder().decode(file.file)
        val collaboratorsJson = Json.encodeToString(file.collaborators)
        return executeUpdate(sql, file.id, file.title, fileBytes, file.whoMain, collaboratorsJson) > 0
    }

    override fun getFilesByCollaborator(login: String): List<Files> {
        val sql = "SELECT * FROM Files WHERE collaborators LIKE ?"
        return getFilesByQuery(sql, "%$login%")
    }

    override fun getFilesByMainOwner(login: String): List<Files> {
        val sql = "SELECT * FROM Files WHERE who_main = ?"
        return getFilesByQuery(sql, login)
    }

    override fun getFileById(id: String): Files? {
        val sql = "SELECT * FROM Files WHERE id_file = ?"
        var file: Files? = null
        executeQuery(sql, id) { resultSet ->
            if (resultSet.next()) {
                file = mapResultSetToFile(resultSet)
            }
        }
        return file
    }

    override fun deleteFile(id: String): Boolean {
        val sql = "DELETE FROM Files WHERE id_file = ?"
        return executeUpdate(sql, id) > 0
    }

    override fun updateFile(id: String, fileBase64: String): Boolean {
        val sql = "UPDATE Files SET file = ? WHERE id_file = ?"
        val fileBytes = Base64.getDecoder().decode(fileBase64)
        return executeUpdate(sql, fileBytes, id) > 0
    }

    override fun removeCollaborator(fileId: String, collaborator: String): Boolean {
        val file = getFileById(fileId) ?: return false
        val updatedCollaborators = file.collaborators.filter { it != collaborator }
        val updatedJson = Json.encodeToString(updatedCollaborators)
        val sql = "UPDATE Files SET collaborators = ? WHERE id_file = ?"
        return executeUpdate(sql, updatedJson, fileId) > 0
    }

    private fun getFilesByQuery(sql: String, vararg params: Any): List<Files> {
        val files = mutableListOf<Files>()
        executeQuery(sql, *params) { resultSet ->
            while (resultSet.next()) {
                files.add(mapResultSetToFile(resultSet))
            }
        }
        return files
    }

    private fun mapResultSetToFile(resultSet: ResultSet): Files {
        val id = resultSet.getString("id_file")
        val title = resultSet.getString("title")
        val fileBytes = resultSet.getBytes("file")
        val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
        val whoMain = resultSet.getString("who_main")
        val collaboratorsJson = resultSet.getString("collaborators")
        val collaborators = Json.decodeFromString<List<String>>(collaboratorsJson)
        return Files(id, title, fileBase64, whoMain, collaborators)
    }
}
