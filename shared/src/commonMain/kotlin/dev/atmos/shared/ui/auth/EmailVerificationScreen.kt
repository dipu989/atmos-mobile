package dev.atmos.shared.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.AlertRed
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.ui.theme.Sage
import kotlinx.coroutines.delay

private const val RESEND_COOLDOWN_SECONDS = 30

@Composable
fun EmailVerificationScreen(
    email: String,
    onResend: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _ -> },
    onContinue: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current

    var resendLoading  by remember { mutableStateOf(false) }
    var resendError    by remember { mutableStateOf<String?>(null) }
    var resendSuccess  by remember { mutableStateOf(false) }
    var cooldownSecs   by remember { mutableStateOf(0) }

    // Countdown tick after a successful resend
    LaunchedEffect(resendSuccess) {
        if (!resendSuccess) return@LaunchedEffect
        cooldownSecs = RESEND_COOLDOWN_SECONDS
        while (cooldownSecs > 0) {
            delay(1_000)
            cooldownSecs--
        }
        resendSuccess = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        // ── Icon ──────────────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .background(Sage.copy(alpha = 0.12f), CircleShape),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(Sage.copy(alpha = 0.18f), CircleShape),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Email,
                    contentDescription = null,
                    tint               = Sage,
                    modifier           = Modifier.size(30.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Headline ──────────────────────────────────────────────────────────
        Text(
            text       = "Check your inbox",
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = colors.textPrimary,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "We sent a verification link to",
            fontSize  = 15.sp,
            color     = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text       = email,
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.textPrimary,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "Click the link in the email to verify your account.",
            fontSize  = 14.sp,
            color     = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(40.dp))

        // ── Resend button ─────────────────────────────────────────────────────
        val resendEnabled = !resendLoading && !resendSuccess && cooldownSecs == 0
        Button(
            onClick = {
                resendLoading = true
                resendError   = null
                onResend(
                    {   // onSuccess
                        resendLoading = false
                        resendSuccess = true
                    },
                    { err ->   // onError
                        resendLoading = false
                        resendError   = err
                    },
                )
            },
            enabled  = resendEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = HorizonBlue,
                disabledContainerColor = HorizonBlue.copy(alpha = 0.5f),
            ),
        ) {
            if (resendLoading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(20.dp),
                    color     = Color.White,
                    strokeWidth = 2.dp,
                )
            } else if (resendSuccess && cooldownSecs > 0) {
                Text(
                    text       = "Email sent — resend in ${cooldownSecs}s",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            } else {
                Text(
                    text       = "Resend email",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            }
        }

        // ── Inline error ──────────────────────────────────────────────────────
        if (resendError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text      = resendError!!,
                fontSize  = 13.sp,
                color     = AlertRed,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Continue link ─────────────────────────────────────────────────────
        TextButton(onClick = onContinue) {
            Text(
                text     = "Continue to app",
                fontSize = 14.sp,
                color    = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
