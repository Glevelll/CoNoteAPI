package com.project.data.tables

import kotlinx.serialization.Serializable

@Serializable
data class Requests(
    val id: String,
    val idFile: String,
    val title: String,
    val fromUser: String,
    val toUsers: List<String>
)