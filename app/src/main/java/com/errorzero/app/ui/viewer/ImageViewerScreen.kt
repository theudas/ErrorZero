package com.errorzero.app.ui.viewer

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.errorzero.app.app
import com.errorzero.app.data.ImageKind
import com.errorzero.app.data.imagesOf
import com.errorzero.app.ui.components.imageFile
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerScreen(mistakeId: Long, kind: Int, startIndex: Int, onClose: () -> Unit) {
    val repo = LocalContext.current.app.repository
    val images by remember(mistakeId, kind) {
        repo.mistake(mistakeId).map { it?.imagesOf(kind)?.map { img -> img.fileName } }
    }.collectAsStateWithLifecycle(initialValue = null)

    // 黑底页面使用浅色状态栏图标
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val list = images
        if (list != null && list.isNotEmpty()) {
            val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, list.lastIndex)) { list.size }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { list[it] }) { page ->
                ZoomableImage(list[page])
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "关闭", tint = Color.White) }
                Text(
                    (if (kind == ImageKind.ANSWER) "答案  " else "题目  ") + "${pager.currentPage + 1} / ${list.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        } else {
            IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding()) {
                Icon(Icons.Outlined.Close, "关闭", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ZoomableImage(name: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1f) / 2f
        val maxY = size.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        offset = clamp((center - tap) * 1.5f, 2.5f)
                    }
                })
            }
            .pointerInput(Unit) {
                // 仅在双指缩放或已放大时消费手势，其余交给翻页
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offset = if (newScale == 1f) Offset.Zero else clamp(offset + event.calculatePan(), newScale)
                            scale = newScale
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = imageFile(name),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
