package com.example.facemark

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.facemark.Face_Recognition.FaceClassifier
import com.example.facemark.Face_Recognition.TFLiteFaceRecognition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.IOException
import android.os.Environment
import com.example.facemark.RegisterScreen



private fun Bitmap.fixRotation(context: Context, uri: Uri): Bitmap {
    val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED
    )

    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onFaceRegistered: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    navController: NavController
) {
    var cameraError by remember { mutableStateOf<String?>(null) }
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var detectedFaceRecognition by remember { mutableStateOf<FaceClassifier.Recognition?>(null) }
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    // Face Detection Setup with improved error handling
    val faceDetector = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
            .let { FaceDetection.getClient(it) }
    }

    val faceClassifier = remember {
        try {
            TFLiteFaceRecognition.create(
                context.assets,
                "facenet.tflite",
                160,
                false,
                context
            )
        } catch (e: IOException) {
            Log.e("RegisterScreen", "Face Classifier Creation Error", e)
            null
        }
    }

    // Image Capture Launchers with improved error handling


    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                try {
                    capturedImageUri = uri
                    val bitmap = context.getBitmapFromUri(uri)
                    bitmap?.let { originalBitmap ->
                        val fixedBitmap = originalBitmap.fixRotation(context, uri)
                        processImage(fixedBitmap, faceDetector, faceClassifier) { recognition ->
                            detectedFaceRecognition = recognition
                            capturedBitmap = fixedBitmap
                            showNameDialog = recognition != null
                        }
                    } ?: run {
                        cameraError = "Failed to load captured image"
                        Toast.makeText(context, "Failed to load captured image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("RegisterScreen", "Error processing camera image", e)
                    cameraError = "Error processing camera image: ${e.message}"
                    Toast.makeText(context, "Error processing camera image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            cameraError = "Failed to capture image"
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val bitmap = context.getBitmapFromUri(selectedUri)
            bitmap?.let { originalBitmap ->
                val fixedBitmap = originalBitmap.fixRotation(context, selectedUri)
                processImage(fixedBitmap, faceDetector, faceClassifier) { recognition ->
                    detectedFaceRecognition = recognition
                    capturedBitmap = fixedBitmap
                    showNameDialog = recognition != null
                }
            }
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            val uri = ImageUtils.createImageUri(context)
            if (uri != null) {
                currentPhotoUri = uri
                cameraLauncher.launch(uri) // ✅ launch camera here
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { (Text("Register Face", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,color = Color.Black, fontFamily = FacemarkFont)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.Add, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            capturedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier.size(300.dp)
                )
            }

            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Gallery")
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }

                Button(
                    onClick = {
                        val permissions = arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        permissionsLauncher.launch(permissions)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Camera")
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }
        }

        if (showNameDialog) {
            FaceRegistrationDialog(
                onDismiss = { showNameDialog = false },
                onRegister = { name ->
                    detectedFaceRecognition?.let { recognition ->
                        faceClassifier?.register(name, recognition)
                        showNameDialog = false
                        Toast.makeText(context, "Face registered successfully", Toast.LENGTH_SHORT).show()
                        onFaceRegistered()
                    }
                }
            )
        }
    }
}
object ImageUtils {
    private const val TAG = "ImageUtils"

    fun createImageUri(context: Context): Uri? {
        return try {
            val timeStamp = System.currentTimeMillis()
            val imageFileName = "FaceMark_$timeStamp.jpg"

            // Try external storage first
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir != null && storageDir.exists()) {
                val imageFile = File(storageDir, imageFileName)
                Log.d(TAG, "Creating image file at external storage: ${imageFile.absolutePath}")

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )
            } else {
                // Fallback to cache directory
                val cacheFile = File(context.cacheDir, imageFileName)
                Log.d(TAG, "Creating image file in cache: ${cacheFile.absolutePath}")

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cacheFile
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image URI", e)
            null
        }
    }
}

private fun processImage(
    bitmap: Bitmap,
    detector: FaceDetector,
    classifier: FaceClassifier?,
    onProcessed: (FaceClassifier.Recognition?) -> Unit
) {
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    detector.process(inputImage)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val croppedFace = Bitmap.createBitmap(
                    bitmap,
                    face.boundingBox.left - 1,
                    face.boundingBox.top - 1,
                    face.boundingBox.width() + 1,
                    face.boundingBox.height() + 1
                )
                val scaledFace = Bitmap.createScaledBitmap(croppedFace, 160, 160, true)
                Log.d("RegisterScreen", "Face detected, processing image...")

                val recognition = classifier?.recognizeImage(scaledFace, true)
                Log.d("RegisterScreen", "Recognition result: $recognition")


                onProcessed(recognition)
            } else {
                Log.d("RegisterScreen", "No face detected")
            }
        }
        .addOnFailureListener { e ->
            Log.e("RegisterScreen", "Face detection failed", e)
        }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRegistrationDialog(
    onDismiss: () -> Unit,
    onRegister: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Face") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter Name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onRegister(name)
                }
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
private fun Context.getBitmapFromUri(uri: Uri): Bitmap? {
    return try {
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    } catch (e: Exception) {
        null
    }
}

private fun performFaceDetection(
    bitmap: Bitmap,
    detector: FaceDetector,
    classifier: FaceClassifier?,
    onFaceDetected: (Rect, FaceClassifier.Recognition?) -> Unit
) {
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    detector.process(inputImage)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0]
                val croppedFace = Bitmap.createBitmap(
                    bitmap,
                    face.boundingBox.left,
                    face.boundingBox.top,
                    face.boundingBox.width(),
                    face.boundingBox.height()
                )

                val scaledFace = Bitmap.createScaledBitmap(
                    croppedFace,
                    160,
                    160,
                    true
                )

                val recognition = classifier?.recognizeImage(scaledFace, true)
                onFaceDetected(face.boundingBox, recognition)
            } else {
                // No faces detected
            }
        }
        .addOnFailureListener { e ->
            Log.e("FaceDetection", "Face detection failed", e)
        }
}