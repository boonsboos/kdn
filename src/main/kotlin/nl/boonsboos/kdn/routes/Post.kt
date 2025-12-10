package nl.boonsboos.kdn.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import io.ktor.utils.io.jvm.javaio.*
import nl.boonsboos.kdn.abstractions.PictureService
import nl.boonsboos.kdn.services.PictureServiceImpl
import org.koin.ktor.ext.inject
import java.io.ByteArrayOutputStream

fun Route.postRoutes(pictureService: PictureService) {
    post {
        // hard 32 megabyte file limit
        val multipartData: MultiPartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 32)

        // only accept one file at a time.
        val data = multipartData.readPart()
        if (data == null) {
            call.application.environment.log.error("Parsing error with multipart data? {}", call.request)
            call.respond(HttpStatusCode.InternalServerError)
        }

        if (data !is PartData.FileItem) {
            call.application.environment.log.error("Not a file! {}", data)
            return@post call.respond(HttpStatusCode.BadRequest)
        }

        try {
            val bytes = ByteArrayOutputStream()

            data.provider().copyTo(bytes)
            data.dispose()
            val result = pictureService.save(bytes)

            call.response.header("Location", result.toString())
            call.respond(HttpStatusCode.OK)
        } catch (exception: Exception) {
            call.application.environment.log.error("Failed to save image: ${exception.message}", exception)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    post("bulk") {
        // hard 32 megabyte file limit
        val multipartData: MultiPartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 32)

        try {
            val pictureIds = mutableListOf<String>()

            multipartData.forEachPart { data ->
                if (data !is PartData.FileItem) {
                    throw IllegalArgumentException("Form contains not only files")
                }

                val bytes = ByteArrayOutputStream()

                data.provider().copyTo(bytes)

                val result = pictureService.save(bytes) ?: IllegalArgumentException("Failed to save image")
                pictureIds.add(result.toString())
                data.dispose()
            }

            call.respond(HttpStatusCode.OK, pictureIds)
        } catch (exception: IllegalArgumentException) {
            call.application.environment.log.error(exception)
            call.respond(HttpStatusCode.BadRequest)
        } catch (exception: Exception) {
            call.application.environment.log.error("Failed to save bulk", exception)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}