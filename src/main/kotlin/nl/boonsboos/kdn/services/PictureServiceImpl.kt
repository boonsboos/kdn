package nl.boonsboos.kdn.services

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import nl.boonsboos.kdn.abstractions.PictureService
import nl.boonsboos.kdn.data.PictureDatabaseConnector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

class PictureServiceImpl(private val db: PictureDatabaseConnector) : PictureService {

    private val compressionLevel: Int = 75

    override suspend fun save(imageDataStream: ByteArrayOutputStream): UUID = coroutineScope {
        val writer = JpegWriter().withCompression(compressionLevel)

        val image = ImmutableImage.loader().fromBytes(imageDataStream.toByteArray())

        async {
            db.saveImage(ByteArrayInputStream(image.bytes(writer)))
        }.await()
    }

    override suspend fun getImage(id: UUID): ByteArray? = coroutineScope {
        async {
            db.getImageData(id)
        }.await()
    }
}