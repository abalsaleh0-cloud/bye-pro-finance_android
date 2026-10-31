package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finance.auth.AuthOutcome
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.model.AppLanguage
import com.example.ui.theme.ByeProTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(language: AppLanguage, onAuthenticated: () -> Unit) {
    var signup by rememberSaveable { mutableStateOf(false) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repository = remember(context) { SupabaseAuthRepository(context) }
    val scope = rememberCoroutineScope()
    fun tr(ar: String, en: String) = if (language == AppLanguage.AR) ar else en

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(62.dp).background(ByeProTheme.colors.goldPrimary.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = ByeProTheme.colors.goldPrimary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("BYE PRO FINANCE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = ByeProTheme.colors.goldPrimary)
                Text(tr("نظامك المالي في مكان واحد", "Your finances in one place"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(22.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(tr(if (signup) "إنشاء حساب جديد" else "تسجيل الدخول", if (signup) "Create account" else "Sign in"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(tr("أدخل بياناتك للمتابعة بأمان.", "Enter your details to continue securely."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        if (signup) {
                            AuthTextField(fullName, { fullName = it }, tr("الاسم الكامل", "Full name"), "auth_full_name")
                            AuthTextField(username, { username = it }, tr("اسم المستخدم", "Username"), "auth_username")
                        }
                        AuthTextField(email, { email = it }, tr("البريد الإلكتروني", "Email address"), "auth_email", KeyboardType.Email)
                        PasswordField(password, { password = it }, tr("كلمة المرور", "Password"), passwordVisible, { passwordVisible = !passwordVisible }, tr("إظهار كلمة المرور", "Show password"), tr("إخفاء كلمة المرور", "Hide password"), "auth_password")
                        if (signup) PasswordField(confirm, { confirm = it }, tr("تأكيد كلمة المرور", "Confirm password"), confirmVisible, { confirmVisible = !confirmVisible }, tr("إظهار تأكيد كلمة المرور", "Show confirm password"), tr("إخفاء تأكيد كلمة المرور", "Hide confirm password"), "auth_confirm_password")
                        message?.let { Text(it, color = if (error) MaterialTheme.colorScheme.error else ByeProTheme.colors.statusSuccess, style = MaterialTheme.typography.bodySmall) }
                        Button(
                            onClick = {
                                message = null
                                when {
                                    !email.trim().contains("@") -> { message = tr("أدخل بريداً إلكترونياً صحيحاً.", "Enter a valid email address."); error = true }
                                    password.length < 8 -> { message = tr("كلمة المرور يجب ألا تقل عن 8 أحرف.", "Password must be at least 8 characters."); error = true }
                                    signup && fullName.isBlank() -> { message = tr("أدخل الاسم الكامل.", "Enter your full name."); error = true }
                                    signup && username.isBlank() -> { message = tr("أدخل اسم المستخدم.", "Enter a username."); error = true }
                                    signup && password != confirm -> { message = tr("كلمتا المرور غير متطابقتين.", "Passwords do not match."); error = true }
                                    else -> {
                                        loading = true
                                        error = false
                                        scope.launch {
                                            val result = if (signup) repository.signUp(fullName, username, email, password) else repository.signIn(email, password)
                                            loading = false
                                            result.onSuccess { outcome ->
                                                when (outcome) {
                                                    AuthOutcome.Authenticated -> onAuthenticated()
                                                    AuthOutcome.NeedsEmailConfirmation -> {
                                                        message = tr("تم إنشاء الحساب. تحقق من بريدك الإلكتروني لتأكيده، ثم سجّل الدخول.", "Account created. Check your email to confirm it, then sign in.")
                                                        signup = false
                                                        password = ""
                                                        confirm = ""
                                                    }
                                                }
                                            }.onFailure {
                                                message = it.message ?: tr("تعذر إتمام العملية. حاول مجدداً.", "Unable to complete the request. Try again.")
                                                error = true
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_submit")
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            else Text(tr(if (signup) "إنشاء الحساب" else "تسجيل الدخول", if (signup) "Create account" else "Sign in"))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(tr(if (signup) "لديك حساب بالفعل؟" else "ليس لديك حساب؟", if (signup) "Already have an account?" else "Don't have an account?"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = {
                                signup = !signup
                                message = null
                                password = ""
                                confirm = ""
                                passwordVisible = false
                                confirmVisible = false
                            }) { Text(tr(if (signup) "تسجيل الدخول" else "إنشاء حساب", if (signup) "Sign in" else "Create account")) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(tr("مصادقة آمنة عبر Supabase", "Secure authentication with Supabase"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, tag: String, type: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = type),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ByeProTheme.colors.fieldFocus, unfocusedBorderColor = ByeProTheme.colors.fieldBorder,
            focusedContainerColor = ByeProTheme.colors.fieldBg, unfocusedContainerColor = ByeProTheme.colors.fieldBg
        )
    )
}

@Composable
private fun PasswordField(
    value: String, onValueChange: (String) -> Unit, label: String, visible: Boolean,
    onToggle: () -> Unit, showDescription: String, hideDescription: String, tag: String
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) }, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggle, modifier = Modifier.testTag("${tag}_visibility")) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) hideDescription else showDescription,
                    tint = ByeProTheme.colors.goldPrimary
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ByeProTheme.colors.fieldFocus, unfocusedBorderColor = ByeProTheme.colors.fieldBorder,
            focusedContainerColor = ByeProTheme.colors.fieldBg, unfocusedContainerColor = ByeProTheme.colors.fieldBg
        )
    )
}
