package com.project.data.dao.files

import com.project.data.tables.Files

interface FilesDao {
    fun createFile(file: Files): Boolean
    fun getFilesByUser(login: String): List<Files>
    fun deleteFile(id: String): Boolean
    fun updateFile(id: String, file: String): Boolean
    fun getFileById(id: String): Files?
//    fun getFilesByAdmin(login: String): List<Files>
    fun removeCollaborator(fileId: String, collaborator: String): Boolean
}