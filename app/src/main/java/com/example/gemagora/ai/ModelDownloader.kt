package com.example.gemagora.ai

import com.example.gemagora.data.model.ModelDownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloader {

    suspend fun downloadModel(
        url: String,
        token: String?,
        destinationFile: File,
        onProgress: (ModelDownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val tempFile = File("${destinationFile.absolutePath}.download")
        var currentUrl = url
        var connection: HttpURLConnection? = null
        var redirects = 0
        val maxRedirects = 6

        try {
            destinationFile.parentFile?.mkdirs()
            tempFile.delete()

            while (redirects < maxRedirects) {
                connection?.disconnect()
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 20000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; GemAgora)")
                    if (!token.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else URL(URL(currentUrl), location).toString()
                        redirects++
                        continue
                    }
                }

                if (responseCode !in 200..299) {
                    tempFile.delete()
                    onProgress(ModelDownloadState.Failed("伺服器回應錯誤: $responseCode"))
                    return@withContext
                }
                break
            }

            val activeConn = connection ?: run {
                tempFile.delete()
                onProgress(ModelDownloadState.Failed("無法建立連線"))
                return@withContext
            }

            val totalSize = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                activeConn.contentLengthLong
            } else {
                activeConn.contentLength.toLong()
            }

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L

            onProgress(ModelDownloadState.Downloading(0f, 0L, totalSize))

            activeConn.inputStream.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    while (true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break

                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        val progress = if (totalSize > 0) totalBytesRead.toFloat() / totalSize else 0f
                        onProgress(ModelDownloadState.Downloading(progress, totalBytesRead, totalSize))
                    }
                    outputStream.flush()
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                onProgress(ModelDownloadState.Failed("寫入檔案失敗：檔案為空"))
                return@withContext
            }

            if (totalSize > 0 && totalBytesRead < totalSize) {
                tempFile.delete()
                onProgress(ModelDownloadState.Failed("下載中斷：檔案不完整 ($totalBytesRead / $totalSize bytes)"))
                return@withContext
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            if (tempFile.renameTo(destinationFile)) {
                onProgress(ModelDownloadState.Completed)
            } else {
                tempFile.delete()
                onProgress(ModelDownloadState.Failed("儲存模型檔案失敗"))
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            onProgress(ModelDownloadState.Cancelled)
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            onProgress(ModelDownloadState.Failed(e.localizedMessage ?: "下載失敗"))
        } finally {
            connection?.disconnect()
        }
    }
}
