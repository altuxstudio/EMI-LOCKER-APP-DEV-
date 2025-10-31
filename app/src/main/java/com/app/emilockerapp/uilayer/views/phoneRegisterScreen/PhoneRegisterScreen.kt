package com.app.emilockerapp.uilayer.views.phoneRegisterScreen

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.app.emilockerapp.coordinator.BaseChildNavGraph
import com.app.emilockerapp.datalayer.viewmodels.PhoneRegisterViewModel
import com.app.emilockerapp.uilayer.views.welcome.WelcomeScreenNavGraph
import com.app.emilockerapp.utils.ComposeBaseExtensions.routeWithArgs
import kotlin.math.sin

class PhoneRegisterScreen(private val nav: NavHostController) : BaseChildNavGraph {

    object Routes {
        const val phoneRegister = "emi_locker/phone_register"
    }

    override fun createChildNavGraphBuilder(): NavGraphBuilder.() -> Unit = {
        composable(routeWithArgs(Routes.phoneRegister)) { PhoneRegisterUI(nav) }
    }
}

@Composable
fun PhoneRegisterUI(
    nav: NavHostController,
    vm: PhoneRegisterViewModel = viewModel()
) {
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

    var token by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }

    val state by vm.uiState.collectAsState()

    val context = LocalContext.current


    // Success → go to Welcome screen
    LaunchedEffect(state.success) {
        if (state.success) {
            Toast.makeText(context, "Registered successfully", Toast.LENGTH_SHORT).show()
            nav.navigate(WelcomeScreenNavGraph.Routes.welcomeScreen) {
                popUpTo(PhoneRegisterScreen.Routes.phoneRegister) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Simple gradient bg like Login


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        // Animated gradient background
        AnimatedGradientBackground(animatedOffset)

        // Floating particles
        FloatingParticles(animatedOffset)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Phone Registration",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text("Enter your token and device IMEI", color = Color.White.copy(.8f))
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("Token", color = Color.White.copy(.7f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color.White.copy(.7f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(.5f),
                                unfocusedBorderColor = Color.White.copy(.3f),
                                cursorColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = imei,
                            onValueChange = { imei = it.filter { ch -> ch.isDigit() }.take(15) },
                            label = { Text("IMEI (15 digits)", color = Color.White.copy(.7f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color.White.copy(.7f)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(.5f),
                                unfocusedBorderColor = Color.White.copy(.3f),
                                cursorColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val canSubmit = token.isNotBlank() && imei.length in 14..16
                        Button(
                            onClick = { vm.register(token, imei) },
                            enabled = canSubmit && !state.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF667eea),
                                                Color(0xFF764ba2)
                                            )
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (state.isLoading) "Registering..." else "Register",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (state.errorMessage != null) {
                            Text(
                                text = state.errorMessage ?: "",
                                color = Color(0xFFFFD2D2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .clickable { vm.clearError() },
                            )
                        }
                    }
                }
            }

        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun FloatingParticles(animatedOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val particleCount = 8
        repeat(particleCount) { index ->
            val angle = (animatedOffset * 180 + index * 45) * (kotlin.math.PI / 180)
            val radius = size.minDimension * 0.15f
            val x = size.width * 0.5f + kotlin.math.cos(angle)
                .toFloat() * radius * (index % 3 + 1) * 0.7f
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
