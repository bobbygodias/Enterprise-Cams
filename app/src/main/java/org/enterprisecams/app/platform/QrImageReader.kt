package org.enterprisecams.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream

object QrImageReader {
    private const val MAX_BYTES = 12 * 1024 * 1024

    fun read(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BYTES) { "Escolha uma imagem de até 12 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Não foi possível abrir a imagem.")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "O arquivo não é uma imagem compatível." }
        options.inSampleSize = 1
        while (options.outWidth / options.inSampleSize > 2048 || options.outHeight / options.inSampleSize > 2048) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: error("Não foi possível ler a imagem.")
        return try { decode(bitmap) } finally { bitmap.recycle() }
    }

    fun decode(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val hints = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)
        val reader = MultiFormatReader()
        return try { reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text }
        catch (_: NotFoundException) {
            try { reader.decode(BinaryBitmap(HybridBinarizer(source.invert())), hints).text }
            catch (_: NotFoundException) { throw IllegalArgumentException("Não encontrei um QR code legível. Use uma foto mais próxima ou cole o link.") }
        } finally { reader.reset() }
    }
}
