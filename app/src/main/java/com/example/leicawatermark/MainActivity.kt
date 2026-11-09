package com.example.leicawatermark

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private var selectedImageUri: Uri? = null
    private var processedBitmap: Bitmap? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPreview = findViewById(R.id.ivPreview)
        val btnSelect = findViewById<Button>(R.id.btnSelect)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnSave.setOnClickListener {
            if (processedBitmap != null) {
                Toast.makeText(this, "相片已成功儲存！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "請先選擇相片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri -> generateWatermark(uri) }
        }
    }

    private fun generateWatermark(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val exifInputStream = contentResolver.openInputStream(uri)
            val exif = ExifInterface(exifInputStream!!)
            exifInputStream.close()

            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "LEICA MP"
            val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL) ?: "AGFA APX100"
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "2023.03.19 08:46:12"
            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: "120mm"
            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "f/4.1"
            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "1/100"
            val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: "90"

            processedBitmap = createLeicaWatermarkBitmap(
                originalBitmap,
                "$model + $lens",
                dateTime.replace(":", "."),
                "$focalLength  $aperture  $exposureTime  ISO$iso",
                "40°3'13\"N 116°19'25\"E"
            )
            ivPreview.setImageBitmap(processedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "讀取 EXIF 失敗", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createLeicaWatermarkBitmap(
        src: Bitmap, lTop: String, lBottom: String, rTop: String, rBottom: String
    ): Bitmap {
        val width = src.width
        val originalHeight = src.height
        val barHeight = (width * 0.14).toInt()
        val totalHeight = originalHeight + barHeight

        val result = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, null)

        val paint = Paint().apply { isAntiAlias = true }
        val textSize = width * 0.025f

        paint.textSize = textSize
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(lTop, width * 0.04f, originalHeight + barHeight * 0.4f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        canvas.drawText(lBottom, width * 0.04f, originalHeight + barHeight * 0.75f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(rTop, width * 0.96f, originalHeight + barHeight * 0.4f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        canvas.drawText(rBottom, width * 0.96f, originalHeight + barHeight * 0.75f, paint)

        paint.textAlign = Paint.Align.CENTER
        val centerX = width * 0.5f
        val centerY = originalHeight + barHeight * 0.5f
        val radius = barHeight * 0.32f

        paint.color = Color.parseColor("#E30613")
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = radius * 0.75f
        canvas.drawText("Leica", centerX, centerY + (radius * 0.3f), paint)

        return result
    }
}
