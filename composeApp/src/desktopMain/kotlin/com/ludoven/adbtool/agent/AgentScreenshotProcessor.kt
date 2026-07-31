package com.ludoven.adbtool.agent

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

data class AgentScreenshot(
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int,
    val height: Int
)

class AgentScreenshotProcessor {
    fun process(png: ByteArray): AgentScreenshot? {
        val source = runCatching {
            ImageIO.read(ByteArrayInputStream(png))
        }.getOrNull() ?: return null
        if (source.width <= 0 || source.height <= 0) return null

        var image = resize(source, MAX_SCREENSHOT_DIMENSION)
        var quality = DEFAULT_JPEG_QUALITY
        var encoded = encodeJpeg(image, quality) ?: return null
        while (encoded.size > MAX_SCREENSHOT_BYTES && quality > MIN_JPEG_QUALITY) {
            quality -= JPEG_QUALITY_STEP
            encoded = encodeJpeg(image, quality) ?: return null
        }
        while (encoded.size > MAX_SCREENSHOT_BYTES && image.width > MIN_SCREENSHOT_DIMENSION) {
            image = resize(image, (image.width.coerceAtLeast(image.height) * 0.8).toInt())
            encoded = encodeJpeg(image, quality) ?: return null
        }
        if (encoded.size > MAX_SCREENSHOT_BYTES) return null
        return AgentScreenshot(
            bytes = encoded,
            mimeType = "image/jpeg",
            width = source.width,
            height = source.height
        )
    }

    private fun resize(source: BufferedImage, maxDimension: Int): BufferedImage {
        val largest = source.width.coerceAtLeast(source.height)
        if (largest <= maxDimension && source.type == BufferedImage.TYPE_INT_RGB) return source
        val scale = (maxDimension.toDouble() / largest).coerceAtMost(1.0)
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        val target = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        target.createGraphics().use { graphics ->
            graphics.color = Color.BLACK
            graphics.fillRect(0, 0, width, height)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(source, 0, 0, width, height, null)
        }
        return target
    }

    private fun encodeJpeg(image: BufferedImage, quality: Float): ByteArray? = runCatching {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().first()
        val output = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(output).use { stream ->
            writer.output = stream
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality.coerceIn(0f, 1f)
            }
            writer.write(null, IIOImage(image, null, null), params)
        }
        writer.dispose()
        output.toByteArray()
    }.getOrNull()
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}

private const val MAX_SCREENSHOT_DIMENSION = 1_200
private const val MIN_SCREENSHOT_DIMENSION = 480
private const val MAX_SCREENSHOT_BYTES = 1_000_000
private const val DEFAULT_JPEG_QUALITY = 0.82f
private const val MIN_JPEG_QUALITY = 0.5f
private const val JPEG_QUALITY_STEP = 0.08f
