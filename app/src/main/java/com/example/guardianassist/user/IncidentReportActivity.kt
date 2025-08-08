package com.example.guardianassist.user

import android.Manifest
import android.R
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.guardianassist.appctrl.IncidentReportResponse
import com.example.guardianassist.appctrl.RetrofitClient
import com.example.guardianassist.appctrl.SessionManager
import com.example.guardianassist.databinding.ActivityIncidentReportBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IncidentReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidentReportBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var capturedIncidentBitmap: Bitmap? = null
    private var capturedCorrectiveBitmap: Bitmap? = null
    private var camera: Camera? = null // Optional camera instance

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
        private const val TAG = "IncidentReportActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidentReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (!isCameraPermissionGranted()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
        if (sessionManager.fetchSiteId() <= 0) {
            AlertDialog.Builder(this)
                .setTitle("Booking Required")
                .setMessage("Please Clock In first.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
            return
        }

        setupIncidentDropdown()
        setupListeners()
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupIncidentDropdown() {
        val incidentTypes = arrayOf(
            "Unauthorized Access",
            "Theft",
            "Vandalism",
            "Equipment Failure",
            "Suspicious Activity",
            "Other"
        )

        val adapter = ArrayAdapter(this, R.layout.simple_spinner_dropdown_item, incidentTypes)
        binding.spinnerIncidentType.adapter = adapter

        binding.spinnerIncidentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.inputLayoutCustomIncident.visibility =
                    if (position == incidentTypes.size - 1) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        binding.btnCaptureIncidentPhoto.setOnClickListener {
            startCameraPreview(binding.cameraPreviewIncident) { bitmap ->
                capturedIncidentBitmap = bitmap
                binding.ivIncidentPhoto.setImageBitmap(bitmap)
                binding.ivIncidentPhoto.visibility = View.VISIBLE
                binding.cameraPreviewIncident.visibility = View.GONE
                Toast.makeText(this, "Incident photo captured successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCaptureCorrectivePhoto.setOnClickListener {
            startCameraPreview(binding.cameraPreviewCorrective) { bitmap ->
                capturedCorrectiveBitmap = bitmap
                binding.ivCorrectiveActionPhoto.setImageBitmap(bitmap)
                binding.ivCorrectiveActionPhoto.visibility = View.VISIBLE
                binding.cameraPreviewCorrective.visibility = View.GONE
                Toast.makeText(this, "Corrective action photo captured successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmitIncident.setOnClickListener {
            submitIncidentReport()
        }
    }

    private fun startCameraPreview(previewView: PreviewView, callback: (Bitmap) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                captureImage(previewView, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Camera preview failed: ${e.message}", e)
                Toast.makeText(this, "Failed to start camera preview.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage(previewView: PreviewView, callback: (Bitmap) -> Unit) {
        val imageCapture = imageCapture ?: return
        val photoFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = fixImageRotation(photoFile.absolutePath)
                callback(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Image capture failed: ${exception.message}", exception)
                Toast.makeText(this@IncidentReportActivity, "Failed to capture image.", Toast.LENGTH_SHORT).show()
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

    private fun submitIncidentReport() {
        binding.progressBar.visibility = View.VISIBLE

        val token = sessionManager.fetchUserToken()
        val realName = sessionManager.fetchRealName() ?: "Unknown"
        val orgId = sessionManager.fetchOrgId()
        val siteId = sessionManager.fetchSiteId()

        val incidentType = binding.spinnerIncidentType.selectedItem.toString().trim()
        val customIncident = if (incidentType == "Other") binding.etCustomIncident.text.toString().trim() else null
        val incidentDescription = binding.etIncidentDescription.text.toString().trim()
        val correctiveAction = binding.etCorrectiveAction.text.toString().trim()
        val severity = when (binding.rgSeverity.checkedRadioButtonId) {
            binding.rbLow.id -> "Low"
            binding.rbMedium.id -> "Medium"
            binding.rbHigh.id -> "High"
            else -> null
        }

        if (token.isNullOrEmpty() || incidentType.isBlank() || incidentDescription.isBlank() ||
            correctiveAction.isBlank() || severity.isNullOrEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        val tokenBody = token.toRequestBody("text/plain".toMediaTypeOrNull())
        val realNameBody = realName.toRequestBody("text/plain".toMediaTypeOrNull())
        val orgIdBody = orgId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val siteIdBody = siteId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val incidentTypeBody = incidentType.toRequestBody("text/plain".toMediaTypeOrNull())
        val customIncidentBody = customIncident?.toRequestBody("text/plain".toMediaTypeOrNull())
        val incidentDescriptionBody = incidentDescription.toRequestBody("text/plain".toMediaTypeOrNull())
        val correctiveActionBody = correctiveAction.toRequestBody("text/plain".toMediaTypeOrNull())
        val severityBody = severity.toRequestBody("text/plain".toMediaTypeOrNull())

        val incidentImagePart = bitmapToMultipart("incident_image", capturedIncidentBitmap)
        val correctiveImagePart = bitmapToMultipart("corrective_image", capturedCorrectiveBitmap)

        RetrofitClient.apiService.reportIncident(
            tokenBody,
            realNameBody,
            orgIdBody,
            siteIdBody,
            incidentTypeBody,
            customIncidentBody,
            incidentDescriptionBody,
            correctiveActionBody,
            severityBody,
            incidentImagePart,
            correctiveImagePart
        ).enqueue(object : Callback<IncidentReportResponse> {  // ✅ FIX: Changed from `Callback<Void>` to `Callback<IncidentReportResponse>`
            override fun onResponse(call: Call<IncidentReportResponse>, response: Response<IncidentReportResponse>) {
                binding.progressBar.visibility = View.GONE
                Log.d("IncidentReport", "API Response Code: ${response.code()}")
                Log.d("IncidentReport", "API Response Body: ${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@IncidentReportActivity, "Incident Report Submitted Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("IncidentReport", "Submission failed: $errorBody")
                    Toast.makeText(this@IncidentReportActivity, "Submission failed: $errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<IncidentReportResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.e("IncidentReport", "Submission failed: ${t.message}", t)
                Toast.makeText(this@IncidentReportActivity, "Submission failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }




private fun bitmapToMultipart(name: String, bitmap: Bitmap?): MultipartBody.Part? {
        if (bitmap == null) return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()
        val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, "$name.jpg", requestBody)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}