package utils

import com.tiedan.Config
import com.tiedan.ImgLocUploader.CONNECT_TIMEOUT
import com.tiedan.ImgLocUploader.READ_TIMEOUT
import com.tiedan.ImgLocUploader.logger
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DownloadHelper {

    data class DownloadResult(val success: Boolean, val message: String)

    fun downloadFile(fileUrl: String, outputPath: String): DownloadResult {
        val outputFile = File(outputPath)

        var resultMsg = ""

        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit {
            var connection: HttpURLConnection? = null

            try {
                logger.debug("执行下载文件：$fileUrl")
                connection = URL(fileUrl).openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { inputStream ->
                        FileOutputStream(outputFile).use { outputStream ->
                            val buffer = ByteArray(1024)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    resultMsg = "下载成功"
                } else {
                    resultMsg = "[错误] HTTP Status ${connection.responseCode}: ${connection.responseMessage}"
                }
            } catch (e: Exception) {
                resultMsg = "[错误] 下载时发生错误: ${e::class.simpleName}(${e.message})"
            } finally {
                connection?.disconnect()
            }
        }

        try {
            future.get(Config.timeout, TimeUnit.SECONDS)    // 限制下载时间
        } catch (e: Exception) {
            future.cancel(true)     // 超时后取消任务
            resultMsg += "[错误] 下载超时：超出最大时间限制${Config.timeout}秒"
        } finally {
            executor.shutdown()
        }

        logger.debug(resultMsg)
        val success = !resultMsg.startsWith("[错误]")
        return DownloadResult(success, resultMsg)
    }

}