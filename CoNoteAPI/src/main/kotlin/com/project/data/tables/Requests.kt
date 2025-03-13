package com.project.data.tables

import kotlinx.serialization.Serializable

@Serializable
data class Requests(
    val idRequest: String,
    val file: String,
    val fromUser: String,
    val toUsers: List<String>
)