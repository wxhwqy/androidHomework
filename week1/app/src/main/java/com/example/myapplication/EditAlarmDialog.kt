package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmDialog(
    alarm: AlarmItem,
    onDismiss: () -> Unit,
    onConfirm: (AlarmItem) -> Unit
) {
    var selectedHour by remember { mutableStateOf(alarm.hour) }
    var selectedMinute by remember { mutableStateOf(alarm.minute) }
    var selectedRepeatType by remember { mutableStateOf(alarm.repeatType) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "编辑闹钟",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 显示选择的时间
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 滚轮时间选择器
                WheelTimePicker(
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    onTimeChanged = { hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 重复类型选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRepeatType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("重复") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        RepeatType.values().forEach { repeatType ->
                            DropdownMenuItem(
                                text = { Text(repeatType.displayName) },
                                onClick = {
                                    selectedRepeatType = repeatType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            val updatedAlarm = alarm.copy(
                                hour = selectedHour,
                                minute = selectedMinute,
                                repeatType = selectedRepeatType
                            )
                            onConfirm(updatedAlarm)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
} 