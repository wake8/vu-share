package com.vunnam.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun ClipboardShareDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSendClipboardText: (String) -> Unit
) {
    val colors = VuShareTheme.colors
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var textToShare by remember {
        val clip = clipboardManager.primaryClip
        val currentClip = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() ?: "" else ""
        mutableStateOf(if (initialText.isNotBlank()) initialText else currentClip)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.elevatedSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = colors.primaryAccent)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Clipboard Text Sharing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Instantly beam text, links, or notes to nearby connected devices without creating files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textToShare,
                    onValueChange = { textToShare = it },
                    placeholder = { Text("Paste or type text to share...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("clipboard_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = vuTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textToShare.isNotBlank()) {
                        onSendClipboardText(textToShare)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent,
                    disabledContainerColor = colors.disabledSurface,
                    disabledContentColor = colors.disabledText
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = textToShare.isNotBlank(),
                modifier = Modifier.testTag("send_clipboard_button")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("Cancel", color = colors.primaryText)
            }
        }
    )
}
