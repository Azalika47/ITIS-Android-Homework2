package com.example.homeworkauthorization

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage

// Ссылки на картинки (можно заменить на любые свои)
private const val IMAGE_MATCH = "https://http.cat/200"
private const val IMAGE_MISMATCH = "https://http.cat/403"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

/** Случайный пароль: минимум 8 символов, гарантированно есть латиница и цифры. */
fun generatePassword(length: Int = 10): String {
    val letters = ('a'..'z') + ('A'..'Z')
    val digits = '0'..'9'
    val chars = mutableListOf<Char>()
    chars += letters.random()
    chars += digits.random()
    repeat(length - 2) { chars += (letters + digits).random() }
    return chars.shuffled().joinToString("")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Генерируется при запуске, хранится в памяти (переживает и поворот экрана)
    val generatedPassword = rememberSaveable { generatePassword() }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onSuccess = { email, password ->
                navController.navigate("result/${Uri.encode(email)}/${Uri.encode(password)}")
            })
        }
        composable(
            route = "result/{email}/{password}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { entry ->
            val email = entry.arguments?.getString("email").orEmpty()
            val password = entry.arguments?.getString("password").orEmpty()
            ResultScreen(
                email = email,
                enteredPassword = password,
                generatedPassword = generatedPassword,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun LoginScreen(onSuccess: (String, String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        // Каждое поле проверяется отдельно
        emailError = when {
            email.isBlank() -> "Введите e-mail"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Некорректный e-mail"
            else -> null
        }
        passwordError = when {
            password.isEmpty() -> "Введите пароль"
            password.length < 8 ||
                    !password.any { it in 'a'..'z' || it in 'A'..'Z' } ||
                    !password.any { it.isDigit() } ->
                "Минимум 8 символов, латинские буквы и цифры"
            else -> null
        }
        if (emailError == null && passwordError == null) {
            onSuccess(email, password)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            // Опционально: кнопка "Далее" на клавиатуре = нажатие кнопки
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) {
            Text("Далее")
        }
    }
}

@Composable
fun ResultScreen(
    email: String,
    enteredPassword: String,
    generatedPassword: String,
    onBack: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val matches = enteredPassword == generatedPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = if (matches) IMAGE_MATCH else IMAGE_MISMATCH,
            contentDescription = null,
            modifier = Modifier.size(240.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(email, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = generatedPassword,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Сгенерированный пароль") },
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector = if (showPassword) Icons.Filled.VisibilityOff
                    else Icons.Filled.Visibility,
                    contentDescription = "Показать/скрыть пароль"
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Опционально: копирование в буфер обмена
        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(generatedPassword)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Скопировать пароль")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("На первый экран")
        }
    }
}