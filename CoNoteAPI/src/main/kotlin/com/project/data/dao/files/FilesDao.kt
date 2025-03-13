package com.project.data.dao.files

import com.project.data.tables.FileUpdateRequests
import com.project.data.tables.Files

interface FilesDao {
    fun createFile(file: Files): Boolean
    fun getFilesByUser(login: String): List<Files>
    fun deleteFile(id: String): Boolean
    fun updateFile(id: String, updateRequest: FileUpdateRequests): Boolean
}