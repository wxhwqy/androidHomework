package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen() {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            calendar = Calendar.getInstance().apply { time = now }
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())
            
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            
            delay(1000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnalogClock(
            calendar = calendar,
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = currentDate,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = currentTime,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnalogClock(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    
    Canvas(
        modifier = modifier
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 * 0.9f
        val center = Offset(centerX, centerY)
        
        drawCircle(
            color = outline,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )
        
        for (i in 1..12) {
            val angle = (i * 30 - 90).toDouble() // 12点为0度起始
            val hourMarkRadius = radius * 0.85f
            val hourMarkLength = radius * 0.15f
            
            val startX = centerX + cos(Math.toRadians(angle)) * hourMarkRadius
            val startY = centerY + sin(Math.toRadians(angle)) * hourMarkRadius
            val endX = centerX + cos(Math.toRadians(angle)) * (hourMarkRadius + hourMarkLength)
            val endY = centerY + sin(Math.toRadians(angle)) * (hourMarkRadius + hourMarkLength)
            
            drawLine(
                color = onSurface,
                start = Offset(startX.toFloat(), startY.toFloat()),
                end = Offset(endX.toFloat(), endY.toFloat()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        for (i in 0..59) {
            if (i % 5 != 0) {
                val angle = (i * 6 - 90).toDouble()
                val minuteMarkRadius = radius * 0.9f
                val minuteMarkLength = radius * 0.05f
                
                val startX = centerX + cos(Math.toRadians(angle)) * minuteMarkRadius
                val startY = centerY + sin(Math.toRadians(angle)) * minuteMarkRadius
                val endX = centerX + cos(Math.toRadians(angle)) * (minuteMarkRadius + minuteMarkLength)
                val endY = centerY + sin(Math.toRadians(angle)) * (minuteMarkRadius + minuteMarkLength)
                
                drawLine(
                    color = outline,
                    start = Offset(startX.toFloat(), startY.toFloat()),
                    end = Offset(endX.toFloat(), endY.toFloat()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        
        val hourAngle = (hours * 30 + minutes * 0.5f)
        val minuteAngle = (minutes * 6).toFloat()
        val secondAngle = (seconds * 6).toFloat()
        
        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = onSurface,
                start = center,
                end = Offset(centerX, centerY - radius * 0.5f),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        rotate(degrees = minuteAngle, pivot = center) {
            drawLine(
                color = onSurface,
                start = center,
                end = Offset(centerX, centerY - radius * 0.7f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        rotate(degrees = secondAngle, pivot = center) {
            drawLine(
                color = primary,
                start = center,
                end = Offset(centerX, centerY - radius * 0.8f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        drawCircle(
            color = primary,
            radius = 8.dp.toPx(),
            center = center
        )
    }
} 