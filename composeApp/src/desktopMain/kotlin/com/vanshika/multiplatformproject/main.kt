package com.vanshika.multiplatformproject

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.utils.io.core.use
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.PDFRenderer
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
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import androidx.compose.ui.window.Dialog
import javax.swing.UIManager.put
import org.apache.poi.ss.usermodel.*
import java.io.*
import org.apache.tika.Tika
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.image.BufferedImage

fun main() = application {
    val answer = "Student's Answer Here"
    val rubric = "Rubric Criteria Here"
    val selectedFiles = mapOf("Rubric" to "path/to/rubric.xlsx")
//    val imageBitmap = ImageIO.read(object {}.javaClass.getResourceAsStream("/edumark.png"))
//        .toComposeImageBitmap()
//    val iconPainter = BitmapPainter(imageBitmap)
    val imageStream = object {}.javaClass.getResourceAsStream("/edumark.png")
        ?: throw IllegalStateException("Icon PNG not found in resources!")
    val awtImage: BufferedImage = ImageIO.read(imageStream)
    val iconPainter = BitmapPainter(awtImage.toComposeImageBitmap())

    Window(onCloseRequest = ::exitApplication, title = "MultiPlatformProject",

        icon = iconPainter )

    {
        DesktopApp(answer, rubric, selectedFiles)
    }
}

@Composable
@Preview
fun DesktopApp(
    answer: String, rubric: String, selectedFiles: Map<String, String>
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
    
    Provide specific feedback with improvement suggestions.
"""
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
            modifier = Modifier.fillMaxSize().background(Color(0xFFADD8E6)).padding(16.dp)
        ) {
            TopAppBar(
                backgroundColor = Color(0xFF3B83BD), contentColor = Color.White
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
//                        text = "EDUMARK-AI: TRANSFORMING SCORING AND FEEDBACK",
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.Bold,
//                        textAlign = TextAlign.Center
                        text = "EDUMARK-AI: TRANSFORMING SCORING AND FEEDBACK",
                        fontSize = 22.sp, // ✅ Bigger Font for Impact
                        fontWeight = FontWeight.Bold, // ✅ Extra Bold for Attention
                        color = Color.White, // ✅ Deep Blue for Professional Look
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Italic, // ✅ Slight Italic for Modern Look
                        letterSpacing = 1.5.sp, // ✅ Better Readability
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp) // ✅ Padding for Proper Spacing
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                    enabled = !isLoading && selectedFiles.contains("Answer Sheet") && selectedFiles.contains(
                        "Rubric"
                    ) && selectedFiles.contains("Exam Paper")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
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
                                    text = "✍ AI Evaluation Prompt",
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
                                            evaluateAnswerSheet(
                                                answerSheet = readFileContent(
                                                    selectedFiles["Answer Sheet"] ?: ""
                                                ),
                                                rubric = readFileContent(
                                                    selectedFiles["Rubric"] ?: ""
                                                ),
                                                questionPaper = readFileContent( // 🔹 Added question paper
                                                    selectedFiles["Question Paper"] ?: ""
                                                ),
                                                prompt = currentPrompt, // Pass the custom prompt
                                                selectedFiles = selectedFiles
                                            ) { result ->
                                                feedbackContent = result
                                                isLoading = false
                                            }
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
                AlertDialog(onDismissRequest = { showDialog = false },
                    title = { Text("Selected Files") },
                    text = {
                        Column {
                            selectedFiles.forEach { (label, path) ->
                                Text(
                                    text = "$label: ${File(path).name}",
                                    modifier = Modifier.clickable {
                                        openSelectedFile(path); showDialog = false
                                    }.padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            showDialog = false
                        }) { Text("Close") }
                    })
            }
            // left panel
            Row(
                modifier = Modifier.fillMaxWidth()
                    .fillMaxHeight(),  // ✅ Remove extreme height value
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(), backgroundColor = Color.White
                ) {
                    val scrollState = rememberScrollState()

                    Box(Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.verticalScroll(scrollState).padding(16.dp)
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
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(scrollState)
                        )
                    }
                }
                // Right Panel Area
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    // PROMPT BOX (added above the right panel)
                    if (finalPrompt != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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
                                        }, modifier = Modifier.size(24.dp)
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
                        modifier = Modifier.weight(1f).fillMaxWidth(), elevation = 4.dp
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            val scrollState = rememberScrollState()

                            Column(
                                modifier = Modifier.padding(16.dp).verticalScroll(scrollState)
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
                                        "\uD83D\uDCD8 EDUMARK-AI Score and Feedback",
                                        style = MaterialTheme.typography.h6,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    DisplayFeedback(
                                        JSONObject(feedbackContent!!),
                                        onConfirmAll = { confirmedScores ->
                                            println("Confirmed Scores: $confirmedScores")
                                        })
                                }
                            }

                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
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
fun DisplayFeedback(
    jsonResponse: JSONObject,
    onConfirmAll: (Map<String, Float>) -> Unit // Function to handle all confirmed scores
) {
    val sectionScores = remember { mutableStateMapOf<String, Float>() }
    val editingState = remember { mutableStateMapOf<String, Boolean>() }
// ✅ Calculate totalMax ONCE and remember it across recompositions
    val totalMax = jsonResponse.optJSONArray("section_wise")?.let { sections ->
        (0 until sections.length()).sumOf {
            val section = sections.getJSONObject(it)
            val sectionName = section.getString("section")
            if (sectionScores.containsKey(sectionName)) {
                section.optString("section_score")
                    .split("/").getOrNull(1)?.toFloatOrNull()?.toDouble() ?: 10.0
            } else {
                0.0
            }
        }
    } ?: 100.0

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        // Score and Confirm All Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Score: ${jsonResponse.optString("overall_score")}",
                style = MaterialTheme.typography.h6,
                color = Color.Blue,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    // Calculate new overall score
                    val totalScore = sectionScores.values.sum().toDouble()

//                    val totalMax = jsonResponse.optJSONArray("section_wise")?.let { sections ->
//                        (0 until sections.length()).sumOf {
//                            val section = sections.getJSONObject(it)
//                            (
//                                    section.optString("section_score")
//                                        .split("/").getOrNull(1)?.toFloatOrNull() ?: 10f
//                                    ).toDouble() // 👈 Force toDouble() to avoid ambiguity
//                        }
//                    } ?: 100.0

                    val newOverall = "${"%.1f".format(totalScore)}/${totalMax.toInt()}"

                    jsonResponse.put("overall_score", newOverall)

                    // Lock all sections from editing
                    editingState.keys.forEach { key -> editingState[key] = false }

                    // Trigger final callback
                    onConfirmAll(sectionScores)
                },   colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF4CAF50), // Green
                    contentColor = Color.White
                )
            ) {
            Text("Confirm All")
        }

        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(16.dp)) {
            Text("✅ Strengths:", fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1) // ✅ Darker Blue Text for better contrast
            )
            jsonResponse.optJSONArray("strengths")?.let {
                for (i in 0 until it.length()) {
                    Text("- ${it.getString(i)}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("⚠ Improvement Areas:", fontWeight = FontWeight.Bold,color = Color.Red)
            jsonResponse.optJSONArray("improvement_areas")?.let {
                for (i in 0 until it.length()) {
                    Text("- ${it.getString(i)}", color = Color.Black)
                }
            }
            // Section-wise Feedback
            jsonResponse.optJSONArray("section_wise")?.let { sections ->
                for (i in 0 until sections.length()) {
                    val section = sections.getJSONObject(i)
                    val sectionName = section.getString("section")
                    var isEditing by remember { mutableStateOf(false) }

                    val sectionScoreInitial = section.optString("section_score")
                        .takeIf { it.isNotEmpty() && it != "NaN" }?.split("/")?.first()?.toFloatOrNull() ?: 0f
                    val maxScore = section.optString("section_score")
                        .split("/").getOrNull(1)?.toFloatOrNull() ?: 10f

                    var sliderValue by remember { mutableStateOf(sectionScoreInitial) }
                    var confirmedScore by remember { mutableStateOf(sectionScoreInitial) }

                    sectionScores[sectionName] = confirmedScore // Update to latest value

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feedback Card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "📌 Section: $sectionName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            section.optJSONArray("criteria")?.let { criteria ->
                                for (j in 0 until criteria.length()) {
                                    val crit = criteria.getJSONObject(j)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "🔹 ${crit.getString("criterion")}: ${crit.getString("score")} (${crit.getString("achieved_level")})",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("   - ${crit.getString("feedback")}")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "📊 Score: ${"%.1f".format(confirmedScore)}/${maxScore.toInt()}",
                                color = Color.Magenta,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Slider + Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        if (isEditing) sliderValue = it
                                    },
                                    valueRange = 0f..maxScore,
                                    steps = (maxScore - 1).toInt(),
                                    enabled = isEditing,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (isEditing) Color(0xFF7B1FA2) else Color.Gray,
                                        activeTrackColor = Color(0xFF7B1FA2),
                                        inactiveTrackColor = Color.LightGray.copy(alpha = 0.4f)
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                if (!isEditing) {
                                    Button(onClick = { isEditing = true }) {
                                        Text("Edit")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            sliderValue = confirmedScore // Revert back
                                            isEditing = false
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red.copy(alpha = 0.6f))
                                    ) {
                                        Text("Cancel")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            sectionScores[sectionName] = sliderValue
                                            confirmedScore = sliderValue

                                            isEditing = false
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green.copy(alpha = 0.6f))
                                    ) {
                                        Text("Confirm")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
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
    questionPaper: String,
    prompt: String,
    selectedFiles: Map<String, String>,
    onResult: (String) -> Unit
) {
    val parsedRubric = if (rubric.contains("\t")) parseExcelRubric(File(selectedFiles["Rubric"] ?: "")) else rubric

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiKey = "AIzaSyACAhaIxIrz1mqt6gyz4c51g0xhCuKQOTc"
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent?key=$apiKey"

    val truncatedRubric = parsedRubric.take(100000)
    val truncatedAnswer = answerSheet.take(500000)
    val truncatedQuestionPaper = questionPaper.take(100000)

    val prompt = createEvaluationPrompt(truncatedQuestionPaper, truncatedRubric, truncatedAnswer)

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
            put("temperature", 0.3)
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

    val request = Request.Builder().url(url).post(requestBody).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult("Network Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string() ?: return onResult("Empty Response")

            try {
                val jsonResponse = JSONObject(responseBody)

                if (jsonResponse.has("error")) {
                    val error = jsonResponse.getJSONObject("error")
                    onResult("API Error (${error.optInt("code")}): ${error.getString("message")}")
                    return
                }

                val candidates = jsonResponse.optJSONArray("candidates") ?: JSONArray()
                if (candidates.length() == 0) {
                    onResult("Error: No evaluation returned from API")
                    return
                }

                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")

                val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                try {
                    val json = JSONObject(text)
                    onResult(json.toString(4))
                } catch (e: Exception) {
                    onResult("Received non-JSON response:\n$text")
                }

            } catch (e: Exception) {
                onResult(
                    """
                    Error parsing response: ${e.message}
                    Raw Response:
                    ${responseBody.take(2000)}${if (responseBody.length > 2000) "\n...[truncated]" else ""}
                """.trimIndent()
                )
            }
        }
    })
}

fun createEvaluationPrompt(questionPaper: String, rubric: String, answerSheet: String): String {
    return """
        ROLE: Expert academic evaluator with extensive experience in assessment.

        TASK:
        1. FIRST: Carefully analyze the question paper to understand what is being asked.
        2. SECOND: Analyze the student's answer to determine if it correctly addresses the question(s) asked.
        3. THIRD: Evaluate how well the answer meets the criteria in the rubric.
        4. Determine if the answer is relevant and responsive to the question before applying rubric scoring.
        5. Provide a JSON response with detailed feedback, scores, strengths, and areas for improvement.

        EVALUATION PROCESS:
        - First check if the answer responds to the question - irrelevant answers should receive low scores
        - Only after determining relevance, apply rubric criteria for content quality, organization, etc.
        - Identify missing key points from the question that should have been addressed
        - Be objective and fair in your assessment

        QUESTION PAPER:
        ${questionPaper.ifBlank { "QUESTION PAPER NOT PROVIDED" }}

        RUBRIC:
        ${rubric.ifBlank { "RUBRIC NOT PROVIDED" }}

        STUDENT ANSWER:
        ${answerSheet.ifBlank { "ANSWER SHEET NOT PROVIDED" }}

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
                            "feedback": "Specific comments that reference both the question requirements and rubric criteria",
                            "achieved_level": "Excellent/Good/Fair/Needs improvement"
                        }
                    ],
                    "section_score": "X/Y"
                }
            ],
            "strengths": ["...", "..."],
            "improvement_areas": ["...", "..."]
        }

        IMPORTANT: Your primary task is to judge if the answer correctly addresses what was asked in the question paper, THEN apply the rubric criteria.
    """.trimIndent()
}


fun openFileDialog(title: String): String? {
    val fileDialog = FileDialog(null as Frame?, "Select $title", FileDialog.LOAD)
    fileDialog.isVisible = true
    return fileDialog.file?.let { File(fileDialog.directory, it).absolutePath }
}

fun readPdfContent(file: File): String {
    return try {
        PDDocument.load(file).use { doc ->
            // 🔹 Remove security restrictions if the document is encrypted
            if (doc.isEncrypted) {
                val accessPermission =
                    AccessPermission().apply { setCanExtractContent(true) }
            }

            // 🔹 Extract text using PDFBox
            val stripper = PDFTextStripper().apply {
                setSortByPosition(true)
                setShouldSeparateByBeads(true)
                setAddMoreFormatting(true) // Preserve layout formatting
            }

            val text = try {
                stripper.getText(doc).trim()
            } catch (e: Exception) {
                println("Warning: Unable to extract full text - ${e.message}")
                ""
            }

            // 🔹 Convert PDF pages to images (if needed)
            val pdfRenderer = PDFRenderer(doc)
            for (i in 0 until doc.numberOfPages) {
                val image = pdfRenderer.renderImageWithDPI(i, 300f) // Convert to image
                ImageIO.write(image, "png", File("page_$i.png"))
            }

            // 🔹 Use Apache Tika for additional extraction
            val tika = Tika()
            val content = tika.parseToString(file)
            println("Tika Extracted Content:\n$content")

            // 🔹 Clean formatting (if required)
            if (text.contains("  {2,}".toRegex())) {
                text.replace("  {2,}".toRegex(), "\t")
                    .replace("\t ", "\t")
                    .lines().joinToString("\n") { line ->
                        if (line.contains("\t")) {
                            "| " + line.split("\t").joinToString(" | ") + " |"
                        } else {
                            line
                        }
                    }
            } else {
                text
            }
        }
    } catch (e: IOException) {
        println("Error reading PDF: ${e.message}")
        "Error reading PDF: ${e.message}"
    }
}

// Function to read DOCX content
fun readDocxContent(file: File): String {
    return try {
        FileInputStream(file).use { fis ->
            val doc = XWPFDocument(fis)
            val content = StringBuilder()
            doc.paragraphs.forEach { para -> content.append(para.text).append("\n") }
            doc.tables.forEach { table ->
                content.append("\n[TABLE START]\n")
                table.rows.forEach { row ->
                    val rowContent =
                        row.tableCells.joinToString(" | ") { it.text.replace("\n", " ") }
                    content.append("| $rowContent |\n")
                }
                content.append("[TABLE END]\n")
            }
            content.toString().trim()
        }
    } catch (e: Exception) {
        "Error reading DOCX file: ${e.message}"
    }
}

// Function to read Excel content
fun readExcelContent(file: File): String {
    return try {
        FileInputStream(file).use { fis ->
            val workbook = XSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)
            val data = StringBuilder()
            for (row in sheet) {
                for (cell in row) {
                    data.append(
                        when (cell.cellType) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> cell.numericCellValue
                            CellType.BOOLEAN -> cell.booleanCellValue
                            else -> ""
                        }
                    ).append("\t")
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

// Function to determine file type and process accordingly
fun readFileContent(filePath: String): String {
    val file = File(filePath)
    return when (file.extension.lowercase()) {
        "pdf" -> readPdfContent(file)
        "docx" -> readDocxContent(file)
        "xlsx" -> readExcelContent(file)
        "txt", "csv", "json" -> file.readText()
        else -> "UNSUPPORTED FILE FORMAT: ${file.extension}"
    }
}

// Function to open selected file
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

// Function to extract images from a PDF
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
                        val outputFile =
                            File.createTempFile("pdf_image_${pageNum}_", ".png")
                        ImageIO.write(xObject.image, "PNG", outputFile)
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

// Function to extract text from images in a PDF using OCR
fun extractTextFromPdfWithOCR(file: File): String {
    val images = extractImagesFromPdf(file)
    if (images.isEmpty()) return readPdfContent(file)
    val text = StringBuilder()
    images.forEach { imagePath ->
        text.append(runTesseractOCR(File(imagePath)))
    }
    return text.toString()
}

// Function to run OCR using Tesseract
fun runTesseractOCR(imageFile: File): String {
    return try {
        val tesseract = Tesseract()
        tesseract.setDatapath("path/to/tessdata")
        tesseract.setLanguage("eng")
        tesseract.doOCR(imageFile)
    } catch (e: TesseractException) {
        println("OCR Error: ${e.message}")
        "OCR_FAILED"
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