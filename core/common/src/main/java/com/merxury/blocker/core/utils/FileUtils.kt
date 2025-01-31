/*
 * Copyright 2023 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.blocker.core.utils

import com.merxury.blocker.core.extension.exec
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtils {

    @JvmStatic
    fun copy(source: String, dest: String): File {
        val sourceFile = SuFile(source)
        val destFile = SuFile(dest)
        return sourceFile.copyTo(destFile)
    }

    @JvmStatic
    suspend fun cat(source: String, dest: String) {
        "cat '$source' > '$dest'".exec()
    }

    @JvmStatic
    fun listFiles(path: String): List<String> {
        val file = SuFile(path)
        if (!file.exists()) {
            Timber.w("File $path not exists")
            return ArrayList()
        }
        return file.list()?.toList() ?: ArrayList()
    }

    @JvmStatic
    suspend fun chmod(path: String, permission: Int, recursively: Boolean) {
        val comm = when (recursively) {
            true -> "chmod $permission '$path'"
            false -> "chmod -R $permission '$path'"
        }
        comm.exec()
    }

    @JvmStatic
    fun read(path: String): String {
        SuFileInputStream.open(path).use {
            return it.readBytes().toString(Charsets.UTF_8)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    suspend fun delete(
        path: String,
        recursively: Boolean,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Boolean = withContext(dispatcher) {
        val file = SuFile(path)
        if (!file.exists()) {
            Timber.e("Can't delete $path since it doesn't exist")
            return@withContext false
        }
        return@withContext if (recursively) {
            file.deleteRecursive()
        } else {
            file.delete()
        }
    }

    fun getFileSize(path: String): Long {
        val file = SuFile(path)
        return if (file.exists()) {
            file.length()
        } else {
            0L
        }
    }

    suspend fun zipFile(prefix: String, input: File): File? {
        return withContext(Dispatchers.IO) {
            if (!input.exists()) {
                return@withContext null
            }
            val output = File.createTempFile(prefix, ".zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
                input.walkTopDown().forEach { file ->
                    val zipFileName = file.absolutePath
                        .removePrefix(input.absolutePath)
                        .removePrefix("/")
                    val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                    zos.putNextEntry(entry)
                    if (file.isFile) {
                        file.inputStream().copyTo(zos)
                    }
                }
            }
            return@withContext output
        }
    }
}
