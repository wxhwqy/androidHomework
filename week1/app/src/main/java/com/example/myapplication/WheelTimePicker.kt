package com.example.myapplication

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberAutoSnapLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    maxItems: Int,
    onItemSelected: (Int) -> Unit
): LazyListState {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex)
    val coroutineScope = rememberCoroutineScope()
    
    var lastScrollTime by remember { mutableStateOf(0L) }
    var isUserScrolling by remember { mutableStateOf(false) }
    
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (!listState.isScrollInProgress) return@LaunchedEffect
        
        isUserScrolling = true
        lastScrollTime = System.currentTimeMillis()
        
        delay(200)
        if (System.currentTimeMillis() - lastScrollTime >= 200 && !listState.isScrollInProgress) {
            isUserScrolling = false
            
            val offset = listState.firstVisibleItemScrollOffset
            val itemHeight = 120
            val targetIndex = if (offset > itemHeight / 2) {
                (listState.firstVisibleItemIndex + 1).coerceIn(0, maxItems - 1)
            } else {
                listState.firstVisibleItemIndex.coerceIn(0, maxItems - 1)
            }
            
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
                onItemSelected(targetIndex)
            }
        }
    }
    
    return listState
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentHour by remember { mutableStateOf(selectedHour) }
    var currentMinute by remember { mutableStateOf(selectedMinute) }
    
    val hourListState = rememberAutoSnapLazyListState(
        initialFirstVisibleItemIndex = selectedHour,
        maxItems = 24
    ) { newHour ->
        val validHour = newHour.coerceIn(0, 23)
        if (validHour != currentHour) {
            currentHour = validHour
            onTimeChanged(currentHour, currentMinute)
        }
    }
    
    val minuteListState = rememberAutoSnapLazyListState(
        initialFirstVisibleItemIndex = selectedMinute,
        maxItems = 60
    ) { newMinute ->
        val validMinute = newMinute.coerceIn(0, 59)
        if (validMinute != currentMinute) {
            currentMinute = validMinute
            onTimeChanged(currentHour, currentMinute)
        }
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .height(200.dp)
            .background(
                Color.Black.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    state = hourListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 80.dp)
                ) {
                    items(24) { hour ->
                        val isSelected = hour == currentHour
                        val distance = kotlin.math.abs(hour - currentHour)
                        val alpha = when (distance) {
                            0 -> 1f
                            else -> 0.7f
                        }
                        val fontSize = 24.sp
                        
                        Text(
                            text = String.format("%02d", hour),
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .alpha(alpha)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }
            
            Text(
                text = "时",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                LazyColumn(
                    state = minuteListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 80.dp)
                ) {
                    items(60) { minute ->
                        val isSelected = minute == currentMinute
                        val distance = kotlin.math.abs(minute - currentMinute)
                        val alpha = when (distance) {
                            0 -> 1f
                            1 -> 0.7f
                            2 -> 0.4f
                            else -> 0.2f
                        }
                        val fontSize = 24.sp // 统一字体大小
                        
                        Text(
                            text = String.format("%02d", minute),
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .alpha(alpha)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }
            
            Text(
                text = "分",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            Color.Transparent
                        )
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
} 