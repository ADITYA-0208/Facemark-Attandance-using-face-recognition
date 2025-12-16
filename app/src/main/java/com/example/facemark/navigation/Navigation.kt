package com.example.facemark.navigation





import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.facemark.RecognitionScreen
import com.example.facemark.RegisterScreen
import com.example.facemark.screens.AttendanceScreen
import com.example.facemark.screens.CreateScreen
import com.example.facemark.screens.EditingScreen
import com.example.facemark.screens.ExportScreen
import com.example.facemark.screens.HomeScreen
import com.example.facemark.screens.InformationScreen
import com.example.facemark.screens.LoginScreen
import com.example.facemark.screens.ProfileScreen
import com.example.facemark.screens.ViewStudents
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun SetupNavGraph(
    navController: NavHostController,
    firestore: FirebaseFirestore,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // Retrieve login state from SharedPreferences
    val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val isLoggedIn = remember {
        sharedPreferences.getBoolean("isLoggedIn", false) && auth.currentUser != null
    }

    // Set HomeScreen as start if logged in, otherwise LoginScreen
    val startDestination = if (isLoggedIn) {
        Screen.HomeScreen.route
    } else {
        Screen.LoginScreen.route
    }

    NavHost(
        navController = navController,
        startDestination = Screen.LoginScreen.route, // ✅ make sure this exists in the enum or sealed class
        modifier = Modifier.fillMaxSize()
    ) {
        composable(route = Screen.HomeScreen.route) {
            HomeScreen(navController)
        }
        composable(route = Screen.AttendanceScreen.route) {
            AttendanceScreen(navController)
        }
        composable(route = Screen.InformationScreen.route) {
            InformationScreen(navController)
        }
        composable(route = Screen.EditingScreen.route) {
            EditingScreen(navController)
        }
        composable(route = Screen.LoginScreen.route) {
            LoginScreen(navController)
        }
        composable(route = Screen.CreateScreen.route) {
            CreateScreen(navController)
        }
        composable(route = Screen.ProfileScreen.route) {
            ProfileScreen(navController)
        }
        composable(route = Screen.ExportScreen.route) {
            ExportScreen(navController)
        }
        composable(route = Screen.ViewStudents.route) {
            ViewStudents(navController)
        }
        composable(route = Screen.RegisterScreen.route) {
            RegisterScreen(
                onFaceRegistered = {
                    navController.navigateUp()
                },
                onBackPressed = {
                    navController.navigateUp()
                },
                navController = navController
            )
        }
        composable(route = Screen.RecognitionScreen.route) {
            RecognitionScreen(navController)
        }
    }

}

