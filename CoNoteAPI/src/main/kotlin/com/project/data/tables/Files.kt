package com.project.data.tables

import kotlinx.serialization.Serializable

@Serializable
data class Files(
    val idFile: String,
    val title: String,
    val file: String,
    val whoMain: String,
    val collaborators: Map<String, String>
)