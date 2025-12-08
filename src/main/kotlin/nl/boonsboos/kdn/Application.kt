package nl.boonsboos.kdn

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import nl.boonsboos.kdn.abstractions.PictureService
import nl.boonsboos.kdn.data.PictureDatabaseConnector
import nl.boonsboos.kdn.routes.getRoutes
import nl.boonsboos.kdn.routes.postRoutes
import nl.boonsboos.kdn.services.PictureServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import kotlin.getValue

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(CachingHeaders) {
        options { call, outgoingContent ->
            val cacheDuration = TimeUnit.SECONDS.convert(7, TimeUnit.DAYS).toInt()

            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Image.WEBP -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cacheDuration))
                ContentType.Image.PNG -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cacheDuration))
                ContentType.Image.JPEG -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = cacheDuration))
                else -> null
            }
        }
    }
    install(Compression)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(Koin) {
        slf4jLogger()
        modules(module {
            singleOf(::PictureDatabaseConnector)
            singleOf(::PictureServiceImpl) { bind<PictureService>() }
        })
    }
    install(ContentNegotiation) {
        json()
    }

    routing {
        route("/images") {
            val pictureService by inject<PictureServiceImpl>()

            postRoutes(pictureService)
            getRoutes(pictureService)
        }
    }
}
