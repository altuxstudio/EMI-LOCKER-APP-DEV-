package com.app.emilockerapp.uilayer.views.auth

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.emilockerapp.datalayer.viewmodels.AuthViewModel
import kotlin.math.sin

@Composable
fun LoginUiScreen(viewModel: AuthViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val state by viewModel.loginState.collectAsState()

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    // Scale animation for logo
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    if (state.success) {
        // Navigate to Dashboard
        Toast.makeText(LocalContext.current, "Login Successful", Toast.LENGTH_SHORT).show()
    }

    state.errorMessage?.let { error ->
        Toast.makeText(LocalContext.current, error, Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated gradient background
        AnimatedGradientBackground(animatedOffset)

        // Floating particles
        FloatingParticles(animatedOffset)

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp)) // Add top padding for better scroll spacing

            // Logo section with animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .blur(15.dp),
                    tint = Color.White.copy(alpha = 0.3f)
                )
                // Main logo
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Logo",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Welcome text
            Text(
                text = "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sign in to your account",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Login form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Username field
                    ModernTextField(
                        label = "Username",
                        value = username,
                        onValueChange = { username = it },
                        leadingIcon = Icons.Default.Person,
                        keyboardType = KeyboardType.Text
                    )

                    // Password field
                    ModernTextField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )

                    // Remember me checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.White,
                                    uncheckedColor = Color.White.copy(alpha = 0.6f),
                                    checkmarkColor = Color(0xFF667eea)
                                )
                            )
                            Text(
                                text = "Remember me",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "Forgot Password?",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { /* Handle forgot password */ }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login button
            ModernActionButton(
                buttonText = "Sign In",
                onClick = {
                    viewModel.login(username, password)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Social login section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f)
                )
                Text(
                    text = "  contact  ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social login buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SocialLoginButton(
                    icon = Icons.Default.Email,
                    onClick = { /* Handle Google login */ },
                    modifier = Modifier.weight(1f)
                )
                SocialLoginButton(
                    icon = Icons.Default.Phone,
                    onClick = { /* Handle phone login */ },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign up text
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign Up",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* Handle sign up */ }
                )
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ModernTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = Color.White.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f)
            )
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ModernActionButton(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AnimatedGradientBackground(animatedOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val colors = listOf(
            Color(0xFF1a1a2e),
            Color(0xFF16213e),
            Color(0xFF0f3460),
            Color(0xFF533483),
            Color(0xFF667eea)
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = colors,
                startY = size.height * animatedOffset * 0.5f,
                endY = size.height * (1 + animatedOffset * 0.5f)
            )
        )
    }
}

@Composable
private fun FloatingParticles(animatedOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val particleCount = 8
        repeat(particleCount) { index ->
            val angle = (animatedOffset * 180 + index * 45) * (kotlin.math.PI / 180)
            val radius = size.minDimension * 0.15f
            val x = size.width * 0.5f + kotlin.math.cos(angle).toFloat() * radius * (index % 3 + 1) * 0.7f
            val y = size.height * 0.3f + sin(angle).toFloat() * radius * (index % 2 + 1) * 0.4f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = 20f + index * 8f
                ),
                radius = 20f + index * 8f,
                center = Offset(x, y)
            )
        }
    }
}

@Preview
@Composable
fun PreviewModernLoginScreen() {
    LoginUiScreen()
}