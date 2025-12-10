package nl.boonsboos.kdn.data;

import org.slf4j.LoggerFactory
import org.sqlite.SQLiteException
import java.io.ByteArrayInputStream
import java.io.File
import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

class PictureDatabaseConnector {

    private val connstring = "jdbc:sqlite:pictures.db"

    private val logger = LoggerFactory.getLogger(PictureDatabaseConnector::class.java)

    val dbInitSql = """
        CREATE TABLE IF NOT EXISTS pictures (
            image_id TEXT PRIMARY KEY,
            image_data BLOB NOT NULL UNIQUE
        );
    """.trimIndent()

    init {
        File("pictures.db").createNewFile()

        val conn = DriverManager.getConnection(connstring)
        conn.use { connHandle ->
            connHandle.prepareStatement(dbInitSql).use {
                it.execute()
            }
        }
    }

    val saveImageSql = """
        INSERT INTO pictures (image_id, image_data) 
        VALUES (?, ?)
    """.trimIndent()

    fun saveImage(imageData: ByteArrayInputStream): UUID {
        val uuid = UUID.randomUUID()

        val bytes = imageData.readBytes()

        val conn = DriverManager.getConnection(connstring)
        conn.use { connHandle ->
            val statement = connHandle.prepareStatement(saveImageSql).apply {
                setString(1, uuid.toString())
                setBytes(2, bytes)
            }
            try {
                statement.execute()
            } catch (e: SQLiteException) {
                logger.error("Error while uploading image: ${e.message}", e)
                // handle image deduplication
                return this.getImageIdFromBlob(bytes)
            }

        }
        return uuid
    }

    val getImageSql = """
        SELECT * FROM pictures
        WHERE image_id = ?;
    """.trimIndent()

    fun getImageData(uuid: UUID): ByteArray? {
        val conn = DriverManager.getConnection(connstring)
        conn.use { connHandle ->
            val statement = connHandle.prepareStatement(getImageSql).apply {
                setString(1, uuid.toString())
            }

            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return resultSet.getBytes("image_data")
            }
        }
        return null
    }

    val getIdSql = """
        SELECT image_id FROM pictures
        WHERE image_data = ?;
    """.trimIndent()

    fun getImageIdFromBlob(data: ByteArray): UUID {
        val conn = DriverManager.getConnection(connstring)
        conn.use { connHandle ->
            val statement = connHandle.prepareStatement(getIdSql).apply {
                setBytes(1, data)
            }

            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return UUID.fromString(resultSet.getString("image_id"))
            }
        }

        throw IllegalStateException("Tried to find the ID for a duplicate, but it somehow disappeared")
    }
}
