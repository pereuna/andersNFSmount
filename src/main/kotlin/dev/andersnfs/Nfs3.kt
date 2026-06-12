package dev.andersnfs

import java.io.File

/**
 * Tiny placeholder for the NFSv3 client API used by MainActivity.
 *
 * This is intentionally simple: the real NFS library can later replace this
 * class while keeping the first UI-thread/background-thread experiment intact.
 */
class Nfs3(private val server: String) {
    fun mount(export: String): NfsFileSystem {
        println("Connecting to NFS server $server export $export")
        return NfsFileSystem(export)
    }
}

class NfsFileSystem(private val export: String) {
    fun list(path: String): List<String> {
        val normalizedPath = path.trim('/').takeIf { it.isNotEmpty() }
        val directory = if (normalizedPath == null) {
            File(export)
        } else {
            File(export, normalizedPath)
        }

        return directory.list()?.sorted().orEmpty()
    }
}
