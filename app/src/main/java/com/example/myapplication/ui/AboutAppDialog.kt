package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Премиальная иконка приложения
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "SmartDo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )

                Text(
                    "Умные заметки, задачи и напоминания",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "✨ Возможности SmartDo:",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("• Минималистичный дизайн в стиле Apple (iOS)", style = MaterialTheme.typography.bodySmall)
                        Text("• Категории задач с выбором кастомных иконок", style = MaterialTheme.typography.bodySmall)
                        Text("• Точные напоминания, будильники и дедлайны", style = MaterialTheme.typography.bodySmall)
                        Text("• Раздел памятных дат и дней рождения с расчётом знаков зодиака", style = MaterialTheme.typography.bodySmall)
                        Text("• Встроенный центр уведомлений с историей", style = MaterialTheme.typography.bodySmall)
                        Text("• Архив с возможностью восстановления задач", style = MaterialTheme.typography.bodySmall)
                        Text("• Поддержка светлой и глубокой тёмной темы", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Версия 1.0.0",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отлично", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
