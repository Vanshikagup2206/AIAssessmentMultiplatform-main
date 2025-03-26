package com.vanshika.multiplatformproject

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.sql.DriverManager.println

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MultiPlatformProject"
    ) {
        DesktopApp()
    }
}

@Composable
@Preview
fun DesktopApp() {
    var selectedFiles by remember { mutableStateOf(mapOf<String, String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") } // Stores the content of the file

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFADD8E6)) // Light Blue Background
                .padding(16.dp)
        ) {
            // TopAppBar
            TopAppBar(
                backgroundColor = Color(0xFF3B83BD), // Blue background
                contentColor = Color.White
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "EDUMARK-AI: TRANSFORMING SCORING AND FEEDBACK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Rubric", "Exam Paper", "Answer Sheet").forEach { label ->
                    Button(
                        onClick = {
                            val file = openFileDialog(label)
                            if (file != null) {
                                selectedFiles = selectedFiles + (label to file)
                            }
                            if (label == "Answer Sheet" && file != null) { // Check if file is not null
                                fileContent = readFileContent(file) // Read and store file content
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EA)) // Purple
                    ) {
                        Text(text = label, color = Color.White)
                    }
                }

                // Show Selected Files Button
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD4A017)) // Mustard Color
                ) {
                    Text("Show Rubric/Question Paper/Answer Sheet", color = Color.White)
                }

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF0000)) // Red Color
                ) {
                    Text("Mark Report", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dialog to Choose Which File to Open
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Select a file to open") },
                    text = {
                        Column {
                            selectedFiles.forEach { (label, path) ->
                                Text(
                                    text = "$label: ${File(path).name}",  // Display file name
                                    modifier = Modifier
                                        .clickable {
                                            openSelectedFile(path) // Open file directly
                                            showDialog = false
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }



            // Main Panels
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                // Left Panel: Student Report
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Student Report", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
//                        Text("Introduction", fontSize = 14.sp, color = Color.Gray)
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text("This is a sample AI evaluation report...", fontSize = 14.sp)
                        Text("Answer Sheet Content:", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(fileContent, fontSize = 14.sp) // Show file content here
                    }
                }

                // Right Panel: AI Score & Feedback
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Score & Feedback", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Introduction Feedback", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Detailed feedback on the student's report...", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Score: 10/15", fontSize = 14.sp)
                Button(onClick = { /* TODO: Confirm Score */ }) {
                    Text("Confirm")
                }
            }
        }
    }
}
fun readFileContent(filePath: String): String {
    return try {
        File(filePath).readText() // Read entire file as text
    } catch (e: Exception) {
        "Error reading file: ${e.message}"
    }
}

// Function to Open File Explorer
fun openFileDialog(title: String): String? {
    val fileDialog = FileDialog(null as Frame?, "Select $title", FileDialog.LOAD)
    fileDialog.isVisible = true
    return if (fileDialog.file != null) {
        File(fileDialog.directory, fileDialog.file).absolutePath
    } else null
}

fun openSelectedFile(filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file) // Open file directly using default app
            } else {
                println("Desktop operations not supported on this system.")
            }
        } else {
            println("File does not exist: $filePath")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}