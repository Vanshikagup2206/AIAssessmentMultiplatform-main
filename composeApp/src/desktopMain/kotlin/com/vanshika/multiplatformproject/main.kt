package com.vanshika.multiplatformproject

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
import javax.swing.UIManager.put

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MultiPlatformProject") {
        DesktopApp()
    }
}

@Composable
@Preview
fun DesktopApp() {
    var selectedFiles by remember { mutableStateOf(mapOf<String, String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var fileContent by remember { mutableStateOf("") }
    var feedbackContent by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFADD8E6))
                .padding(16.dp)
        ) {
            TopAppBar(
                backgroundColor = Color(0xFF3B83BD),
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
                                if (label == "Answer Sheet") {
                                    fileContent = readFileContent(file)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EA))
                    ) {
                        Text(text = label, color = Color.White)
                    }
                }

                Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD4A017))) {
                    Text("Show Selected Files", color = Color.White)
                }

                Button(onClick = {
                    val answer = readFileContent(selectedFiles["Answer Sheet"] ?: "")
                    val question = readFileContent(selectedFiles["Exam Paper"] ?: "")
                    val rubric = readFileContent(selectedFiles["Rubric"] ?: "")

                    evaluateAnswerSheet(answer, rubric) { result ->
                        feedbackContent = result
                    }
                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF0000))) {
                    Text("Mark Report", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Selected Files") },
                    text = {
                        Column {
                            selectedFiles.forEach { (label, path) ->
                                Text(
                                    text = "$label: ${File(path).name}",
                                    modifier = Modifier.clickable { openSelectedFile(path); showDialog = false }
                                        .padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = { Button(onClick = { showDialog = false }) { Text("Close") } }
                )
            }

            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f).fillMaxSize(), shape = RoundedCornerShape(8.dp), elevation = 4.dp) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("Student Report", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(fileContent, fontSize = 14.sp)
                    }
                }

                Card(modifier = Modifier.weight(1f).fillMaxSize(), shape = RoundedCornerShape(8.dp), elevation = 4.dp) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("AI Score & Feedback", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(feedbackContent, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

fun evaluateAnswerSheet(answerSheet: String, rubric: String, onResult: (String) -> Unit) {
    val client = OkHttpClient()
    val apiKey = "AIzaSyCpRpmUSkhZnzUPbFvxDxQUJXKMMrDlAlc"
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateText?key=$apiKey"

    val prompt = """
        Evaluate the following answer based on the rubric provided.
        Return scores and feedback in JSON format.
        
        **Rubric:** $rubric  
        **Answer:** $answerSheet
    """.trimIndent()

    val jsonBody = JSONObject().put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
    val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder().url(url).post(requestBody).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult("Error: ${'$'}{e.message}")
        }
        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string()
            println("Raw API Response: $responseBody") // Debugging ke liye print karein

            try {
                val jsonResponse = JSONObject(responseBody ?: "{}")

                // Pehle check karein ki "evaluation" array exist karta hai ya nahi
                val evaluationArray = jsonResponse.optJSONArray("evaluation") ?: JSONArray()
                val feedbackList = mutableListOf<String>()

                for (i in 0 until evaluationArray.length()) {
                    val section = evaluationArray.optJSONObject(i) ?: continue  // Null check
                    val sectionName = section.optString("section", "Unknown Section")
                    val feedback = section.optString("feedback", "No Feedback")
                    val score = section.optInt("score", 0)

                    feedbackList.add("$sectionName: $feedback (Score: $score/10)")
                }

                val overallScore = jsonResponse.optInt("overall_score", 0)
                onResult("AI Evaluation:\n${feedbackList.joinToString("\n")}\nOverall Score: $overallScore/40")

            } catch (e: Exception) {
                onResult("Error parsing AI response: ${e.message}\nRaw Response:\n$responseBody")
            }
        }

    })
}


fun openFileDialog(title: String): String? {
    val fileDialog = FileDialog(null as Frame?, "Select $title", FileDialog.LOAD)
    fileDialog.isVisible = true
    return fileDialog.file?.let { File(fileDialog.directory, it).absolutePath }
}

fun readFileContent(filePath: String): String {
    val file = File(filePath)
    return when {
        file.extension.equals("docx", ignoreCase = true) -> readDocxContent(file)
        file.extension.equals("pdf", ignoreCase = true) -> readPdfContent(file)
        else -> "Unsupported file format: ${file.extension}"
    }
}

fun readDocxContent(file: File): String {
    return try {
        FileInputStream(file).use { fis ->
            val doc = XWPFDocument(fis)
            doc.paragraphs.joinToString("\n") { it.text }
        }
    } catch (e: Exception) {
        "Error reading DOCX file: ${e.message}"
    }
}

fun readPdfContent(file: File): String {
    return try {
        PDDocument.load(file).use { doc ->
            PDFTextStripper().getText(doc)
        }
    } catch (e: Exception) {
        "Error reading PDF file: ${e.message}"
    }
}

fun openSelectedFile(filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists() && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
