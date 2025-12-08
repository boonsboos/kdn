package nl.boonsboos.kdn.abstractions

import java.io.ByteArrayOutputStream
import java.util.UUID

interface PictureService {
    suspend fun save(imageDataStream: ByteArrayOutputStream): UUID?
    suspend fun getImage(id: UUID): ByteArray?
}