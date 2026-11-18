package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.BengaliUtils
import com.example.ui.PendingImport

@Composable
fun SplashOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF085C4C))
                .testTag("splash_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 12.dp,
                    color = Color(0xFF085C4C)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_book_icon),
                        contentDescription = "BCS Literature অ্যাপ আইকন",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(28.dp))
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "BCS Literature",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFDF7)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "বিসিএস বাংলা ও ইংরেজি সাহিত্য প্রস্তুতি",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFBCECE0),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PermissionExplanationDialog(
    needsLegacyRuntimePermission: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "অটো-ব্যাকআপ ও স্টোরেজ",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = if (needsLegacyRuntimePermission) {
                    "আপনার সংরক্ষিত সাহিত্য তথ্য নিরাপদ রাখতে অ্যাপটি স্বয়ংক্রিয়ভাবে ফোনের Documents/BCSLiterature/backup.json ফাইলে ব্যাকআপ রাখে। এতে অ্যাপের ডাটা ক্লিয়ার করলেও তথ্য ফিরে পাওয়া যায়। ব্যাকআপ চালু রাখতে স্টোরেজ অনুমতি দিন।"
                } else {
                    "আপনার সংরক্ষিত সাহিত্য তথ্য নিরাপদ রাখতে অ্যাপটি স্বয়ংক্রিয়ভাবে ফোনের Documents/BCSLiterature/backup.json ফাইলে ব্যাকআপ রাখবে। কোনো তথ্য যোগ, পরিবর্তন বা মুখস্থ করলে ব্যাকআপ নিজে থেকেই আপডেট হবে।"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("permission_confirm_button")
            ) {
                Text(if (needsLegacyRuntimePermission) "অনুমতি দিন" else "বুঝেছি")
            }
        },
        dismissButton = {
            if (needsLegacyRuntimePermission) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("permission_dismiss_button")
                ) {
                    Text("পরে")
                }
            }
        }
    )
}

@Composable
fun ImportModeDialog(
    pendingImport: PendingImport,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    val validCount = BengaliUtils.toBengaliDigits(pendingImport.result.validItems.size)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Merge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "মার্জ করব নাকি রিপ্লেস?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ফাইলে ${validCount}টি সঠিক সাহিত্য তথ্য পাওয়া গেছে। আপনি কীভাবে ইম্পোর্ট করতে চান?",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (pendingImport.result.skippedCount > 0) {
                    val skipped = BengaliUtils.toBengaliDigits(pendingImport.result.skippedCount)
                    Text(
                        text = "বিঃদ্রঃ ${skipped}টি অসম্পূর্ণ বা অবৈধ তথ্য স্বয়ংক্রিয়ভাবে বাদ দেওয়া হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onReplace,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_replace_button")
                ) {
                    Text("রিপ্লেস")
                }
                Button(
                    onClick = onMerge,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_merge_button")
                ) {
                    Text("মার্জ")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("import_cancel_button")
            ) {
                Text("বাতিল")
            }
        }
    )
}
