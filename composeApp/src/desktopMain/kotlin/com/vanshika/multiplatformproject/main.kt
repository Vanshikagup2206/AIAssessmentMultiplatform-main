package com.vanshika.multiplatformproject

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog // This is crucial for Desktop
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.utils.io.core.use
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.json.JSONArray
import org.json.JSONObject
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.sql.DriverManager.println
import javax.swing.UIManager.put
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException

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
    var currentPrompt by remember { mutableStateOf("Evaluate this answer sheet against the provided rubric") }
//    var customPromptResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackContent by remember { mutableStateOf<String?>(null) }
    val BRIEF_PROMPT_PREVIEW = "Evaluate answer sheet against rubric and provide detailed feedback"
    val FULL_PROMPT_TEMPLATE = """
    Evaluate this answer sheet against the provided rubric and provide detailed feedback with scores.
    RUBRIC:
    %RUBRIC%
    ANSWER SHEET:
    %ANSWER_SHEET%
     Provide specific feedback with improvement suggestions."""
    // New states for prompt management
    var showPromptPanel by remember { mutableStateOf(false) }
//    var currentPrompt by remember { mutableStateOf("") }
    var finalPrompt by remember { mutableStateOf<String?>(null) }

//    LaunchedEffect(Unit) {
//        evaluateAnswerSheet(answer, rubric, selectedFiles) { result ->
//            feedbackContent = result
//        }
//    }
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
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp),
//            verticalAlignment = Alignment.CenterVertically,
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
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EA)),
//                        modifier = Modifier.height(48.dp) // Correct modifier syntax
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
                        showPromptPanel = true
                        // Generate initial prompt
                        currentPrompt = """
                    "Evaluate this answer sheet against the provided rubric and provide detailed feedback with scores."
                    RUBRIC:
                    ${readFileContent(selectedFiles["Rubric"] ?: "").take(10000)}

                    ANSWER SHEET:
                    ${readFileContent(selectedFiles["Answer Sheet"] ?: "").take(50000)}
                    
                    QUESTION PAPER:
                    ${readFileContent(selectedFiles["Exam Paper"] ?: "").take(50000)}

                    Provide detailed feedback with scores.
                    ""${'"'}.trimIndent()

                    """
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF0000)),
                    modifier = Modifier.height(48.dp),
                    enabled =
                    !isLoading && selectedFiles.contains("Answer Sheet") && selectedFiles.contains(
                        "Rubric"
                    ) && selectedFiles.contains("Exam Paper")
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

                  if (showPromptPanel) {
                    Dialog(onDismissRequest = { showPromptPanel = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),  // Fixed height
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White) // ✅ White Card
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxSize()
                            ) {
                                // 🔹 Blue Heading
                                Text(
                                    text = "✍️ AI Evaluation Prompt",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Blue, // ✅ Blue color for heading
                                    fontSize = 18.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Scrolling inside TextField
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()) // Scroll only inside box
                                ) {
                                    OutlinedTextField(
                                        value = currentPrompt,
                                        onValueChange = { currentPrompt = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),  // Ensure TextField does not expand
                                        label = { Text("Edit the evaluation prompt") },
                                        maxLines = Int.MAX_VALUE
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(onClick = { showPromptPanel = false }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            finalPrompt = currentPrompt
                                            showPromptPanel = false
                                            isLoading = true
                                        }
                                    ) {
                                        Text("Evaluate with This Prompt")
                                    }
                                }
                            }
                        }
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
                    confirmButton = {
                        Button(onClick = {
                            showDialog = false
                        }) { Text("Close") }
                    }
                )
            }
            // left panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),  // ✅ Remove extreme height value
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    backgroundColor = Color.White
                ) {
                    val scrollState = rememberScrollState()

                    Box(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                                .fillMaxWidth()  // ✅ Set a reasonable max height
                        ) {
                            Text(
                                "Student Report",
                                fontSize = 16.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = fileContent,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // ✅ Vertical Scrollbar - Works properly now
                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(scrollState)
                        )
                    }
                }
                // Right Panel Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // PROMPT BOX (added above the right panel)
                    if (finalPrompt != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .height(150.dp),
                            elevation = 4.dp,
                            backgroundColor = Color(0xFFE3F2FD) // Light blue background
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Evaluation Prompt",
                                        style = MaterialTheme.typography.subtitle1,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            showPromptPanel = true
                                            currentPrompt = finalPrompt ?: ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit Prompt")
                                    }
                                }
                                Text(
                                    finalPrompt!!,
                                    modifier = Modifier.padding(top = 8.dp),
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // RIGHT PANEL (AI Evaluation Only)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        elevation = 4.dp
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            val scrollState = rememberScrollState()

                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (feedbackContent != null) {
                                    Text(
                                        "AI Evaluation",
                                        style = MaterialTheme.typography.h6,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    DisplayFeedback(JSONObject(feedbackContent!!))
                                }
                            }

                            VerticalScrollbar(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayFeedback(jsonResponse: JSONObject) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Overall Score Section
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

        // Section-wise Feedback
        jsonResponse.optJSONArray("section_wise")?.let { sections ->
            for (i in 0 until sections.length()) {
                val section = sections.getJSONObject(i)
//                val initialScore = section.optDouble("section_score").takeIf { !it.isNaN() }?.toFloat() ?: 0f

                // State for editable score
//                var sectionScore by remember { mutableStateOf(initialScore) }
                var isEditing by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Section Header
                        Text(
                            "📌 Section: ${section.getString("section")}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        // Criteria feedback
                        section.optJSONArray("criteria")?.let { criteria ->
                            for (j in 0 until criteria.length()) {
                                val crit = criteria.getJSONObject(j)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "🔹 ${crit.getString("criterion")}: ${crit.getString("score")} (${
                                        crit.getString("achieved_level")
                                    })",
                                    fontWeight = FontWeight.Bold
                                )
                                Text("   - ${crit.getString("feedback")}")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score display and slider section
                        Text(
                            "📊 Score: ${section.getString("section_score")}", color = Color.Magenta,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        var sectionScore by remember {
                            mutableStateOf(
                                section.optDouble("section_score").takeIf { !it.isNaN() }?.toFloat()
                                    ?: 0f
                            )
                        }
                        // Slider with edit/confirm buttons
                        Slider(
                            value = sectionScore,
                            onValueChange = { sectionScore = it },
                            valueRange = 0f..10f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF87CEEB), // Sky Blue
                                activeTrackColor = Color(0xFF00BFFF), // Deep Sky Blue
                                inactiveTrackColor = Color(0xFFADD8E6) // Light Blue
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!isEditing) {
                                Button(
                                    onClick = { isEditing = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        sectionScore = sectionScore
                                        isEditing = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Red.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                ) {
                                    Text("Cancel")
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = {
                                        // Here you would update the backend with sectionScore
                                        isEditing = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Green.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                ) {
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun evaluateAnswerSheet(
    answerSheet: String,
    rubric: String,
    questionPaper: String, // Added Question Paper
    prompt: String,
    selectedFiles: Map<String, String>,
    onResult: (String) -> Unit
) {
    val parsedRubric =
        if (rubric.contains("\t")) parseExcelRubric(
            File(
                selectedFiles["Rubric"] ?: ""
            )
        ) else rubric

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiKey = "AIzaSyACAhaIxIrz1mqt6gyz4c51g0xhCuKQOTc" // Ensure this is a valid key
    val url =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent?key=$apiKey"

    // Truncate inputs to avoid exceeding model limits
    val truncatedRubric = parsedRubric.take(100000) // ~100k characters
    val truncatedAnswer = answerSheet.take(500000) // ~500k characters
    val truncatedQuestionPaper = questionPaper.take(100000) // ~100k characters

    val prompt = """
    ROLE: Expert academic evaluator analyzing answer sheets using both rubric and question paper.
    
    TASK:
    1. Parse the question paper to understand what is expected in the answers.
    2. Analyze the rubric to extract evaluation criteria and scoring guidelines.
    3. Evaluate the student’s answer based on both the question paper and the rubric.
    4. Provide a JSON response with detailed feedback, scores, strengths, and improvement areas.
//    5. Review the question paper to understand what was asked
//    6. Parse the tabular rubric data
//    7. Evaluate answer against both the question requirements and rubric criteria
//    8. Return JSON response with scores
    QUESTION PAPER:
    $truncatedQuestionPaper
    
    
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
    - Match student’s answer against the **question paper** expectations and the **rubric**.
    - Assign scores accordingly and provide specific feedback for each criterion.
    - Convert rubric percentages into appropriate scores.
    - Highlight where the student met or missed key points from the question paper.
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
            PDFTextStripper().getText(doc).trim()  // Remove image extraction logic
        }
    } catch (e: Exception) {
        "Error reading PDF: ${e.message}"
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

fun extractImagesFromPdf(file: File): List<String> {
    val imagePaths = mutableListOf<String>()
    try {
        PDDocument.load(file).use { document ->
            for (pageNum in 0 until document.numberOfPages) {
                val page = document.getPage(pageNum)
                val resources: PDResources = page.resources ?: continue

                for (xObjectName in resources.xObjectNames) {
                    val xObject = resources.getXObject(xObjectName)
                    if (xObject is PDImageXObject) {
                        val image = xObject.image
                        val outputFile = File.createTempFile("pdf_image_${pageNum}_", ".png")
                        ImageIO.write(image, "PNG", outputFile)
                        imagePaths.add(outputFile.absolutePath)
                    }
                }
            }
        }
    } catch (e: IOException) {
        println("Error extracting images: ${e.message}")
    }
    return imagePaths
}

fun extractTextFromPdfWithOCR(file: File): String {
    val images = extractImagesFromPdf(file)
    if (images.isEmpty()) return readPdfContent(file)

    val text = StringBuilder()
    images.forEach { imagePath ->
        text.append(runTesseractOCR(File(imagePath)))  // Implement Tesseract
    }
    return text.toString()
}

fun runTesseractOCR(imageFile: File): String {
    return try {
        val tesseract = Tesseract()
        tesseract.setDatapath("path/to/tessdata")  // Path to tessdata directory
        tesseract.setLanguage("eng")               // Language (e.g., "eng" for English)
        tesseract.doOCR(imageFile)                // Extract text
    } catch (e: TesseractException) {
        println("OCR Error: ${e.message}")
        "OCR_FAILED"
    }
}