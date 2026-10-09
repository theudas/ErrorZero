package com.errorzero.app.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.errorzero.app.app
import com.errorzero.app.data.ImageKind
import com.errorzero.app.data.MistakeFull
import com.errorzero.app.data.MistakeImage
import com.errorzero.app.data.answerImages
import com.errorzero.app.data.displayTitle
import com.errorzero.app.data.moduleTags
import com.errorzero.app.data.questionImages
import com.errorzero.app.data.reasonTags
import com.errorzero.app.review.ReviewSchedule
import com.errorzero.app.ui.components.ConfirmDialog
import com.errorzero.app.ui.components.DatePickDialog
import com.errorzero.app.ui.components.Hairline
import com.errorzero.app.ui.components.PrimaryButton
import com.errorzero.app.ui.components.SecondaryButton
import com.errorzero.app.ui.components.SectionLabel
import com.errorzero.app.ui.components.TagLabel
import com.errorzero.app.ui.components.imageFile
import com.errorzero.app.util.Dates
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mistakeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onViewImage: (kind: Int, index: Int) -> Unit,
) {
    val app = LocalContext.current.app
    val vm: DetailViewModel = viewModel { DetailViewModel(app.repository, mistakeId) }
    val holder by vm.item.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickNext by remember { mutableStateOf(false) }
    var suggestArchive by remember { mutableStateOf(false) }

    val item = holder.item
    LaunchedEffect(holder) {
        if (holder.loaded && holder.item == null) onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } },
                actions = {
                    if (item != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "编辑") }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "更多") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (item.mistake.archived) "取消归档" else "归档") },
                                    onClick = {
                                        menu = false
                                        vm.setArchived(!item.mistake.archived)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("删除", color = MaterialTheme.colorScheme.tertiary) },
                                    onClick = {
                                        menu = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (item != null) {
                ReviewBar(
                    archived = item.mistake.archived,
                    onReview = { mastered ->
                        vm.review(mastered) { result ->
                            if (result.justCompleted) {
                                suggestArchive = true
                            } else {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        "已记录" + (result.next?.let { "，下次重做：${Dates.shortDate(it)}" } ?: ""),
                                    )
                                }
                            }
                        }
                    },
                    onUnarchive = { vm.setArchived(false) },
                )
            }
        },
    ) { padding ->
        if (item != null) {
            DetailContent(
                item = item,
                modifier = Modifier.padding(padding),
                onViewImage = onViewImage,
                onPickNext = { pickNext = true },
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除这道错题？",
            text = "图片和复习记录会一并删除，且无法恢复。若只是不想再被提醒，可以选择归档。",
            confirmText = "删除",
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = { vm.delete() },
        )
    }
    if (pickNext && item != null) {
        DatePickDialog(
            initial = item.mistake.nextReviewAt?.let(Dates::toLocalDate) ?: LocalDate.now().plusDays(1),
            onDismiss = { pickNext = false },
            onPick = { vm.setNextReview(it) },
        )
    }
    if (suggestArchive) {
        ConfirmDialog(
            title = "已完成全部复习",
            text = "这道题已连续 ${ReviewSchedule.ROUNDS} 轮掌握。归档后将不再提醒；不归档则每 30 天提醒一次。",
            confirmText = "归档",
            onDismiss = { suggestArchive = false },
            onConfirm = { vm.setArchived(true) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    item: MistakeFull,
    modifier: Modifier,
    onViewImage: (Int, Int) -> Unit,
    onPickNext: () -> Unit,
) {
    val m = item.mistake
    val cs = MaterialTheme.colorScheme
    var showAnswer by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        val questions = item.questionImages
        if (questions.isNotEmpty()) {
            ImagePager(questions) { onViewImage(ImageKind.QUESTION, it) }
        }

        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(
                "${item.subject?.name.orEmpty()}  ·  ${Dates.dateTime(m.recordedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(item.displayTitle, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth()) {
                Stat("复习进度", ReviewSchedule.progressLabel(m.reviewStage), Modifier.weight(1f))
                Stat("累计重做", "${m.reviewCount} 次", Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    SectionLabel("下次重做")
                    Spacer(Modifier.height(6.dp))
                    when {
                        m.archived -> Text("已归档", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                        m.nextReviewAt == null -> Text("未安排", style = MaterialTheme.typography.bodyLarge)
                        else -> Text(
                            Dates.dueLabel(m.nextReviewAt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (Dates.daysFromToday(m.nextReviewAt) <= 0) cs.tertiary else cs.onSurface,
                            modifier = Modifier.clickable(onClick = onPickNext),
                        )
                    }
                }
            }
            if (!m.archived) {
                TextButton(onClick = onPickNext, modifier = Modifier.align(Alignment.End)) {
                    Text("调整重做日期", color = cs.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(20.dp))
            }
            Hairline()
        }

        val answers = item.answerImages
        if (answers.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("答案", Modifier.weight(1f))
                if (showAnswer) {
                    Text(
                        "隐藏",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.clickable { showAnswer = false },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (showAnswer) {
                ImagePager(answers) { onViewImage(ImageKind.ANSWER, it) }
            } else {
                Box(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(0.8.dp, cs.outline, MaterialTheme.shapes.medium)
                        .clickable { showAnswer = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("点击查看答案", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "共 ${answers.size} 张 · 建议先独立重做",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Hairline(Modifier.padding(horizontal = 24.dp))
        }

        if (item.reasonTags.isNotEmpty()) Block("错因") { Tags(item.reasonTags) }
        if (item.moduleTags.isNotEmpty()) Block("模块") { Tags(item.moduleTags) }
        if (m.note.isNotBlank()) {
            Block("备注") { Text(m.note, style = MaterialTheme.typography.bodyLarge) }
        }
        if (item.reviews.isNotEmpty()) {
            Block("重做记录", divider = false) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.reviews.sortedByDescending { it.reviewedAt }.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Dates.shortDateTime(r.reviewedAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (r.mastered) "已掌握" else "仍不会",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (r.mastered) cs.onSurface else cs.tertiary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePager(images: List<MistakeImage>, onClick: (Int) -> Unit) {
    val pager = rememberPagerState { images.size }
    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { images[it].id }) { page ->
            AsyncImage(
                model = imageFile(images[page].fileName),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onClick(page) },
            )
        }
        if (images.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${images.size}",
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Block(title: String, divider: Boolean = true, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(20.dp))
        SectionLabel(title)
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(20.dp))
        if (divider) Hairline()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { TagLabel(it) }
    }
}

@Composable
private fun ReviewBar(archived: Boolean, onReview: (Boolean) -> Unit, onUnarchive: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Hairline()
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                if (archived) {
                    SecondaryButton("取消归档", onUnarchive, Modifier.fillMaxWidth())
                } else {
                    SecondaryButton("仍不会", { onReview(false) }, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    PrimaryButton("已掌握", { onReview(true) }, Modifier.weight(1f))
                }
            }
        }
    }
}
