package com.vunnam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vunnam.model.FileCategory
import com.vunnam.model.SelectableFile
import com.vunnam.network.NetworkUtils
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun FileItemCard(
    file: SelectableFile,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val iconVector = when (file.category) {
        FileCategory.PHOTOS -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.Movie
        FileCategory.MUSIC -> Icons.Default.Audiotrack
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.APPS -> Icons.Default.Android
        FileCategory.FILES -> Icons.Default.InsertDriveFile
    }

    val iconTint = when (file.category) {
        FileCategory.PHOTOS -> colors.primaryAccent
        FileCategory.VIDEOS -> if (colors.isLight) Color(0xFF6D28D9) else Color(0xFFC084FC)
        FileCategory.MUSIC -> colors.warning
        FileCategory.DOCUMENTS -> if (colors.isLight) Color(0xFF1D4ED8) else Color(0xFF60A5FA)
        FileCategory.APPS -> colors.success
        FileCategory.FILES -> if (colors.isLight) Color(0xFF7E22CE) else Color(0xFFA855F7)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_item_${file.name}")
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) colors.selectedBorder else colors.border
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.selectedSurface else colors.primaryCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = if (colors.isLight) 0.12f else 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = file.category.displayName,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = NetworkUtils.formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primaryAccent else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (isSelected) colors.primaryAccent else colors.secondaryText,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = colors.onPrimaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
