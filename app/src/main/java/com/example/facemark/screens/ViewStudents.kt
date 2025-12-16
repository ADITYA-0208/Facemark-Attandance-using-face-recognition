package com.example.facemark.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.facemark.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ViewStudents(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Student Details", "Add Student")
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> StudentDetailsTab()
            1 -> AddStudentTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsTab() {
    val firestore = FirebaseFirestore.getInstance()
    var selectedBatch by remember { mutableStateOf("F1") } // default batch
    var studentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Students Screen", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,color = Color.Black, fontFamily =FacemarkFont)}
            )
        }
    ){paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(paddingValues)
    ) {
        // Dropdown to select batch
        Text("Select Batch:", style = MaterialTheme.typography.titleMedium)
        DropdownMenuBox(
            options = listOf("F1", "F2", "F3","F4", "F5", "F6","F7", "F8", "F9"),
            selectedOption = selectedBatch,
            onOptionSelected = { selectedBatch = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Fetch Students Button
        Button(
            onClick = {
                scope.launch {
                    studentNames = fetchStudentNames(firestore, selectedBatch)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Fetch Students", style = MaterialTheme.typography.bodyLarge)
        }
    }
        Spacer(modifier = Modifier.height(16.dp))

        // Display student document names in Cards
        studentNames.forEach { documentName ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = documentName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentTab() {
    val firestore = FirebaseFirestore.getInstance()
    var selectedBatch by remember { mutableStateOf("F1") }
    var studentName by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "View Students Screen",
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
                .padding(paddingValues )
        ) {

            Text("Select Batch:", style = MaterialTheme.typography.titleMedium)
            DropdownMenuBox(
                options = listOf("F1", "F2", "F9"),
                selectedOption = selectedBatch,
                onOptionSelected = { selectedBatch = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                label = { Text("Student Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        addStudentToBatch(firestore, selectedBatch, studentName.text)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Student", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun DropdownMenuBox(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedOption, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                CustomDropdownMenuItem(
                    text = option,
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CustomDropdownMenuItem(
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        onClick = onClick,
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) }
    )
}




suspend fun fetchStudentNames(firestore: FirebaseFirestore, batch: String): List<String> {
    return try {
        val querySnapshot = firestore.collection(batch).get().await()
        // Map each document to its ID (document name)
        querySnapshot.documents.map { it.id }
    } catch (e: Exception) {
        emptyList()
    }
}


// Function to add a new student document to the selected batch collection
suspend fun addStudentToBatch(firestore: FirebaseFirestore, batch: String, studentName: String) {
    try {
        // Use studentName as the document ID in the Firestore collection
        firestore.collection(batch)
            .document(studentName) // Sets the document ID to the student's name
            .set(mapOf("name" to studentName)) // Set fields inside the document
    } catch (e: Exception) {
        // Handle error (e.g., log error or show a message to the user)
    }
}


