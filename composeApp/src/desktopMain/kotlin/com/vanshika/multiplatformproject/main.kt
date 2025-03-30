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
import io.ktor.utils.io.core.use
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
import org.apache.poi.ss.usermodel.CellType
import java.sql.DriverManager.println
import javax.swing.UIManager.put
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jetbrains.skia.impl.Stats.enabled

fun main() = application {
    val answer = "Student's Answer Here"
    val rubric = "Rubric Criteria Here"
    val selectedFiles = mapOf("Rubric" to "path/to/rubric.xlsx")
    Window(onCloseRequest = ::exitApplication, title = "MultiPlatformProject") {
        DesktopApp(answer, rubric, selectedFiles)
    }
}

@Composable
@Preview
fun DesktopApp(
    answer: String,
    rubric: String,
    selectedFiles: Map<String, String>
) {
    var selectedFiles by remember { mutableStateOf(mapOf<String, String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var fileContent by remember { mutableStateOf("") }
//    var feedbackContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackContent by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        evaluateAnswerSheet(answer, rubric, selectedFiles) { result ->
            feedbackContent = result
        }
    }
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

                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD4A017))
                ) {
                    Text("Show Selected Files", color = Color.White)
                }

                Button(
                    onClick = {
                        val answer = readFileContent(selectedFiles["Answer Sheet"] ?: "")
                        val question = readFileContent(selectedFiles["Exam Paper"] ?: "")
                        val rubric = readFileContent(selectedFiles["Rubric"] ?: "")

                        evaluateAnswerSheet(
                            answerSheet = answer,
                            rubric = rubric,
                            selectedFiles = selectedFiles  // Pass the map here
                        ) { result ->
                            feedbackContent = result
                        }
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF0000)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Mark Report", color = Color.White)
                    }
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
                                    modifier = Modifier.clickable {
                                        openSelectedFile(path); showDialog = false
                                    }
                                        .padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = { Button(onClick = { showDialog = false }) { Text("Close") } }
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Student Report",
                            fontSize = 16.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(fileContent, fontSize = 14.sp)
                    }
                }

//                Column(modifier = Modifier.padding(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "AI Score & Feedback",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (feedbackContent == null) {
                            CircularProgressIndicator() // Show loading indicator
                        } else {
                            // Parse JSON before UI rendering
                            val jsonResponse = try {
                                JSONObject(feedbackContent ?: "{}") // Avoid null exceptions
                            } catch (e: Exception) {
                                null // Return null if parsing fails
                            }

                            if (jsonResponse != null) {
                                DisplayFeedback(jsonResponse) // ✅ Safe to call composable
                            } else {
                                Text(text = "Error: Could not parse response.") // ✅ UI remains valid
                            }

                        }
                    }
                }
            }
//                Card(modifier = Modifier.weight(1f).fillMaxSize(), shape = RoundedCornerShape(8.dp), elevation = 4.dp) {
//                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
//                        Text("AI Score & Feedback", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(feedbackContent, fontSize = 14.sp)
//                    }
//                }
        }
    }
}


@Composable
fun DisplayFeedback(jsonResponse: JSONObject) {
    // Ensure we are not using NaN, defaulting to 0 if invalid
    var adjustedScore by remember {
        mutableStateOf(
            jsonResponse.optDouble("overall_score").takeIf { !it.isNaN() }?.toFloat() ?: 0f
        )
    }
    var showToast by remember { mutableStateOf(false) } // State for triggering toast

    Column(modifier = Modifier.padding(16.dp)) {

        Text(
            "🔢 Overall Score: ${jsonResponse.optString("overall_score")}",
            fontSize = 18.sp,
            color = Color.Blue
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("✅ Strengths:", fontWeight = FontWeight.Bold)
        jsonResponse.optJSONArray("strengths")?.let {
            for (i in 0 until it.length()) {
                Text("- ${it.getString(i)}")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("⚠ Improvement Areas:", fontWeight = FontWeight.Bold)
        jsonResponse.optJSONArray("improvement_areas")?.let {
            for (i in 0 until it.length()) {
                Text("- ${it.getString(i)}", color = Color.Red)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Slider for adjusting the overall score
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔢 Overall Score: ${String.format("%.1f", adjustedScore)}",
                fontSize = 18.sp,
                color = Color.Blue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slider for adjusting the overall score
            Text("🎛 Adjust Final Score")
            Slider(
                value = adjustedScore,
                onValueChange = { adjustedScore = it },
                valueRange = 0f..100f,  // Assuming 0 to 100 scale
                steps = 99,  // Defines step intervals for smooth adjustment
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm button
            Button(
                onClick = {
                    println("Confirmed Score: $adjustedScore")
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Confirm Score")
            }
        }
        jsonResponse.optJSONArray("section_wise")?.let { sections ->
            for (i in 0 until sections.length()) {
                val section = sections.getJSONObject(i)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📌 Section: ${section.getString("section")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text("📊 Score: ${section.getString("section_score")}", color = Color.Magenta)

                section.optJSONArray("criteria")?.let { criteria ->
                    for (j in 0 until criteria.length()) {
                        val crit = criteria.getJSONObject(j)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "🔹 ${crit.getString("criterion")}: ${crit.getString("score")} (${
                                crit.getString(
                                    "achieved_level"
                                )
                            })"
                        )
                        Text("   - ${crit.getString("feedback")}")
                    }
                }
            }
        }
    }

}
fun evaluateAnswerSheet(
    answerSheet: String,
    rubric: String,
    selectedFiles: Map<String, String>,
    onResult: (String) -> Unit
) {
    val parsedRubric =
        if (rubric.contains("\t")) parseExcelRubric(
            File(
                selectedFiles["Rubric"] ?: ""
            )
        ) else rubric
    val client = OkHttpClient()
    val apiKey = "AIzaSyACAhaIxIrz1mqt6gyz4c51g0xhCuKQOTc" // Verify this key is valid
    val url =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent?key=$apiKey"
    // Truncate inputs to reasonable lengths (though 1.5 Pro supports up to 2M tokens)
    val truncatedRubric = parsedRubric.take(100000) // ~100k characters
    val truncatedAnswer = answerSheet.take(500000) // ~500k characters

    val prompt = """
    ROLE: Expert academic evaluator analyzing Excel-based rubrics
    
    TASK:
    1. Parse the tabular rubric data
    2. Extract evaluation criteria and scoring guidelines
    3. Evaluate answer against each criterion
    4. Return JSON response with scores
    
    RUBRIC:
        ${if (truncatedRubric.startsWith("ERROR")) "INVALID RUBRIC FORMAT" else truncatedRubric}
        
        STUDENT ANSWER:
        $truncatedAnswer
        
    RESPONSE FORMAT:
    {
        "overall_score": "X/100",
        "section_wise": [
            {
                "section": "Section Name",
                "criteria": [
                    {
                        "criterion": "Criterion Name",
                        "score": "X/Y",
                        "feedback": "Specific comments",
                        "achieved_level": "Excellent/Good/Fair/Needs improvement"
                    }
                ],
                "section_score": "X/Y"
            }
        ],
        "strengths": [],
        "improvement_areas": []
    }
    
    INSTRUCTIONS:
    - Match answer content to rubric levels
    - Assign scores based on achieved level
    - Provide detailed feedback for each criterion
    - Convert all rubric percentages to scores
""".trimIndent()

    val requestBody = JSONObject().apply {
        put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", prompt)
                    })
                })
            })
        })
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.3) // Lower for consistent scoring
            put("topP", 0.8)
            put("topK", 40)
            put("maxOutputTokens", 4096)
            put("responseMimeType", "application/json")
        })
        put("safetySettings", JSONArray().apply {
            put(JSONObject().apply {
                put("category", "HARM_CATEGORY_HATE_SPEECH")
                put("threshold", "BLOCK_NONE")
            })
            put(JSONObject().apply {
                put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                put("threshold", "BLOCK_NONE")
            })
        })
    }.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult("Network Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string() ?: ""
            try {
                val jsonResponse = JSONObject(responseBody)

                // Handle API errors
                if (jsonResponse.has("error")) {
                    val error = jsonResponse.getJSONObject("error")
                    onResult("API Error (${error.optInt("code")}): ${error.getString("message")}")
                    return
                }

                // Parse successful response
                val candidates = jsonResponse.optJSONArray("candidates") ?: JSONArray()
                if (candidates.length() == 0) {
                    onResult("Error: No evaluation returned from API")
                    return
                }

                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() == 0) {
                    onResult("Error: No evaluation content found")
                    return
                }

                val text = parts.getJSONObject(0).getString("text")

                // Try to pretty-print the JSON
                try {
                    val json = JSONObject(text)
                    onResult(json.toString(4))
                } catch (e: Exception) {
                    // If not valid JSON, return as-is with note
                    onResult("Received non-JSON response:\n$text")
                }

            } catch (e: Exception) {
                onResult(
                    """
                    Error parsing response: ${e.message}
                    
                    Raw Response (first 2000 chars):
                    ${responseBody.take(2000)}${if (responseBody.length > 2000) "\n[...truncated]" else ""}
                """.trimIndent()
                )
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
    return when (file.extension.lowercase()) {
        "pdf" -> readPdfContent(file)
        "docx" -> readDocxContent(file)
        "xlsx" -> parseExcelRubric(file)
        "txt", "csv", "json" -> file.readText()
        else -> try {
            file.readText()
        } catch (e: Exception) {
            "UNSUPPORTED FILE FORMAT: ${file.extension}"
        }
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

fun readExcelContent(file: File): String {
    return try {
        FileInputStream(file).use { fis ->
            val workbook = XSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)
            val data = StringBuilder()

            for (row in sheet) {
                for (cell in row) {
                    when (cell.cellType) {
                        CellType.STRING -> data.append(cell.stringCellValue)
                        CellType.NUMERIC -> data.append(cell.numericCellValue)
                        CellType.BOOLEAN -> data.append(cell.booleanCellValue)
                        else -> data.append("")
                    }
                    data.append("\t")
                }
                data.append("\n")
            }
            workbook.close()
            data.toString()
        }
    } catch (e: Exception) {
        "ERROR READING EXCEL: ${e.message}"
    }
}

fun parseExcelRubric(file: File): String {
    return try {
        FileInputStream(file).use { fis ->
            val workbook = XSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)
            val parsedRubric = StringBuilder()

            var currentSection = ""

            for (row in sheet) {
                val cells = row.map { cell ->
                    when (cell.cellType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> cell.numericCellValue.toString()
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> ""
                    }
                }

                // Parse section headers
                if (cells.size > 1 && cells[1].isNotBlank()) {
                    currentSection = cells[1]
                }
                // Parse criteria rows
                else if (cells.size > 3 && cells[0].isNotBlank() && cells[2].isNotBlank()) {
                    parsedRubric.append(
                        """
                    | Criteria: ${cells[1]}
                    | Max Score: ${cells[2]}
                    | Excellent: ${cells.getOrNull(3) ?: ""}
                    | Good: ${cells.getOrNull(4) ?: ""}
                    | Fair: ${cells.getOrNull(5) ?: ""}
                    | Needs Improvement: ${cells.getOrNull(6) ?: ""}
                    |---
                    """.trimMargin()
                    )
                }
            }
            workbook.close()
            parsedRubric.toString()
        }
    } catch (e: Exception) {
        "ERROR PARSING EXCEL RUBRIC: ${e.message}"
    }
}