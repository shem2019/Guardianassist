package com.example.guardianassist.user

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.guardianassist.R
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Uniformcheck : AppCompatActivity() {

    // UI Components
    private lateinit var cameraPreview: PreviewView
    private lateinit var capturedImage: ImageView
    private lateinit var takeButton: Button
    private lateinit var retakeButton: Button
    private lateinit var switchCameraButton: FloatingActionButton

    private lateinit var submitButton: Button

    // NFC Components
    private lateinit var handler: Handler
    private lateinit var nfcTimeoutRunnable: Runnable
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var loadingDialog: AlertDialog
    private var isWaitingForTag = true

    // Camera Components
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var capturedBitmap: Bitmap? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Session Management
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val NFC_TIMEOUT = 15000L // 15 seconds in milliseconds

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uniformcheck)

        // Initialize UI Components
        cameraPreview = findViewById(R.id.cameraPreview)
        capturedImage = findViewById(R.id.capturedImage)
        takeButton = findViewById(R.id.takeButton)
        retakeButton = findViewById(R.id.retakeButton)
        switchCameraButton= findViewById(R.id.switchCameraFab)
        submitButton = findViewById(R.id.submitButton)

        sessionManager = SessionManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // NFC Setup
        handler = Handler(Looper.getMainLooper())
        nfcTimeoutRunnable = Runnable {
            if (isWaitingForTag) {
                isWaitingForTag = false
                loadingDialog.dismiss()
                Toast.makeText(this, "NFC scan timed out. Please try again.", Toast.LENGTH_SHORT).show()
                finish() // Close the activity
            }
        }

        // Start the timeout countdown
        handler.postDelayed(nfcTimeoutRunnable, NFC_TIMEOUT)


        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Request Camera Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }

        // Disable Camera Buttons Initially
        takeButton.isEnabled = false
        retakeButton.isEnabled = false
        submitButton.isEnabled = false

        // NFC Prompt
        showNfcPrompt()
    }

    private fun showNfcPrompt() {
        loadingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting to Scan NFC Tag")
            .setMessage("Place your device near an NFC tag with 'Book On'...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

    }

    override fun onResume() {
        super.onResume()
        if (isWaitingForTag) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isWaitingForTag && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                handleNfcTag(tag)
            } else {
                Toast.makeText(this, "No NFC tag detected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            ndef?.connect()
            val ndefMessage: NdefMessage? = ndef?.ndefMessage
            ndef?.close()

            if (ndefMessage != null && ndefMessage.records.isNotEmpty()) {
                val record: NdefRecord = ndefMessage.records[0]
                val payload = String(record.payload, Charsets.UTF_8)
                val cleanedPayload = payload.substring(3) // Skip the language code prefix

                if (cleanedPayload.contains("Clock In", ignoreCase = true)) {
                    loadingDialog.dismiss()
                    isWaitingForTag = false
                    handler.removeCallbacks(nfcTimeoutRunnable) // Cancel timeout
                    Toast.makeText(this, "Book On detected!", Toast.LENGTH_LONG).show()
                    enableCameraFeatures()
                } else {
                    Toast.makeText(this, "This is not a 'Book On' tag.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "NFC tag is empty.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error reading NFC tag", e)
            Toast.makeText(this, "Error reading NFC tag.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun enableCameraFeatures() {
        startCamera()
        takeButton.isEnabled = true
        retakeButton.isEnabled = true
        submitButton.isEnabled = true

        takeButton.setOnClickListener { captureImage() }
        retakeButton.setOnClickListener { retakeImage() }
        switchCameraButton.setOnClickListener { switchCamera() }


        submitButton.setOnClickListener { submitImage() }
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("Uniformcheck", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(cacheDir, "uniform_check_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = fixImageRotation(photoFile.absolutePath) // Correct rotation
                capturedBitmap = bitmap
                capturedImage.setImageBitmap(bitmap)
                capturedImage.visibility = View.VISIBLE
                cameraPreview.visibility = View.GONE
                takeButton.visibility = View.GONE
                retakeButton.visibility = View.VISIBLE
                submitButton.visibility = View.VISIBLE
                Toast.makeText(this@Uniformcheck, "Image captured successfully!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Uniformcheck", "Image capture failed: ${exception.message}", exception)
                Toast.makeText(this@Uniformcheck, "Failed to capture image.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fixImageRotation(imagePath: String): Bitmap {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val exif = ExifInterface(imagePath)

        val rotation = when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }



    private fun retakeImage() {
        capturedBitmap = null
        capturedImage.visibility = View.GONE
        cameraPreview.visibility = View.VISIBLE
        takeButton.visibility = View.VISIBLE
        retakeButton.visibility = View.GONE
        submitButton.visibility = View.GONE
    }

    private fun submitImage() {
        val token = sessionManager.fetchUserToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid session. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (capturedBitmap == null) {
            Toast.makeText(this, "No image to submit.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show a progress dialog
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Submitting Image")
            .setMessage("Uploading. Please wait...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        val stream = ByteArrayOutputStream()
        capturedBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()

        val requestBody = RequestBody.Companion.create("image/jpeg".toMediaTypeOrNull(), byteArray)
        val imagePart = MultipartBody.Part.createFormData("image", "uniform_check.jpg", requestBody)
        val descriptionPart = RequestBody.Companion.create("text/plain".toMediaTypeOrNull(), "Uniform Check")

        RetrofitClient.apiService.uploadImage("Bearer $token", imagePart, descriptionPart)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        Toast.makeText(this@Uniformcheck, "Uniform Check Successful!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@Uniformcheck, "Failed to submit image.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(this@Uniformcheck, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}