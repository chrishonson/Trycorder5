package net.ddns.mlsoftlaberge.trycorder.inventory

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class AppFileAccessor actual constructor(directory: String, fileName: String) : FileAccessor {
    private val path = "$directory/$fileName"

    actual override fun read(): String? =
        if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        } else {
            null
        }

    actual override fun write(text: String) {
        NSString.create(string = text)
            .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}

/** The app's Documents directory. */
fun iosDocumentsDirectory(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String ?: "."
