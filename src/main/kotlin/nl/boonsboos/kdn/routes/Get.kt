package nl.boonsboos.kdn.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import nl.boonsboos.kdn.abstractions.PictureService
import java.util.UUID

fun Route.getRoutes(pictureService: PictureService) {
    get("{imageId}") {
        val imageId = call.parameters["imageId"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        try {
            val id = UUID.fromString(imageId)

            call.respondBytes(ContentType.Image.JPEG, HttpStatusCode.OK) {
                pictureService.getImage(id) ?: throw IllegalStateException("No image with id $id")
            }
        } catch (e: IllegalArgumentException) {
            call.application.environment.log.error("Unformatted ID $imageId", e)
            call.respond(HttpStatusCode.BadRequest)
        } catch (e: IllegalStateException) {
            call.application.environment.log.error("Unknown image $imageId", e)
            call.respond(HttpStatusCode.NotFound)
        }
    }

}
