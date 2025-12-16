package com.example.facemark.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.facemark.R
import com.example.facemark.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showLogoutDialog by remember { mutableStateOf(false) }
    val FacemarkFont = FontFamily(
        Font(R.font.panton_black_caps, FontWeight.Normal)
    )
    // Function to handle logout (this is just a placeholder, customize as needed)
    fun handleLogout() {
        // TODO: Add your logout logic here (e.g., FirebaseAuth.getInstance().signOut())
        navController.navigate(Screen.LoginScreen.route) {
            popUpTo(Screen.HomeScreen.route) { inclusive = true } // Clears the backstack
        }
    }

    if (showLogoutDialog) {
        // Confirmation Dialog for Logout
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    handleLogout()
                    showLogoutDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Logout Confirmation") },
            text = { Text("Are you sure you want to logout?") }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Drawer content (Slider Menu)
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // User Image (Default Person Icon)
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Image",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { /* TODO: Handle menu item click */ }) {
                        Text("Menu Item 1")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { /* TODO: Handle menu item click */ }) {
                        Text("Menu Item 2")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Logout Button
                    Button(
                        onClick = {
                            showLogoutDialog = true // Show confirmation dialog
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Logout")
                    }
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {  Text("Face Mark", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,color = Color.Black, fontFamily = FacemarkFont)  },
                        actions = {
                            IconButton(onClick = {
                                // Navigate to ProfileScreen when the user icon is clicked
                                navController.navigate(Screen.ProfileScreen.route)
                            }) {
                                Icon(Icons.Default.Person, contentDescription = "User Profile")
                            }
                        }
                    )
                },
                content = { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Live Clock and Date at the top of the HomeScreen
                        var currentTime by remember { mutableStateOf(getCurrentTime()) }
                        var currentDate by remember { mutableStateOf(getCurrentDate()) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                currentTime = getCurrentTime()
                                kotlinx.coroutines.delay(1000L)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LaunchedEffect(Unit) {
                            currentDate = getCurrentDate() // Set the current date once
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentDate,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF808080),
                                textAlign = TextAlign.Center,
                                fontFamily = FacemarkFont,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF808080),
                                textAlign = TextAlign.Center,
                                fontFamily = FacemarkFont,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                        }

                        // Attendance and Information Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CardButton(
                                text = "Mark \n Attendance",
                                icon = Icons.Default.Add,
                                onClick = { navController.navigate(Screen.RecognitionScreen.route) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CardButton(
                                text = "View \n Attendance",
                                icon = Icons.Default.Monitor,
                                onClick = { navController.navigate(Screen.InformationScreen.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit Attendance and View Students Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CardButton(
                                text = "Edit \n Attendance",
                                icon = Icons.Default.Edit,
                                onClick = { navController.navigate(Screen.EditingScreen.route) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CardButton(
                                text = "Search \n Students",
                                icon = Icons.Default.Search,
                                onClick = {  navController.navigate(Screen.ViewStudents.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Export Attendance and Subjects Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CardButton(
                                text = "Export \n Attendance",
                                icon = Icons.Default.ArrowOutward,
                                onClick = { navController.navigate(Screen.ExportScreen.route) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            CardButton(
                                text = "Register",
                                icon = Icons.Default.Person,
                                onClick = { navController.navigate(Screen.RegisterScreen.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            )
        }
    )
}


// CardButton Composable that takes a text, an icon, and a click action
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .height(120.dp), // Increase height to fit icon and text vertically
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column( // Changed from Row to Column
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center, // Center the icon and text vertically
            horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
        ) {
            // Icon at the top
            Icon(imageVector = icon, contentDescription = text, tint = Color(0xFF00796B))

            // Text below the icon
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                textAlign = TextAlign.Center // Center the text under the icon
            )
        }
    }
}
fun getCurrentDate(): String {
    // Correct format to display date as "Day Number Month Name Year" (e.g., "18 September 2024")
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return sdf.format(Date())
}



fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    HomeScreen(navController = rememberNavController())
}
