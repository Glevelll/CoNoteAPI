package com.project

import com.project.data.dao.files.FilesDao
import com.project.data.dao.requests.RequestsDao
import com.project.data.tables.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(filesDao: FilesDao, requestsDao: RequestsDao) {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/requests") {
            val request = call.receive<Requests>()
            if (requestsDao.createRequest(request)) {
                call.respond(HttpStatusCode.Created, "Request created")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create request")
            }
        }

        get("/requests/{login}") {
            val login = call.parameters["login"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Login is required")
            val requests = requestsDao.getRequestsByUser(login)
            call.respond(HttpStatusCode.OK, requests)
        }

        delete("/requests/{requestId}/decline") {
            val requestId = call.parameters["requestId"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Request ID is required")
            val login = call.request.queryParameters["login"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Login is required")

            if (requestsDao.declineRequest(requestId, login)) {
                call.respond(HttpStatusCode.OK, "Request declined")
            } else {
                call.respond(HttpStatusCode.NotFound, "Request not found or user not in to_users")
            }
        }

        // Новый маршрут для подтверждения заявки
        post("/requests/{requestId}/confirm") {
            val requestId = call.parameters["requestId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Request ID is required")
            val login = call.request.queryParameters["login"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Login is required")

            if (requestsDao.confirmRequest(requestId, login)) {
                call.respond(HttpStatusCode.OK, "Request confirmed")
            } else {
                call.respond(HttpStatusCode.NotFound, "Request not found or user not in to_users")
            }
        }

        post("/files") {
            val file = call.receive<Files>()
            if (filesDao.createFile(file)) {
                call.respond(HttpStatusCode.Created, "File created")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create file")
            }
        }

        get("/files/{login}") {
            val login = call.parameters["login"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Login is required")
            val files = filesDao.getFilesByUser(login)
            call.respond(HttpStatusCode.OK, files)
        }

        delete("/files/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "File ID is required")
            if (filesDao.deleteFile(id)) {
                call.respond(HttpStatusCode.OK, "File deleted")
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found")
            }
        }

        patch("/files/{id}") {
            val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest, "File ID is required")
            val updateRequest = call.receive<FileUpdateRequests>()
            if (filesDao.updateFile(id, updateRequest)) {
                call.respond(HttpStatusCode.OK, "File updated")
            } else {
                call.respond(HttpStatusCode.NotFound, "File not found")
            }
        }
    }
}