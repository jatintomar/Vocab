package com.jtvocab.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jtvocab.quiz.viewmodel.VocabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF59E0B), // Amber 500
                    background = Color(0xFF0F172A), // Slate 900
                    surface = Color(0xFF1E293B) // Slate 800
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VocabApp()
                }
            }
        }
    }
}

@Composable
fun VocabApp(viewModel: VocabViewModel = viewModel()) {
    var currentMode by remember { mutableStateOf("quiz") }
    var currentCat by remember { mutableStateOf("ow") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // App Nav
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Button(
                onClick = { currentMode = "quiz" },
                modifier = Modifier.weight(1f).padding(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentMode == "quiz") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Quiz", color = if (currentMode == "quiz") Color.Black else Color.White)
            }
            Button(
                onClick = { currentMode = "review" },
                modifier = Modifier.weight(1f).padding(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentMode == "review") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Learn", color = if (currentMode == "review") Color.Black else Color.White)
            }
        }

        // Category Tabs
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            listOf("ow" to "OWS", "sy" to "Syno", "id" to "Idiom").forEach { (id, label) ->
                TextButton(
                    onClick = { 
                        currentCat = id
                        viewModel.startQuiz(id, 0)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label, 
                        color = if (currentCat == id) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Content
        val quizItems by viewModel.currentQuizBatch
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(quizItems) { index, quiz ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = quiz.item.w,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        quiz.options.forEach { option ->
                            val isCorrect = option == quiz.item.a
                            val isSelected = quiz.selectedOption == option
                            val backgroundColor = when {
                                quiz.isAnswered && isCorrect -> Color(0xFF10B981) // Green
                                isSelected && !isCorrect -> Color(0xFFEF4444) // Red
                                else -> Color(0xFF0F172A)
                            }
                            
                            Button(
                                onClick = { viewModel.submitAnswer(index, option) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
                            ) {
                                Text(option, color = if (quiz.isAnswered && (isCorrect || isSelected)) Color.Black else Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
