package com.jtvocab.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jtvocab.quiz.viewmodel.VocabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VocabTheme {
                VocabApp()
            }
        }
    }
}

@Composable
fun VocabTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFFF59E0B), // Amber 500
        background = Color(0xFF0F172A), // Slate 900
        surface = Color(0xFF1E293B), // Slate 800
        secondary = Color(0xFF6366F1), // Indigo 500
        onPrimary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
fun VocabApp(viewModel: VocabViewModel = viewModel()) {
    var mode by remember { mutableStateOf("quiz") } // quiz, learn, comp
    var cat by remember { mutableStateOf("ow") } // ow, sy, id
    val state by viewModel.state

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Header(state.streak, state.achievements.filter { it.unlocked }.lastOrNull()?.icon ?: "🎓")
                Spacer(modifier = Modifier.height(16.dp))
                ModeSelector(mode) { mode = it }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (mode != "comp") {
                CategoryTabs(cat) { 
                    cat = it
                    viewModel.startQuiz(it, 0)
                }
            }
            
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                when (mode) {
                    "quiz" -> QuizSection(viewModel)
                    "learn" -> LearnSection(viewModel, cat)
                    "comp" -> CompSection(viewModel)
                }
            }
        }
    }
}

@Composable
fun Header(streak: Int, achievementIcon: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "JT VOCAB",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = Color.White
            )
            Text(
                "SSC EXAM 2026",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(achievementIcon)
            StreakBadge(streak)
        }
    }
}

@Composable
fun Badge(icon: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp)
    }
}

@Composable
fun StreakBadge(streak: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text("STREAK 🔥", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(streak.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun ModeSelector(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        listOf("quiz" to "Quiz", "learn" to "Learn", "comp" to "Comp").forEach { (id, label) ->
            val isSelected = current == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun CategoryTabs(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf("ow" to "OWS", "sy" to "Syno", "id" to "Idiom").forEach { (id, label) ->
            val isSelected = current == id
            TextButton(onClick = { onSelect(id) }) {
                Text(
                    label,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun QuizSection(viewModel: VocabViewModel) {
    val quizItems by viewModel.currentQuizBatch
    
    if (quizItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category to start", color = Color.Gray)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ChallengeCard()
        }
        itemsIndexed(quizItems) { index, quiz ->
            QuizCard(quiz) { viewModel.submitAnswer(index, it) }
        }
    }
}

@Composable
fun ChallengeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text("75 DAY CHALLENGE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.7f))
            Text("DAY 14: THE ASCENT", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = 0.18f,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(0.2f)
            )
        }
    }
}

@Composable
fun QuizCard(quiz: VocabViewModel.QuizItem, onAnswer: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                quiz.item.w,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            quiz.options.forEach { option ->
                val isCorrect = option == quiz.item.a
                val isSelected = quiz.selectedOption == option
                val color = when {
                    quiz.isAnswered && isCorrect -> Color(0xFF10B981)
                    isSelected && !isCorrect -> Color(0xFFEF4444)
                    else -> Color(0xFF0F172A)
                }
                
                Button(
                    onClick = { onAnswer(option) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Text(
                        option,
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (quiz.isAnswered && (isCorrect || isSelected)) Color.White else Color.White.copy(0.8f)
                    )
                }
            }
            
            if (quiz.isAnswered) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text(quiz.item.h, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LearnSection(viewModel: VocabViewModel, cat: String) {
    val quizItems by viewModel.currentQuizBatch
    
    if (quizItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a category to learn", color = Color.Gray)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(quizItems) { quiz ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(quiz.item.a.take(1).uppercase(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(quiz.item.a, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                        Text(quiz.item.w, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(quiz.item.h, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun CompSection(viewModel: VocabViewModel) {
    val pqrs by viewModel.dailyPQRS
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("DAILY COMPREHENSION", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        item {
            PQRSCard(pqrs)
        }
    }
}

@Composable
fun PQRSCard(pqrs: PQRSQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("PQRS (PARAJUMBLES)", fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(12.dp))
            pqrs.s1?.let { Text(it, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            pqrs.sentences.forEach { sentence ->
                Text(sentence, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            pqrs.s6?.let { Text(it, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Show Answer & Logic", fontWeight = FontWeight.Bold)
            }
        }
    }
}
