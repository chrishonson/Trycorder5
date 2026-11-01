package net.ddns.mlsoftlaberge.trycorder.inventory

/** A single text file of the app's private storage. Calls block: use them off the main thread. */
interface FileAccessor {
    /** The file content, or null when the file does not exist. */
    fun read(): String?

    fun write(text: String)
}

/**
 * A file [fileName] in [directory], the app's private directory:
 * Android passes Context.filesDir, iOS the Documents directory.
 */
expect class AppFileAccessor(directory: String, fileName: String) : FileAccessor {
    override fun read(): String?
    override fun write(text: String)
}
