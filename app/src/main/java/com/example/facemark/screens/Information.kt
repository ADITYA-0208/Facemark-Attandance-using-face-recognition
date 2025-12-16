package com.example.facemark.screens




import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.facemark.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance() // Initialize Firestore

    // State for selected date
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    // For the confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmFetch by remember { mutableStateOf(false) }

    // State for Dropdown menu selection
    val options = listOf("Subject 1", "Subject 2", "Subject 3")
    var selectedOption by remember { mutableStateOf(options[0]) }
    var expanded by remember { mutableStateOf(false) }

    // State for Batch Dropdown menu selection
    val batches = listOf("F1", "F2", "F3","F4", "F5", "F6","F7", "F8", "F9")
    var selectedBatch by remember { mutableStateOf(batches[0]) }
    var expandedBatch by remember { mutableStateOf(false) }

    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    var fetchedData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }


    // Coroutine scope for suspend function calls
    val scope = rememberCoroutineScope()

    // Load batches (Firestore collections) on component composition
    LaunchedEffect(confirmFetch) {
        if (confirmFetch) {
            scope.launch {
                FetchFromFirestore(firestore, selectedBatch, selectedOption, selectedDate) { data ->
                    fetchedData = data // Store the fetched data
                }
            }
            confirmFetch = false
        }
    }

    // DatePickerDialog setup
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            showDatePicker = false // Dismiss the DatePicker
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Information Screen",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontFamily = FacemarkFont
                    )
                }
            )
        }
    ) { paddingValues ->
        // UI Layout with Scrollable Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()), // Make screen scrollable
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

             // Show the DatePickerDialog when triggered
            if (showDatePicker) {
                datePickerDialog.show()
            }

            // Display selected date
            TextButton(onClick = { showDatePicker = true }) {
                Text(
                    text = "Selected Date: $selectedDate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF808080),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Dropdown Menu for selecting a subject
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
                                    selectedOption = option // Set the selected option
                                    expanded = false // Close the dropdown
                                },
                                text = {
                                    Text(option) // Pass the Text composable explicitly as 'text'
                                }
                            )
                        }
                    }
                }
            }


            // Dropdown Menu for selecting a batch
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
                                    selectedBatch = batch // Set the selected batch
                                    expandedBatch = false // Close the dropdown
                                },
                                text = {
                                    Text(batch)
                                }
                            )
                        }
                    }
                }
            }

            // Button to confirm fetching data
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
            ) {
                Text("Confirm")
            }

            // Confirmation Dialog
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Confirm Action") },
                    text = { Text("Are you sure you want to fetch data?") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmFetch = true // Set to true to trigger data fetch
                            showConfirmDialog = false
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Display fetched data
// Display fetched data with document IDs
            if (fetchedData.isNotEmpty()) {
                Text(
                    text = "Fetched Data:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF808080),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable LazyColumn to display data in a neat table-like format
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .weight(1f)
                    // Ensures the LazyColumn takes up remaining space
                ) {
                    items(fetchedData.entries.toList()) { (documentId, data) ->
                        // Header row for each student
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Student Name:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF808080),
                                modifier = Modifier.padding(4.dp)
                            )
                            Text(
                                text = documentId,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(4.dp)
                            )
                        }

                        // Divider for better separation
                        Divider(color = Color.Gray, thickness = 1.dp)

                        // Row for each key-value pair (e.g., Date and Attendance)
                        data.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    color = Color(0xFF808080),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = value.toString(),
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
// Function to fetch from Firestore based on selected batch, subject, and date
suspend fun FetchFromFirestore(
    firestore: FirebaseFirestore,
    selectedBatch: String,
    selectedSubject: String,
    selectedDate: String,
    onDataFetched: (Map<String, Map<String, Any>>) -> Unit
) {
    try {
        // Create the collection reference using the selected batch
        val collectionRef = firestore.collection(selectedBatch)

        // Fetch all documents in the selected batch collection
        val querySnapshot = collectionRef.get().await()

        // Initialize a mutable map to store document IDs and their matching fields
        val documentData = mutableMapOf<String, Map<String, Any>>()

        // Loop through each document in the collection
        for (document in querySnapshot.documents) {
            val data = document.data?.filterKeys { key ->
                key.startsWith(selectedDate) && key.endsWith("_$selectedSubject")
            } ?: emptyMap()

            // If the document has matching data, add the document ID and data to the map
            if (data.isNotEmpty()) {
                documentData[document.id] = data
            }
        }

        // If matching data is found, pass it to the callback
        if (documentData.isNotEmpty()) {
            onDataFetched(documentData)
            Log.d("Firestore", "Data fetched: $documentData")
        } else {
            onDataFetched(emptyMap())
            Log.d("Firestore", "No matching data found")
        }
    } catch (e: Exception) {
        onDataFetched(emptyMap())
        Log.w("Firestore", "Error fetching documents: ${e.message}", e)
    }
}
