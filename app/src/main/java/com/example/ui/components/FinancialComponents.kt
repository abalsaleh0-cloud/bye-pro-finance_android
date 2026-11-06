package com.example.ui.components

import com.example.ui.theme.ByeProTheme
import com.example.finance.i18n.LocalAppLanguage
import com.example.finance.i18n.WebI18n
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinancialMetricCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = ByeProTheme.colors.goldPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun HealthScoreCard(
    score: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1200),
        label = "healthScore"
    )
    val healthColors = ByeProTheme.colors

    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BYE PRO Health Index",
                    style = MaterialTheme.typography.labelMedium,
                    color = ByeProTheme.colors.goldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = WebI18n.tr("سيولة مستقرة ومعدل ديون منخفض وميزانية متوازنة", "Stable liquidity, low debt ratio, and a balanced budget", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(76.dp)) {
                    val stroke = 8.dp.toPx()
                    drawCircle(
                        color = healthColors.borderBase.copy(alpha = 0.65f),
                        style = Stroke(stroke)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(healthColors.goldPrimary, healthColors.financialGreen, healthColors.goldMuted)),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = ByeProTheme.colors.financialGreen
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tag: String,
    accentColor: Color = ByeProTheme.colors.financialGreen,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .testTag(tag)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = ByeProTheme.colors.goldPrimary
                )
            }
        }
    }
}

@Composable
fun InteractiveCashFlowChart(
    income: Double,
    expense: Double,
    currency: String = "SAR",
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val total = (income + expense).coerceAtLeast(1.0)
    val incomePercent = (income / total).toFloat()
    val expensePercent = (expense / total).toFloat()

    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = WebI18n.tr("التدفق والمقارنة الشهرية (Cash Flow Comparison)", "Cash Flow Comparison", lang),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Income bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(WebI18n.tr("الدخل (Income)", "Income", lang), style = MaterialTheme.typography.bodySmall, color = ByeProTheme.colors.financialGreen)
                Text("${income.toInt()} $currency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { incomePercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = ByeProTheme.colors.financialGreen,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expense bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(WebI18n.tr("المصروف (Expense)", "Expense", lang), style = MaterialTheme.typography.bodySmall, color = ByeProTheme.colors.financialRed)
                Text("${expense.toInt()} $currency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { expensePercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = ByeProTheme.colors.financialRed,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun ByeProLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(com.example.ui.theme.LogoBgDark)
            .border(1.5.dp, com.example.ui.theme.LogoGoldBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "B",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45).sp
            ),
            color = com.example.ui.theme.LogoIvoryIcon
        )
    }
}

@Composable
fun FinancialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    val colors = com.example.ui.theme.ByeProTheme.colors
    val borderColor = when {
        isError -> colors.statusError
        else -> colors.fieldBorder
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (enabled) colors.fieldText else colors.textMuted
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.fieldFocus),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (singleLine) 44.dp else androidx.compose.ui.unit.Dp.Unspecified)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.fieldBg)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = onTrailingIconClick != null) {
                                    onTrailingIconClick?.invoke()
                                }
                        )
                    }
                }
            }
        )
    }
}

