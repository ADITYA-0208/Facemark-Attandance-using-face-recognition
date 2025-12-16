package com.example.facemark.screens





import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.facemark.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

private const val TF_OD_API_INPUT_SIZE = 160

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // State variables
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognitionResults by remember { mutableStateOf<String?>(null) }

    // Date state
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Dialog states
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Dropdown states
    val subjects = listOf("Subject 1", "Subject 2", "Subject 3")
    var selectedSubject by remember { mutableStateOf(subjects[0]) }
    var expandedSubject by remember { mutableStateOf(false) }

    val batches = listOf("F1", "F2", "F3","F4", "F5", "F6","F7", "F8", "F9")
    var selectedBatch by remember { mutableStateOf(batches[0]) }
    var expandedBatch by remember { mutableStateOf(false) }



    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = { navController.navigate(Screen.RecognitionScreen.route) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
        ) {
            Text("Mark Attendance")
        }
        Button(
            onClick = { navController.navigate(Screen.RegisterScreen.route) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
        ) {
            Text("Register")
        }
        }
    }


