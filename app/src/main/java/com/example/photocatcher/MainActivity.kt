package com.example.photocatcher

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var currentPicturePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        open_camera.setOnClickListener {
            // permission camera here
            startCheckingPermissions()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var image: File? = null
        if (intent.resolveActivity(packageManager) != null) {
            // create file
            image = generatePicturePath(this)
        } else {
            // don't have camera
        }

        if (image != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                intent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", image)
                )
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image))
            }
            currentPicturePath = image.absolutePath
            path_absolute.text = currentPicturePath
        }

        startActivityForResult(intent, 13)
    }

    private fun startCheckingPermissions() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        if (!hasPermission(permissions)) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                12
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            12 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    // boop permission denied!
                }
                return
            }
        }
    }

    private fun hasPermission(permissions: Array<String>): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions
                .filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
                .forEach { return false }
        }
        return true
    }

    private fun generatePicturePath(context: Context): File? {
        try {
            val storageDir = getAlbumDir(context, BuildConfig.APPLICATION_ID)
            val date = Date()
            date.time = System.currentTimeMillis() + random(1, 1000)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date)
            return File(storageDir, "IMG_$timeStamp.jpg")
        } catch (e: Exception) {

        }

        return null
    }

    private fun random(from: Int, to: Int): Int {
        return Random().nextInt((to + 1) - from) + from
    }

    private fun getAlbumDir(context: Context, appId: String?): File? {
        var storageDir: File? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }

        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appId)
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    return null
                }
            }
        }

        return storageDir
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 13) {

                val file = File(currentPicturePath)
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                preview.setImageBitmap(bitmap)

                addMediaToGallery(currentPicturePath)
                currentPicturePath = null
            }
        }
    }

    private fun addMediaToGallery(fromPath: String?) {
        if (fromPath == null) {
            return
        }
        val f = File(fromPath)
        val contentUri = Uri.fromFile(f)
        addMediaToGallery(contentUri)
    }

    private fun addMediaToGallery(uri: Uri?) {
        if (uri == null) {
            return
        }
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = uri
            applicationContext.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {

        }
    }

}
