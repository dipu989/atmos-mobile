package dev.atmos.shared.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Sage

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onBackToSignIn: () -> Unit = {},
    onSendResetLink: (email: String) -> Unit = {},
    sendLoading: Boolean = false,
    sendError: String? = null,
    showSuccess: Boolean = false,
) {
    val colors = LocalAtmosColors.current
    val scrollState = rememberScrollState()

    var email        by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }

    val emailError = if (emailTouched && !email.isValidEmail()) "Enter a valid email address" else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textSecondary,
                )
            }
        }

        // ── Animated content: form ↔ success ──────────────────────────────────
        AnimatedContent(
            targetState = showSuccess,
            transitionSpec = {
                (slideInVertically { it / 4 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 4 } + fadeOut())
            },
            label = "forgot-state",
        ) { isSuccess ->
            if (!isSuccess) {
                ForgotForm(
                    email        = email,
                    onEmailChange = { email = it },
                    emailTouched = emailTouched,
                    emailError   = emailError,
                    onFocusLost  = { emailTouched = true },
                    sendLoading  = sendLoading,
                    sendError    = sendError,
                    onSend       = {
                        emailTouched = true
                        if (email.isValidEmail()) onSendResetLink(email.trim())
                    },
                )
            } else {
                ForgotSuccess(
                    email          = email,
                    sendLoading    = sendLoading,
                    sendError      = sendError,
                    onBackToSignIn = onBackToSignIn,
                    onResend       = { onSendResetLink(email.trim()) },
                )
            }
        }
    }
}

// ── Form state ────────────────────────────────────────────────────────────────

@Composable
private fun ForgotForm(
    email: String,
    onEmailChange: (String) -> Unit,
    emailTouched: Boolean,
    emailError: String?,
    onFocusLost: () -> Unit,
    onSend: () -> Unit,
    sendLoading: Boolean = false,
    sendError: String? = null,
) {
    val colors = LocalAtmosColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(HorizonBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = HorizonBlue,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Forgot your password?",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enter your email and we'll send you a secure link to reset your password.",
            style = TextStyle(
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            ),
        )

        Spacer(Modifier.height(36.dp))

        AuthTextField(
            value         = email,
            onValueChange = onEmailChange,
            label         = "Email",
            placeholder   = "you@example.com",
            leadingIcon   = Icons.Outlined.Email,
            error         = emailError,
            onFocusLost   = onFocusLost,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction    = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSend() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))

        AuthPrimaryButton(
            text    = "Send reset link",
            loading = sendLoading,
            onClick = onSend,
        )

        if (sendError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = sendError,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = dev.atmos.shared.ui.theme.AlertRed,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ── Success state ─────────────────────────────────────────────────────────────

@Composable
private fun ForgotSuccess(
    email: String,
    onBackToSignIn: () -> Unit,
    onResend: () -> Unit = {},
    sendLoading: Boolean = false,
    sendError: String? = null,
) {
    val colors = LocalAtmosColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // Success icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Sage.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Sage,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Check your inbox",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "We sent a reset link to\n$email",
            style = TextStyle(
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Didn't get it? Check your spam folder or wait a minute.",
            style = TextStyle(
                fontSize = 12.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            ),
        )

        Spacer(Modifier.height(36.dp))

        AuthPrimaryButton(
            text    = "Back to Sign In",
            onClick = onBackToSignIn,
        )

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick  = onResend,
            enabled  = !sendLoading,
            shape    = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "Resend email",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HorizonBlue,
                ),
            )
        }

        if (sendError != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = sendError,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = dev.atmos.shared.ui.theme.AlertRed,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
