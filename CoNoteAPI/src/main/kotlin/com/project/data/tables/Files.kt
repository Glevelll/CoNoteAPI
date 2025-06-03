package com.project.data.tables

import kotlinx.serialization.Serializable

@Serializable
data class Files(
    val id: String,
    val title: String,
    val file: String,
    val whoMain: String,
    val collaborators: List<String>
)

@Serializable
data class Requests(
    val id: String,
    val idFile: String,
    val title: String,
    val fromUser: String,
    val toUsers: List<String>
)

@Serializable
data class FileUpdateRequests(
    val file: String
)