package com.example.facemark.screens





import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var showConfirmDialog by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val options = listOf("Subject 1", "Subject 2", "Subject 3")
    var selectedOption by remember { mutableStateOf(options[0]) }
    var expanded by remember { mutableStateOf(false) }

    val batches = listOf("F1", "F2", "F3","F4", "F5", "F6","F7", "F8", "F9")
    var selectedBatch by remember { mutableStateOf(batches[0]) }
    var expandedBatch by remember { mutableStateOf(false) }

    var fetchedData by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    var isDataEditable by remember { mutableStateOf(false) }
    var newDocumentName by remember { mutableStateOf("") }
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )

    suspend fun fetchData(
        firestore: FirebaseFirestore,
        selectedBatch: String,
        selectedSubject: String,
        selectedDate: String,
        onDataFetched: (Map<String, Map<String, Any>>) -> Unit
    ) {
        try {
            val collectionRef = firestore.collection(selectedBatch)
            val querySnapshot = collectionRef.get().await()

            val documentData = mutableMapOf<String, Map<String, Any>>()

            for (document in querySnapshot.documents) {
                val data = document.data?.filterKeys { key ->
                    key.startsWith(selectedDate) && key.endsWith("_$selectedSubject")
                } ?: emptyMap()

                if (data.isNotEmpty()) {
                    documentData[document.id] = data
                }
            }

            if (documentData.isEmpty()) {
                Log.d("Firestore", "No data found for the selected date and subject.")
            }

            onDataFetched(documentData)
        } catch (e: Exception) {
            onDataFetched(emptyMap())
            Log.w("Firestore", "Error fetching documents: ${e.message}", e)
        }
    }

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

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Action") },
            text = { Text("Are you sure you want to save the changes?") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        fetchedData.forEach { (documentId, fields) ->
                            fields.forEach { (fieldKey, fieldValue) ->
                                val fieldPath = FieldPath.of(*fieldKey.split(".").toTypedArray())

                                firestore.collection(selectedBatch)
                                    .document(documentId)
                                    .update(fieldPath, fieldValue)
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "DocumentSnapshot successfully updated!")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("Firestore", "Error updating document", e)
                                    }
                            }
                        }
                    }
                    showConfirmDialog = false
                }) {
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editing Screen",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontFamily = FacemarkFont
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clickable date text
            TextButton(onClick = { showDatePicker = true }) {
                Text(
                    text = "Selected Date: $selectedDate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF808080),
                    modifier = Modifier.padding(top = 16.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            if (isDataEditable) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    fetchedData.forEach { (docId, data) ->
                        Text(
                            text = "Student Name: $docId",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF808080),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        data.forEach { (fieldKey, fieldValue) ->
                            var selectedValue by remember { mutableStateOf(fieldValue.toString()) }
                            var expandedDropdown by remember { mutableStateOf(false) }

                            TextButton(onClick = { expandedDropdown = true }) {
                                Text(text = selectedValue, modifier = Modifier.fillMaxWidth())
                            }
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                listOf("True", "False").forEach { option ->
                                    DropdownMenuItem(
                                        onClick = {
                                            selectedValue = option
                                            expandedDropdown = false

                                            fetchedData = fetchedData.toMutableMap().apply {
                                                this[docId] = this[docId]?.toMutableMap()?.apply {
                                                    put(fieldKey, selectedValue)
                                                } ?: emptyMap()
                                            }
                                        },
                                        text = { Text(option) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Centered Edit Button
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                coroutineScope.launch {
                    fetchData(firestore, selectedBatch, selectedOption, selectedDate) { data ->
                        fetchedData = data
                        isDataEditable = true
                    }
                }
            }) {
                Text("Edit")
            }

            // Centered Confirm Edits Button
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showConfirmDialog = true }) {
                Text("Confirm Edits")
            }
        }
    }
}
