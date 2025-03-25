package com.vanshika.multiplatformproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material3.Slider


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MultiPlatformProject"
    ) {
        DesktopApp()
    }
}

@Composable
fun DesktopApp() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFB0D9F4.toInt())) // Light Blue Background
        ) {
            // Top Bar
            TopAppBar(
                title = { Text("EDUMARK-AI: Transforming Scoring and Feedback", fontSize = 18.sp) },
                backgroundColor = Color(0xFF0077C2.toInt()),
                contentColor = Color(0xFFFFFFFF.toInt())
            )

            // Main Content
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel (Student Report)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Student Report", fontSize = 16.sp, color = Color(0xFF000000.toInt()))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Introduction", fontSize = 14.sp, color = Color(0xFF808080.toInt()))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("This is a sample AI evaluation report...", fontSize = 14.sp)
                    }
                }

                // Right Panel (AI Score & Feedback)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Score & Feedback", fontSize = 16.sp, color = Color(0xFF000000.toInt()))
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreSection()
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreSection() {
    var score by remember { mutableStateOf(10f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Score: ${score.toInt()} / 15",
            fontSize = 16.sp,
            color = Color(0xFF000000.toInt())
        )

        Spacer(modifier = Modifier.height(8.dp))

//        Slider(
//            value = score,
//            onValueChange = { score = it },
////            valueRange = 0f.rangeTo(15f),
//            valueRange = ClosedFloatingPointRange(0f, 15f),
//
//                    modifier = Modifier.fillMaxWidth()
//        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* Handle score confirmation */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Confirm")
        }
    }
}





