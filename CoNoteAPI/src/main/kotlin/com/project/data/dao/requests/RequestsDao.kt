package com.project.data.dao.requests

import com.project.data.tables.Requests

interface RequestsDao {
    fun createRequest(request: Requests): Boolean
    fun getRequestsByUser(login: String): List<Requests>
    fun declineRequest(requestId: String, login: String): Boolean
    fun confirmRequest(requestId: String, login: String): Boolean
    fun removeCollaboratorFromRequests(fileId: String, collaborator: String): Boolean
}