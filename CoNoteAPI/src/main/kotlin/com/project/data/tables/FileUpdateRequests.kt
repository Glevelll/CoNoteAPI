package com.project.data.tables

import kotlinx.serialization.Serializable

@Serializable
data class FileUpdateRequests(
    val title: String? = null,
    val file: String? = null,
    val collaborators: Map<String, String>? = null,
    val updatedBy: String? = null // Логин пользователя, который обновил файл
)