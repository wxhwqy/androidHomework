package com.example.myapplication

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    val alarmManagerHelper = remember { AlarmManagerHelper(context) }
    val alarmDataManager = remember { AlarmDataManager(context) }
    
    var alarmList by remember { 
        mutableStateOf(
            alarmDataManager.getAlarms().ifEmpty {
                listOf(
                    AlarmItem(hour = 5, minute = 30, isEnabled = false, repeatType = RepeatType.ONCE),
                    AlarmItem(hour = 6, minute = 0, isEnabled = false, repeatType = RepeatType.DAILY),
                    AlarmItem(hour = 7, minute = 15, isEnabled = false, repeatType = RepeatType.DAILY),
                    AlarmItem(hour = 7, minute = 30, isEnabled = false, repeatType = RepeatType.ONCE),
                    AlarmItem(hour = 8, minute = 0, isEnabled = false, repeatType = RepeatType.DAILY),
                    AlarmItem(hour = 9, minute = 0, isEnabled = false, repeatType = RepeatType.ONCE)
                )
            }.sortedByTime()
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var selectedAlarms by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedAlarms.isNotEmpty()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alarmList) { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    isSelected = alarm.id in selectedAlarms,
                    isSelectionMode = isSelectionMode,
                    onToggle = { isEnabled ->
                        if (!isSelectionMode) {
                            alarmList = alarmList.map { 
                                if (it.id == alarm.id) {
                                    val updatedAlarm = it.copy(isEnabled = isEnabled)
                                    if (isEnabled) {
                                        alarmManagerHelper.setAlarm(updatedAlarm)
                                    } else {
                                        alarmManagerHelper.cancelAlarm(it.id)
                                    }
                                    updatedAlarm
                                } else it 
                            }
                            alarmDataManager.saveAlarms(alarmList)
                            ClockWidgetProvider.updateAllWidgets(context)
                        }
                    },
                    onLongPress = {
                        if (!isSelectionMode) {
                            selectedAlarms = setOf(alarm.id)
                        }
                    },
                    onClick = {
                        if (isSelectionMode) {
                            selectedAlarms = if (alarm.id in selectedAlarms) {
                                selectedAlarms - alarm.id
                            } else {
                                selectedAlarms + alarm.id
                            }
                        } else {
                            editingAlarm = alarm
                            showEditDialog = true
                        }
                    }
                )
            }
        }
        
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加闹钟")
            }
        }
        
        if (isSelectionMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedAlarms = setOf()
                        }
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            selectedAlarms.forEach { alarmId ->
                                alarmManagerHelper.cancelAlarm(alarmId)
                            }
                            alarmList = alarmList.filter { it.id !in selectedAlarms }
                            selectedAlarms = setOf()
                            alarmDataManager.saveAlarms(alarmList)
                            ClockWidgetProvider.updateAllWidgets(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("删除 (${selectedAlarms.size})")
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newAlarm ->
                alarmList = (alarmList + newAlarm).sortedByTime()
                if (newAlarm.isEnabled) {
                    alarmManagerHelper.setAlarm(newAlarm)
                }
                alarmDataManager.saveAlarms(alarmList)
                ClockWidgetProvider.updateAllWidgets(context)
                showAddDialog = false
            }
        )
    }
    
    if (showEditDialog && editingAlarm != null) {
        EditAlarmDialog(
            alarm = editingAlarm!!,
            onDismiss = { 
                showEditDialog = false
                editingAlarm = null
            },
            onConfirm = { updatedAlarm ->
                alarmList = alarmList.map { alarm ->
                    if (alarm.id == updatedAlarm.id) {
                        if (updatedAlarm.isEnabled) {
                            alarmManagerHelper.cancelAlarm(alarm.id)
                            alarmManagerHelper.setAlarm(updatedAlarm)
                        }
                        updatedAlarm
                    } else {
                        alarm
                    }
                }.sortedByTime()
                alarmDataManager.saveAlarms(alarmList)
                ClockWidgetProvider.updateAllWidgets(context)
                showEditDialog = false
                editingAlarm = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmListItem(
    alarm: AlarmItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: (Boolean) -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else 
                        Color.Transparent
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (alarm.hour < 12) "上午" else "下午",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.repeatType.displayName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isSelectionMode) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
} 