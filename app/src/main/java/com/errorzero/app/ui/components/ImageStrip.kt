package com.errorzero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val TileSize = 96.dp

/** 编辑页的图片行：已添加的缩略图 + 拍照 / 相册入口 */
@Composable
fun ImageStrip(
    images: List<String>,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onCrop: (String) -> Unit,
    onRemove: (String) -> Unit,
    onMoveFirst: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(images, key = { _, name -> name }) { index, name ->
            var menu by remember { mutableStateOf(false) }
            Box {
                Box(
                    Modifier
                        .size(TileSize)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { menu = true },
                ) {
                    AsyncImage(
                        model = imageFile(name),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRemove(name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, "删除", Modifier.size(14.dp), tint = Color.White)
                    }
                    Text(
                        "${index + 1}",
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("裁剪") },
                        leadingIcon = { Icon(Icons.Outlined.Crop, null) },
                        onClick = { menu = false; onCrop(name) },
                    )
                    if (index > 0) {
                        DropdownMenuItem(text = { Text("移到最前") }, onClick = { menu = false; onMoveFirst(name) })
                    }
                    DropdownMenuItem(text = { Text("删除") }, onClick = { menu = false; onRemove(name) })
                }
            }
        }
        item(key = "camera") { AddTile(Icons.Outlined.PhotoCamera, "拍照", onCamera) }
        item(key = "gallery") { AddTile(Icons.Outlined.PhotoLibrary, "相册", onGallery) }
    }
}

@Composable
private fun AddTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(TileSize)
            .clip(MaterialTheme.shapes.medium)
            .border(0.8.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
