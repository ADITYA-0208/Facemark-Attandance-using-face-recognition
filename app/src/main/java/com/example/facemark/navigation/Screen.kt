package com.example.facemark.navigation



sealed class Screen(val route: String) {
    object AttendanceScreen : Screen("Attendance_Screen")
    object InformationScreen : Screen("Information_Screen")
    object HomeScreen : Screen("home_screen")
    object EditingScreen : Screen("editing_screen")
    object CreateScreen : Screen("Create_screen")
    object LoginScreen : Screen("Login_screen")
    object ProfileScreen : Screen("Profile_screen")
    object ExportScreen : Screen("Export_screen")
    object ViewStudents : Screen("View_Students")
    object RegisterScreen : Screen("Register_screen")
    object RecognitionScreen : Screen("Recognition_Screen")
    object FirstScreen : Screen("First_Screen")
    object SecondScreen : Screen("Second_Screen")

}