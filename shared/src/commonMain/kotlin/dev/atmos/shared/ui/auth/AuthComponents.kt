package dev.atmos.shared.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Sage

// ── Validation helpers ────────────────────────────────────────────────────────

fun String.isValidEmail(): Boolean = contains("@") && contains(".") && length >= 5
fun String.isValidPassword(): Boolean = length >= 8

// ── AuthTextField ─────────────────────────────────────────────────────────────

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = label,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    error: String? = null,
    onFocusLost: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = LocalAtmosColors.current
    var hasFocus by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> AlertRed
            hasFocus      -> HorizonBlue
            else          -> colors.divider
        },
        animationSpec = tween(200),
        label = "border",
    )
    val borderWidth = if (hasFocus || error != null) 2.dp else 1.dp
    val iconTint = if (hasFocus) HorizonBlue else colors.textTertiary

    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (error != null) AlertRed else colors.textSecondary,
                letterSpacing = 0.3.sp,
            ),
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(HorizonBlue),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = colors.textPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    val losing = hasFocus && !focusState.isFocused
                    hasFocus = focusState.isFocused
                    if (losing) onFocusLost()
                },
            decorationBox = { innerField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = colors.textTertiary,
                                ),
                            )
                        }
                        innerField()
                    }
                    if (isPassword) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Outlined.VisibilityOff
                                else
                                    Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            },
        )
        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = error ?: "",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = AlertRed,
                ),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

// ── AuthPrimaryButton ─────────────────────────────────────────────────────────

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HorizonBlue,
            disabledContainerColor = HorizonBlue.copy(alpha = 0.45f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                ),
            )
        }
    }
}

// ── GoogleSignInButton ────────────────────────────────────────────────────────

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val colors = LocalAtmosColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.divider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            disabledContainerColor = colors.surface.copy(alpha = 0.6f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color(0xFF4285F4),
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "G",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4),
                ),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Continue with Google",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                ),
            )
        }
    }
}

// ── OrDivider ─────────────────────────────────────────────────────────────────

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    val colors = LocalAtmosColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), color = colors.divider)
        Text(
            text = "  or  ",
            style = TextStyle(
                fontSize = 13.sp,
                color = colors.textTertiary,
            ),
        )
        HorizontalDivider(Modifier.weight(1f), color = colors.divider)
    }
}

// ── AuthBrandHeader ───────────────────────────────────────────────────────────

@Composable
fun AuthBrandHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAtmosColors.current
    val headerHeight = if (compact) 160.dp else 230.dp
    val iconSize    = if (compact) 52.dp else 64.dp
    val iconInner   = if (compact) 26.dp else 34.dp
    val titleSize   = if (compact) 26.sp else 32.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight),
        contentAlignment = Alignment.Center,
    ) {
        // ── Soft atmospheric canvas decoration ────────────────────────────────
        Canvas(Modifier.matchParentSize()) {
            // Large haze — top-right
            drawCircle(
                color = HorizonBlue.copy(alpha = 0.07f),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.88f, size.height * 0.08f),
            )
            // Medium sage haze — bottom-left
            drawCircle(
                color = Sage.copy(alpha = 0.06f),
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.08f, size.height * 0.92f),
            )
            // Small blue accent — upper-left
            drawCircle(
                color = HorizonBlue.copy(alpha = 0.10f),
                radius = size.width * 0.14f,
                center = Offset(size.width * 0.14f, size.height * 0.22f),
            )
            // Tiny sage accent — lower-right
            drawCircle(
                color = Sage.copy(alpha = 0.08f),
                radius = size.width * 0.09f,
                center = Offset(size.width * 0.88f, size.height * 0.82f),
            )
        }

        // ── Logo + wordmark ───────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(HorizonBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = "atmos",
                    tint = Color.White,
                    modifier = Modifier.size(iconInner),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "atmos",
                style = TextStyle(
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.8).sp,
                ),
            )
            if (!compact) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your carbon journey, tracked",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                    ),
                )
            }
        }
    }
}

// ── AuthFooterRow ─────────────────────────────────────────────────────────────

@Composable
fun AuthFooterRow(
    prompt: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAtmosColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = prompt,
            style = TextStyle(
                fontSize = 14.sp,
                color = colors.textSecondary,
            ),
        )
        Text(
            text = actionLabel,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HorizonBlue,
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}
