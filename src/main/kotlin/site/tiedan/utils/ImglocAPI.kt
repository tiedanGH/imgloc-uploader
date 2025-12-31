package site.tiedan.utils

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import site.tiedan.Config
import site.tiedan.ImgLocUploader.logger
import site.tiedan.ImgLocUploader.save
import site.tiedan.UploadData
import site.tiedan.utils.DownloadHelper.downloadFile
import java.io.File
import java.util.concurrent.TimeUnit

object ImglocAPI {
    
    fun uploadImageFromUrlImgLoc(imageUrl: String, subject: Contact?): Pair<Boolean, String> {
        val fixedUrl = if (subject !is Group) {
            imageUrl.replace("download?appid=1407", "download?appid=1406")
        } else imageUrl
        return uploadImageFromUrlImgLoc(fixedUrl)
    }

    private fun uploadImageFromUrlImgLoc(imageUrl: String): Pair<Boolean, String> {
        val tempFile = File.createTempFile("upload_", "")
        try {
            val downloadResult = downloadFile(imageUrl, tempFile.path)
            if (!downloadResult.success) {
                return Pair(false, downloadResult.message)
            }

            logger.info("Uploading image: $imageUrl")

            val command = listOf(
                "curl",
                "-s",
                "-X", "POST",
                "-H", "X-API-Key: ${Config.API_Key}",
                "-H", "Content-Type: multipart/form-data",
                "-F", "source=@${tempFile.path}",
                "https://imgloc.com/api/1/upload"
            )
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(Config.timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                tempFile.delete()
                return false to "[请求超时] ${Config.timeout}秒内未返回结果"
            }

            val output = process.inputStream.bufferedReader().readText().trim()
            if (output.isEmpty()) {
                return false to "[上传失败] 返回内容为空"
            }

            val urlRegex = """"url"\s*:\s*"([^"]+)"""".toRegex()
            val url = urlRegex.find(output)?.groupValues?.get(1)?.replace("\\/", "/") ?: ""
            val deleteUrlRegex = """"delete_url"\s*:\s*"([^"]+)"""".toRegex()
            val deleteUrl = deleteUrlRegex.find(output)?.groupValues?.get(1)?.replace("\\/", "/")

            return if (url.startsWith("http")) {
                UploadData.history.add(0, url to deleteUrl)
                UploadData.save()
                true to url
            } else {
                false to "[上传失败] 返回内容异常：$output"
            }
        } catch (e: Exception) {
            return false to "[上传失败] 请求发生错误：${e.message}"
        } finally {
            tempFile.delete()
        }
    }

    fun deleteImageFromUrlImgLoc(deleteUrl: String): Pair<Boolean, String> {
        logger.info("Deleting image: $deleteUrl")

        val processBuilder = ProcessBuilder(
            "curl", "-X", "POST", deleteUrl
        )

        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        if (!process.waitFor(Config.timeout, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return false to "[请求超时] ${Config.timeout}秒内未返回结果"
        }

        return true to "删除成功"
    }
}