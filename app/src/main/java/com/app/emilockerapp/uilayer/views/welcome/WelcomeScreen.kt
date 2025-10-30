package com.app.emilockerapp.uilayer.views.welcome


import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.app.emilockerapp.uilayer.permission.ManagePermissionActivity
import kotlin.math.sin

@Composable
fun WelcomeScreen() {

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    val context = LocalContext.current

    // Pulse animation for icon
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        AnimatedGradientBackground(animatedOffset)

        // Floating orbs
        FloatingOrbs(animatedOffset)

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Star Icon with glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    modifier = Modifier
                        .size(80.dp)
                        .blur(20.dp),
                    tint = Color.White.copy(alpha = 0.3f)
                )
                // Main
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Welcome Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Your journey starts here.\nEnjoy a smooth experience with Emilocker.",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    startActivity(context, Intent(context, ManagePermissionActivity::class.java), null)
                },
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Permission")
            }

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
            Color(0xFF533483)
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = colors,
                startY = size.height * animatedOffset,
                endY = size.height * (1 + animatedOffset)
            )
        )
    }
}

@Composable
private fun FloatingOrbs(animatedOffset: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val orbCount = 6
        repeat(orbCount) { index ->
            val angle = (animatedOffset * 360 + index * 60) * (kotlin.math.PI / 180)
            val radius = size.minDimension * 0.1f
            val x = size.width * 0.5f + kotlin.math.cos(angle).toFloat() * radius * (index + 1) * 0.8f
            val y = size.height * 0.5f + sin(angle).toFloat() * radius * (index + 1) * 0.6f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = 40f + index * 10f
                ),
                radius = 40f + index * 10f,
                center = Offset(x, y)
            )
        }
    }
}

@Preview
@Composable
fun PreviewWelcomeScreen() {
    WelcomeScreen()
}
