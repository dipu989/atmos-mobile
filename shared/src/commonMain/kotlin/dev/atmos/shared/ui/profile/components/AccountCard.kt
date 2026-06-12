package dev.atmos.shared.ui.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.common.AtmosCard
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCard(
    onExportData: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: (password: String, onError: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    var showSignOutDialog  by remember { mutableStateOf(false) }
    var showDeleteSheet    by remember { mutableStateOf(false) }

    AtmosCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = "Account",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExportData)
                .padding(vertical = 12.dp),
        ) {
            Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null, tint = HorizonBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = "Export My Data", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = HorizonBlue)
        }

        HorizontalDivider(thickness = 1.dp, color = colors.divider)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSignOutDialog = true }
                .padding(vertical = 12.dp),
        ) {
            Text(text = "Sign Out", fontSize = 15.sp, color = colors.textSecondary)
        }

        HorizontalDivider(thickness = 1.dp, color = colors.divider)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteSheet = true }
                .padding(vertical = 12.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = "Delete Account", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AlertRed)
        }
    }

    // ── Sign-out confirmation ─────────────────────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", color = colors.textPrimary) },
            text = { Text("Are you sure you want to sign out?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) {
                    Text("Sign Out", color = AlertRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
        )
    }

    // ── Delete account sheet ──────────────────────────────────────────────────
    if (showDeleteSheet) {
        DeleteAccountSheet(
            onConfirm  = { password, onError -> onDeleteAccount(password, onError) },
            onDismiss  = { showDeleteSheet = false },
        )
    }
}

// ── Delete account bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountSheet(
    onConfirm: (password: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    var password   by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf<String?>(null) }
    var isLoading  by remember { mutableStateOf(false) }
    var hasFocus   by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue   = when {
            error != null -> AlertRed
            hasFocus      -> AlertRed.copy(alpha = 0.6f)
            else          -> colors.divider
        },
        animationSpec = tween(200),
        label         = "deletePasswordBorder",
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.surface,
        tonalElevation   = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 40.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint               = AlertRed,
                    modifier           = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "Delete account",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AlertRed,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Warning text ──────────────────────────────────────────────────
            Text(
                text      = "Your account enters a 7-day grace period. Log back in within 7 days to cancel. After that, all your trips, insights, and preferences are permanently erased.",
                fontSize  = 14.sp,
                color     = colors.textSecondary,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(24.dp))

            // ── Password field ────────────────────────────────────────────────
            Text(
                text     = "Current password",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color    = colors.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value                  = password,
                onValueChange          = { password = it; error = null },
                singleLine             = true,
                visualTransformation   = PasswordVisualTransformation(),
                cursorBrush            = SolidColor(AlertRed),
                textStyle              = TextStyle(fontSize = 15.sp, color = colors.textPrimary),
                modifier               = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { hasFocus = it.isFocused },
                decorationBox = { innerField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(if (hasFocus || error != null) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint               = if (hasFocus) AlertRed.copy(alpha = 0.7f) else colors.textTertiary,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (password.isEmpty()) {
                                Text("Enter password", style = TextStyle(fontSize = 15.sp, color = colors.textTertiary))
                            }
                            innerField()
                        }
                    }
                },
            )

            // ── Google sign-in hint ───────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            Text(
                text     = "Signed in with Google? Leave the password blank.",
                fontSize = 12.sp,
                color    = colors.textTertiary,
            )

            // ── Inline error ──────────────────────────────────────────────────
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = error!!, fontSize = 13.sp, color = AlertRed)
            }

            Spacer(Modifier.height(28.dp))

            // ── Delete button ─────────────────────────────────────────────────
            Button(
                onClick  = {
                    isLoading = true
                    error     = null
                    onConfirm(password) { msg ->
                        isLoading = false
                        error     = msg
                    }
                },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = AlertRed,
                    disabledContainerColor = AlertRed.copy(alpha = 0.5f),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Delete my account", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { if (!isLoading) onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = colors.textSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
