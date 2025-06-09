package com.tiedan.utils

import com.tiedan.Config
import com.tiedan.ImgLocUploader.logger
import com.tiedan.UploadData
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import utils.DownloadHelper.downloadFile
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
                "--fail-with-body",
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

            // TODO 获取删除图片需要的URL
            val regex = """"url"\s*:\s*"([^"]+)"""".toRegex()
            val url = regex.find(output)?.groupValues?.get(1)?.replace("\\/", "/")

            return if (url != null && url.startsWith("http")) {
                UploadData.history.add(url)
                if (UploadData.history.size > 20) UploadData.history.removeFirst()
                true to url
            } else {
                false to "[上传失败] 返回内容异常：$output"
            }
        } catch (e: Exception) {
            return Pair(false, "[上传失败] 请求发生错误：${e.message}")
        } finally {
            tempFile.delete()
        }
    }
}