package com.errorzero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.errorzero.app.app
import com.errorzero.app.data.Mistake
import com.errorzero.app.data.MistakeFull
import com.errorzero.app.data.displayTitle
import com.errorzero.app.data.moduleTags
import com.errorzero.app.data.questionImages
import com.errorzero.app.data.reasonTags
import com.errorzero.app.data.sortedImages
import com.errorzero.app.util.Dates
import java.io.File
import java.time.LocalDate

val ButtonShape = RoundedCornerShape(6.dp)

@Composable
fun imageFile(name: String): File = LocalContext.current.app.repository.images.file(name)

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 28.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** 下划线式的文字标签页，用于科目、状态切换 */
@Composable
fun TextTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .width(14.dp)
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent),
        )
    }
}

/** 可选中的方角小标签 */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingClose: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) cs.primary else Color.Transparent)
            .border(0.8.dp, if (selected) cs.primary else cs.outline, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (selected) cs.onPrimary else cs.onSurface)
        if (trailingClose) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.Close, contentDescription = "移除", Modifier.size(14.dp), tint = if (selected) cs.onPrimary else cs.onSurfaceVariant)
        }
    }
}

/** 只读标签 */
@Composable
fun TagLabel(text: String) {
    Text(
        text,
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick, modifier.height(48.dp), enabled = enabled, shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) { Text(text, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick, modifier.height(48.dp), enabled = enabled, shape = ButtonShape,
        border = ButtonDefaults.outlinedButtonBorder,
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) { Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun MistakeRow(item: MistakeFull, onClick: () -> Unit) {
    val m = item.mistake
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        val cover = item.questionImages.firstOrNull() ?: item.sortedImages.firstOrNull()
        Box(
            Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.small)
                .background(cs.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                AsyncImage(
                    model = imageFile(cover.fileName),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(item.subject?.name?.take(1).orEmpty(), style = MaterialTheme.typography.titleLarge, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.subject?.name.orEmpty()}  ·  ${Dates.shortDate(m.recordedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusText(m)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.displayTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val tags = item.reasonTags + item.moduleTags
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    tags.joinToString("  /  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun StatusText(m: Mistake) {
    val cs = MaterialTheme.colorScheme
    when {
        m.archived -> Text("已归档", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        m.nextReviewAt != null -> {
            val due = Dates.daysFromToday(m.nextReviewAt) <= 0
            Text(
                Dates.dueLabel(m.nextReviewAt),
                style = MaterialTheme.typography.labelMedium,
                color = if (due) cs.tertiary else cs.onSurfaceVariant,
                fontWeight = if (due) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- 对话框 ----

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "确定",
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, color = if (destructive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text(placeholder) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()); onDismiss() }, enabled = value.isNotBlank()) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPickerMillis(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Dates.fromPickerMillis(it)) }
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("选择时间", style = MaterialTheme.typography.titleLarge) },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) } },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour, state.minute); onDismiss() }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
