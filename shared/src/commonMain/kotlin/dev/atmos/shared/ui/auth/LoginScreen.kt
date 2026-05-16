package dev.atmos.shared.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun LoginScreen(
    onSignIn: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
) {
    val colors = LocalAtmosColors.current
    val scrollState = rememberScrollState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailTouched    by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    val emailError    = if (emailTouched && !email.isValidEmail())       "Enter a valid email address" else null
    val passwordError = if (passwordTouched && !password.isValidPassword()) "Password must be at least 8 characters" else null
    val isFormValid   = email.isValidEmail() && password.isValidPassword()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState),
    ) {
        // ── Branding header ───────────────────────────────────────────────────
        AuthBrandHeader(compact = false)

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp),
        ) {
            Text(
                text = "Welcome back",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sign in to continue your journey",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                ),
            )

            Spacer(Modifier.height(28.dp))

            // Email
            AuthTextField(
                value           = email,
                onValueChange   = { email = it },
                label           = "Email",
                placeholder     = "you@example.com",
                leadingIcon     = Icons.Outlined.Email,
                error           = emailError,
                onFocusLost     = { emailTouched = true },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Password
            AuthTextField(
                value         = password,
                onValueChange = { password = it },
                label         = "Password",
                placeholder   = "Min. 8 characters",
                leadingIcon   = Icons.Outlined.Lock,
                isPassword    = true,
                error         = passwordError,
                onFocusLost   = { passwordTouched = true },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        passwordTouched = true
                        if (isFormValid) onSignIn()
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            // Forgot password — right-aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Forgot password?",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HorizonBlue,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onForgotPassword)
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            // Sign In CTA
            AuthPrimaryButton(
                text = "Sign In",
                onClick = {
                    emailTouched    = true
                    passwordTouched = true
                    if (isFormValid) onSignIn()
                },
            )

            Spacer(Modifier.height(22.dp))
            OrDivider()
            Spacer(Modifier.height(16.dp))

            GoogleSignInButton(onClick = onSignIn)

            Spacer(Modifier.height(36.dp))

            AuthFooterRow(
                prompt      = "Don't have an account?  ",
                actionLabel = "Sign up",
                onAction    = onNavigateToSignUp,
            )
        }
    }
}
