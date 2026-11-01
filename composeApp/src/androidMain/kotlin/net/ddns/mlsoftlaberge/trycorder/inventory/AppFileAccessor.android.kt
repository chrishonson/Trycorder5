package net.ddns.mlsoftlaberge.trycorder.inventory

import java.io.File

actual class AppFileAccessor actual constructor(directory: String, fileName: String) : FileAccessor {
    private val file = File(directory, fileName)

    actual override fun read(): String? = if (file.exists()) file.readText() else null

    /** Writes a temporary file then renames it, so a crash never leaves half a file. */
    actual override fun write(text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}
