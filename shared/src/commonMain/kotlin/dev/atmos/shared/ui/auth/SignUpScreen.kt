package dev.atmos.shared.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors

@Composable
fun SignUpScreen(
    /** Called when the user taps "Create Account" with valid form data. */
    onCreateAccount: (name: String, email: String, password: String) -> Unit = { _, _, _ -> },
    onNavigateToSignIn: () -> Unit = {},
    // Real Google Sign-In callback — wired up in AtmosApp.
    onGoogleSignIn: () -> Unit = {},
    googleSignInLoading: Boolean = false,
    googleSignInError: String? = null,
    emailSignUpLoading: Boolean = false,
    emailSignUpError: String? = null,
) {
    val colors = LocalAtmosColors.current
    val scrollState = rememberScrollState()

    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted   by remember { mutableStateOf(false) }

    var nameTouched            by remember { mutableStateOf(false) }
    var emailTouched           by remember { mutableStateOf(false) }
    var passwordTouched        by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }

    val nameError =
        if (nameTouched && name.isBlank()) "Please enter your name" else null
    val emailError =
        if (emailTouched && !email.isValidEmail()) "Enter a valid email address" else null
    val passwordError =
        if (passwordTouched && !password.isValidPassword()) "Password must be at least 8 characters" else null
    val confirmPasswordError = when {
        !confirmPasswordTouched          -> null
        confirmPassword != password      -> "Passwords do not match"
        else                             -> null
    }

    val isFormValid = name.isNotBlank()
        && email.isValidEmail()
        && password.isValidPassword()
        && confirmPassword == password
        && termsAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState),
    ) {
        // ── Compact branding header ───────────────────────────────────────────
        AuthBrandHeader(compact = true)

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp),
        ) {
            Text(
                text = "Create your account",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Start tracking your carbon footprint today",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                ),
            )

            Spacer(Modifier.height(28.dp))

            // Full name
            AuthTextField(
                value         = name,
                onValueChange = { name = it },
                label         = "Full name",
                placeholder   = "Shantnu Kumar",
                leadingIcon   = Icons.Outlined.Person,
                error         = nameError,
                onFocusLost   = { nameTouched = true },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Email
            AuthTextField(
                value         = email,
                onValueChange = { email = it },
                label         = "Email",
                placeholder   = "you@example.com",
                leadingIcon   = Icons.Outlined.Email,
                error         = emailError,
                onFocusLost   = { emailTouched = true },
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
                    imeAction    = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Confirm password
            AuthTextField(
                value         = confirmPassword,
                onValueChange = { confirmPassword = it },
                label         = "Confirm password",
                placeholder   = "Repeat your password",
                leadingIcon   = Icons.Outlined.Lock,
                isPassword    = true,
                error         = confirmPasswordError,
                onFocusLost   = { confirmPasswordTouched = true },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        confirmPasswordTouched = true
                        if (isFormValid) onCreateAccount(name, email, password)
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // Terms checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor   = HorizonBlue,
                        uncheckedColor = colors.textTertiary,
                    ),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.textSecondary, fontSize = 13.sp)) {
                            append("I agree to the ")
                        }
                        withStyle(SpanStyle(
                            color      = HorizonBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 13.sp,
                        )) {
                            append("Terms of Service")
                        }
                        withStyle(SpanStyle(color = colors.textSecondary, fontSize = 13.sp)) {
                            append(" and ")
                        }
                        withStyle(SpanStyle(
                            color      = HorizonBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 13.sp,
                        )) {
                            append("Privacy Policy")
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { termsAccepted = !termsAccepted },
                )
            }

            Spacer(Modifier.height(28.dp))

            // Create Account CTA
            AuthPrimaryButton(
                text    = "Create Account",
                loading = emailSignUpLoading,
                enabled = !googleSignInLoading,
                onClick = {
                    nameTouched            = true
                    emailTouched           = true
                    passwordTouched        = true
                    confirmPasswordTouched = true
                    if (isFormValid) onCreateAccount(name, email, password)
                },
            )

            // Show error below the create-account button if it failed
            if (emailSignUpError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = emailSignUpError,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = dev.atmos.shared.ui.theme.AlertRed,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(22.dp))
            OrDivider()
            Spacer(Modifier.height(16.dp))

            GoogleSignInButton(
                onClick = onGoogleSignIn,
                loading = googleSignInLoading,
                enabled = !emailSignUpLoading,
            )

            // Show error below the Google button if Google sign-in failed
            if (googleSignInError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = googleSignInError,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = dev.atmos.shared.ui.theme.AlertRed,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(36.dp))

            AuthFooterRow(
                prompt      = "Already have an account?  ",
                actionLabel = "Sign in",
                onAction    = onNavigateToSignIn,
            )
        }
    }
}

