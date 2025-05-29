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