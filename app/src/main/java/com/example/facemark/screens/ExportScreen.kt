package com.example.facemark.screens




import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.facemark.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var startDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var endDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    val subjects = listOf("Subject 1", "Subject 2", "Subject 3")
    var selectedSubject by remember { mutableStateOf(subjects[0]) }
    var expandedSubject by remember { mutableStateOf(false) }

    val batches = listOf("F1", "F2", "F3","F4", "F5", "F6","F7", "F8", "F9")
    var selectedBatch by remember { mutableStateOf(batches[0]) }
    var expandedBatch by remember { mutableStateOf(false) }

    var fetchedData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    var attendancePercentage by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var isExporting by remember { mutableStateOf(false) }
    var attendanceCountMap by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()


    // Function to open the DatePickerDialog for start and end dates
    fun openDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) startDate else endDate
        val calendarInstance = Calendar.getInstance().apply {
            time = dateFormat.parse(currentDate) ?: Date()
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                if (isStartDate) {
                    startDate = formattedDate
                } else {
                    endDate = formattedDate
                }
            },
            calendarInstance.get(Calendar.YEAR),
            calendarInstance.get(Calendar.MONTH),
            calendarInstance.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Function to export attendance data to CSV
    fun exportAttendanceToCSV(
        context: Context,
        subject: String,
        startDate: String,
        endDate: String,
        attendanceCountMap: Map<String, Pair<Int, Int>>
    ) {
        val csvFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "$subject-Attendance.csv"
        )
        csvFile.printWriter().use { out ->
            // Adding a clear header section with start and end dates
            out.println("Attendance Report for $subject")
            out.println("Start Date: $startDate")
            out.println("End Date: $endDate")
            out.println() // Blank line for separation

            // Adding headers for the CSV data
            out.println("Student Name, Attended, Total, Attendance ")

            // Writing student data
            attendanceCountMap.forEach { (studentName, count) ->
                val (attended, total) = count
                val percentage = if (total > 0) (attended.toFloat() / total * 100) else 0f
                out.println(
                    "$studentName, $attended, $total, ${
                        String.format(
                            "%.2f",
                            percentage
                        )
                    }%"
                )
            }
        }
        shareFile(context, csvFile)
    }


    // Function to fetch data and calculate attendance percentage
    suspend fun fetchDataAndCalculatePercentage() {
        try {
            val result = mutableMapOf<String, Map<String, Any>>()
            FetchFromFirestoreexport(
                firestore,
                selectedBatch,
                selectedSubject,
                startDate,
                endDate
            ) { data ->
                result.putAll(data)
            }
            fetchedData = result

            // Calculate attendance percentage
            val percentageMap = mutableMapOf<String, Float>()
            val attendanceCountMap = mutableMapOf<String, Pair<Int, Int>>() // (attended, total)

            // Iterate over each student's attendance data
            fetchedData.forEach { (studentName, attendanceRecords) ->
                var totalClasses = 0
                var attendedClasses = 0

                // Count total classes and attended classes for each subject-specific record
                attendanceRecords.forEach { (recordKey, recordValue) ->
                    // Filter records that are specific to the selected subject
                    if (recordKey.endsWith("_$selectedSubject")) {
                        totalClasses++ // Increment total classes for each record found

                        // Check if the student attended the class
                        when (recordValue) {
                            is String -> {
                                if (recordValue.equals("true", ignoreCase = true)) attendedClasses++
                            }

                            is Boolean -> {
                                if (recordValue) attendedClasses++
                            }
                        }
                    }
                }

                // Calculate percentage based on attended and total classes
                val percentage = if (totalClasses > 0) {
                    (attendedClasses.toFloat() / totalClasses) * 100
                } else {
                    0f
                }

                percentageMap[studentName] = percentage
                attendanceCountMap[studentName] = attendedClasses to totalClasses
            }

            attendancePercentage = percentageMap

            // Generate CSV with updated logic
            exportAttendanceToCSV(context, selectedSubject, startDate, endDate, attendanceCountMap)
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching data: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Export Screen",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontFamily = FacemarkFont
                    )
                }
            )
        }
    ) { paddingValues ->
        // UI for the Export Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp).padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subject Dropdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Select Subject: ", modifier = Modifier.padding(end = 8.dp))
                Box {
                    TextButton(onClick = { expandedSubject = true }) {
                        Text(text = selectedSubject)
                    }
                    DropdownMenu(
                        expanded = expandedSubject,
                        onDismissRequest = { expandedSubject = false }) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedSubject = subject
                                    expandedSubject = false
                                },
                                text = { Text(subject) }
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Batch Dropdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Select Batch: ", modifier = Modifier.padding(end = 8.dp))
                Box {
                    TextButton(onClick = { expandedBatch = true }) {
                        Text(text = selectedBatch)
                    }
                    DropdownMenu(
                        expanded = expandedBatch,
                        onDismissRequest = { expandedBatch = false }) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Start Date Picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start Date: ", modifier = Modifier.padding(end = 8.dp))
                Button(onClick = { openDatePicker(true) }) {
                    Text(startDate)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // End Date Picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("End Date: ", modifier = Modifier.padding(end = 8.dp))
                Button(onClick = { openDatePicker(false) }) {
                    Text(endDate)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fetch Data Button
            Button(onClick = {
                isExporting = true
                coroutineScope.launch {
                    fetchDataAndCalculatePercentage()
                    isExporting = false
                }
            }) {
                Text("Fetch Data")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Options if Data Exists
            if (attendancePercentage.isNotEmpty()) {
                Text("Export as:", fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = {
                        // Pass the context, selectedSubject, and attendanceCountMap
                        exportAttendanceToCSV(
                            context,
                            selectedSubject,
                            startDate,
                            endDate,
                            attendanceCountMap
                        )
                    }) {
                        Text("CSV")
                    }
                }
            }

            if (isExporting) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}

// Firestore fetch function (modified for date range)
suspend fun FetchFromFirestoreexport(
    firestore: FirebaseFirestore,
    selectedBatch: String,
    selectedSubject: String,
    startDate: String,
    endDate: String,
    onDataFetched: (Map<String, Map<String, Any>>) -> Unit
) {
    try {
        val querySnapshot = firestore.collection(selectedBatch).get().await()
        val result = mutableMapOf<String, Map<String, Any>>()

        val start = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(startDate)
        val end = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(endDate)

        querySnapshot.documents.forEach { document ->
            val data = document.data?.filterKeys { key ->
                val datePart = key.substringBefore("_")
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(datePart)
                date in start..end && key.endsWith("_$selectedSubject")
            } ?: emptyMap()

            if (data.isNotEmpty()) result[document.id] = data
        }
        onDataFetched(result)
    } catch (e: Exception) {
        Log.w("Firestore", "Error: ${e.message}", e)
        onDataFetched(emptyMap())
    }
}
fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = when (file.extension) {
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Important for granting access
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}
