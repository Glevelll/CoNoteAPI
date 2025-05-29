package com.project.data.dao.files

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
        return executeUpdate(sql, file.id, file.title, fileBytes, file.whoMain, collaboratorsJson) > 0
    }

    override fun getFilesByUser(login: String): List<Files> {
        val sql = """
        SELECT * FROM Files 
        WHERE who_main = ? OR collaborators LIKE ?
    """
        val files = mutableListOf<Files>()
        executeQuery(sql, login, "%$login%") { resultSet ->
            while (resultSet.next()) {
                val id = resultSet.getString("id_file")
                val title = resultSet.getString("title")
                val fileBytes = resultSet.getBytes("file")
                val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
                val whoMain = resultSet.getString("who_main")
                val collaboratorsJson = resultSet.getString("collaborators")
                val collaborators = Json.decodeFromString<List<String>>(collaboratorsJson)
                files.add(Files(id, title, fileBase64, whoMain, collaborators))
            }
        }
        return files
    }

    override fun getFileById(id: String): Files? {
        val sql = "SELECT * FROM Files WHERE id_file = ?"
        var file: Files? = null
        executeQuery(sql, id) { resultSet ->
            if (resultSet.next()) {
                val idFile = resultSet.getString("id_file")
                val title = resultSet.getString("title")
                val fileBytes = resultSet.getBytes("file")
                val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
                val whoMain = resultSet.getString("who_main")
                val collaboratorsJson = resultSet.getString("collaborators")
                val collaborators = Json.decodeFromString<List<String>>(collaboratorsJson)
                file = Files(idFile, title, fileBase64, whoMain, collaborators)
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
}