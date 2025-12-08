package nl.boonsboos.kdn.data;

import java.io.ByteArrayInputStream
import java.io.File
import java.sql.DriverManager
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

class PictureDatabaseConnector {

    val connstring = "jdbc:sqlite:pictures.db"

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

        val conn = DriverManager.getConnection(connstring)
        conn.use { connHandle ->
            val statement = connHandle.prepareStatement(saveImageSql).apply {
                setString(1, uuid.toString())
                setBlob(2, imageData)
            }
            try {
                statement.executeQuery()
            } catch (e: SQLIntegrityConstraintViolationException) {
                // handle image deduplication
                return this.getImageIdFromBlob(imageData)
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

    fun getImageIdFromBlob(data: ByteArrayInputStream): UUID {
        val conn = DriverManager.getConnection(connstring)
        conn.use { connHandle ->
            val statement = connHandle.prepareStatement(getIdSql).apply {
                setBlob(1, data)
            }

            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return UUID.fromString(resultSet.getString("image_id"))
            }
        }

        throw IllegalStateException("Tried to find the ID for a duplicate, but it somehow disappeared")
    }
}
