package com.example.facemark

import com.example.facemark.Face_Recognition.FaceClassifier
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.facemark.Face_Recognition.TFLiteFaceRecognition
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.FileProvider
import android.os.Environment
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    // State variables
    var detectionResults by remember { mutableStateOf("No results yet.") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // Dropdown menu state
    val options = listOf("Subject 1", "Subject 2", "Subject 3")
    var selectedOption by remember { mutableStateOf(options[0]) }
    var expanded by remember { mutableStateOf(false) }

    val batches = listOf("F1", "F2", "F3","F4", "F5", "F6","F7", "F8", "F9")
    var selectedBatch by remember { mutableStateOf(batches[0]) }
    var expandedBatch by remember { mutableStateOf(false) }

    // Face detector setup
    val highAccuracyOpts = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    }
    val detector = remember { FaceDetection.getClient(highAccuracyOpts) }

    // Face recognition classifier
    val faceClassifier = remember {
        try {
            TFLiteFaceRecognition.create(
                context.assets,
                "facenet.tflite",
                TF_OD_API_INPUT_SIZE,
                false,
                context
            )
        } catch (e: IOException) {
            null
        }
    }

    // Gallery image picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputImage = uriToBitmap(context, it)
            val rotated = rotateBitmap(context, inputImage, it)
            rotated?.let { bitmap ->
                performFaceDetection(bitmap, detector, faceClassifier) { processedBitmap, results ->
                    imageBitmap = processedBitmap
                    detectionResults = results
                }
            }
        }
    }

    // Similar modification for cameraLauncher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                try {
                    val inputImage = uriToBitmap(context, uri)
                    if (inputImage != null) {
                        val rotated = rotateBitmap(context, inputImage, uri)
                        rotated?.let { bitmap ->
                            performFaceDetection(bitmap, detector, faceClassifier) { processedBitmap, results ->
                                imageBitmap = processedBitmap
                                detectionResults = results
                            }
                        }
                    } else {
                        Log.e("Camera", "Failed to decode image from URI")
                        detectionResults = "Error: Failed to process image"
                    }
                } catch (e: Exception) {
                    Log.e("Camera", "Error processing camera image", e)
                    detectionResults = "Error processing image: ${e.message}"
                }
            }
        } else {
            Log.e("Camera", "Camera capture failed")
            detectionResults = "Camera capture failed"
        }
    }

    // Modified permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.CAMERA] == true -> {
                try {
                    val uri = createImageUri(context)
                    tempPhotoUri = uri
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Log.e("Camera", "Error creating image URI", e)
                    detectionResults = "Error: Could not initialize camera"
                }
            }
            else -> {
                detectionResults = "Camera permission is required"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,color = Color.Black, fontFamily = FacemarkFont)}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Picker
            TextButton(onClick = { showDatePicker = true }) {
                Text(
                    text = "Selected Date: $selectedDate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF808080)
                )
            }

            // Subject Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subject: ", modifier = Modifier.padding(end = 8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { expanded = true }) {
                        Text(text = selectedOption)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                },
                                text = { Text(option) }
                            )
                        }
                    }
                }
            }

            // Batch Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Batch: ", modifier = Modifier.padding(end = 8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { expandedBatch = true }) {
                        Text(text = selectedBatch)
                    }
                    DropdownMenu(
                        expanded = expandedBatch,
                        onDismissRequest = { expandedBatch = false }
                    ) {
                        batches.forEach { batch ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedBatch = batch
                                    expandedBatch = false
                                },
                                text = { Text(batch) }
                            )
                        }
                    }
                }
            }

            // Image Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                imageBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Recognized Face",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Text("No image selected")
            }

            // Detection Results
            Text(
                text = "Detection Results: $detectionResults",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF808080)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Gallery")
                }

                Button(
                    onClick = {
                        if (checkPermissions(context)) {
                            captureImage(context) { uri ->
                                imageUri = uri
                                cameraLauncher.launch(uri)
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Camera")
                }
            }

            // Confirm Button
            Button(
                onClick = {
                    showConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
            ) {
                Text("Confirm")
            }

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Confirm Detection") },
                    text = {
                        Text("Do you want to save this detection for ${selectedOption} on $selectedDate?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                saveDetectionToFirestore(
                                    firestore,
                                    selectedBatch,
                                    selectedDate,
                                    selectedOption,
                                    detectionResults
                                )
                                showConfirmDialog = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedDate = "$dayOfMonth/${month + 1}/$year"
                    showDatePicker = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
}
private fun createImageUri(context: Context): Uri {
    // First try to create the file in external files directory
    try {
        val imageFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "FaceMark_${System.currentTimeMillis()}.jpg"
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    } catch (e: Exception) {
        // Fallback to cache directory if external storage is not available
        val cacheFile = File(
            context.cacheDir,
            "FaceMark_${System.currentTimeMillis()}.jpg"
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            cacheFile
        )
    }
}

// Utility functions
private fun uriToBitmap(context: Context, selectedFileUri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(selectedFileUri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}
private fun rotateBitmap(context: Context, input: Bitmap?, uri: Uri): Bitmap? {
    return input
}

private fun checkPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
}
private fun captureImage(context: Context, onUriReady: (Uri) -> Unit) {
    try {
        val uri = createImageUri(context)
        onUriReady(uri)
    } catch (e: Exception) {
        Log.e("CaptureImage", "Failed to capture image", e)
        onUriReady(Uri.EMPTY)
    }
}


private fun saveDetectionToFirestore(
    firestore: FirebaseFirestore,
    selectedBatch: String,
    selectedDate: String,
    selectedOption: String,
    detectionResults: String
) {
    // Split detection results by comma and trim whitespace
    val detectedObjects = detectionResults
        .split(",")
        .map { it.trim().replace("'", "") }
        .filter { it.isNotEmpty() && it != "No faces detected" }

    if (detectedObjects.isNotEmpty()) {
        // Create a unique key for the attendance entry
        val attendanceKey = "${selectedDate}_${selectedOption}"

        // Batch write operation to update multiple documents
        val batch = firestore.batch()

        // Track which names were detected
        val detectedNames = mutableSetOf<String>()

        // First, update detected faces with attendance as true
        detectedObjects.forEach { detectedObject ->
            if (detectedObject.isNotEmpty()) {
                detectedNames.add(detectedObject)
                val documentRef = firestore.collection(selectedBatch)
                    .document(detectedObject)

                // Create the document data with attendance marked true
                val documentData = hashMapOf(
                    attendanceKey to true
                )

                batch.set(documentRef, documentData, SetOptions.merge())
            }
        }

        // Then, query all documents in the batch and set undetected names to false
        firestore.collection(selectedBatch)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { document ->
                    if (!detectedNames.contains(document.id)) {
                        // If the document wasn't in the detected names, set attendance to false
                        val documentRef = firestore.collection(selectedBatch)
                            .document(document.id)

                        val documentData = hashMapOf(
                            attendanceKey to false
                        )

                        batch.set(documentRef, documentData, SetOptions.merge())
                    }
                }

                // Commit the batch write
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Successfully saved attendance for $detectedObjects")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to save attendance", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to query batch documents", e)
            }
    } else {
        Log.d("Firestore", "No valid detection results")
    }
}
fun performFaceDetection(
    inputImage: Bitmap,
    detector: FaceDetector,
    faceClassifier: FaceClassifier?,
    onProcessed: (Bitmap, String) -> Unit
) {
    val mutableBmp = inputImage.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBmp)
    val image = InputImage.fromBitmap(inputImage, 0)

    detector.process(image)
        .addOnSuccessListener { faces ->
            val detectionList = mutableListOf<String>()
            for (face in faces) {
                val bounds = face.boundingBox
                val p = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    color = android.graphics.Color.RED
                }
                canvas.drawRect(bounds, p)

                performFaceRecognition(bounds, inputImage, canvas, faceClassifier) { result ->
                    result?.let { detectionList.add(it) }
                }
            }
            val detectionResults = if (detectionList.isNotEmpty())
                detectionList.joinToString(", ")
            else "No faces detected"
            onProcessed(mutableBmp, detectionResults)
        }
        .addOnFailureListener {
            onProcessed(inputImage, "Detection failed")
        }
}

private fun performFaceRecognition(
    bounds: Rect,
    inputImage: Bitmap,
    canvas: Canvas,
    faceClassifier: FaceClassifier?,
    onResult: (String?) -> Unit
) {
    faceClassifier ?: return

    val safeBounds = Rect(
        bounds.left.coerceAtLeast(0),
        bounds.top.coerceAtLeast(0),
        bounds.right.coerceAtMost(inputImage.width - 1),
        bounds.bottom.coerceAtMost(inputImage.height - 1)
    )

    var croppedFace = Bitmap.createBitmap(
        inputImage,
        safeBounds.left,
        safeBounds.top,
        safeBounds.width(),
        safeBounds.height()
    )
    croppedFace = Bitmap.createScaledBitmap(
        croppedFace,
        TF_OD_API_INPUT_SIZE,
        TF_OD_API_INPUT_SIZE,
        false
    )

    val startTime = SystemClock.uptimeMillis()
    val result = faceClassifier.recognizeImage(croppedFace, true)
    val lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime

    result?.let {
        val conf = it.distance ?: 1.0f
        if (conf < 1.0f) {
            val paint = Paint().apply {
                color = if (it.id == "0") android.graphics.Color.GREEN else android.graphics.Color.RED
                textSize = 104f
            }
            canvas.drawText(
                "${it.title} $conf",
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                paint
            )
            onResult("'${it.title}'")
        } else {
            onResult(null)
        }
    } ?: onResult(null)
}

private const val TF_OD_API_INPUT_SIZE = 160