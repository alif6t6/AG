package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Pulse animation for glow
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowRatio"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2200) // auto navigate after 2.2s as specified in PRD
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Pulse background glow
        Box(
            modifier = Modifier
                .size(240.dp)
                .alpha(0.08f * glowScale)
                .blur(50.dp)
                .background(DevilRed, CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated logo icon
            AnimatedVisibility(
                visible = startAnimation,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.5f)) + fadeIn()
            ) {
                Text(
                    text = "😈",
                    fontSize = 72.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVIL ",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "GPT",
                    color = DevilRed,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "YOUR AI-POWERED LIFE OS",
                color = TextGrey,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // Progress loader at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .width(120.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DarkSurface2)
        ) {
            val progressProgress = remember { Animatable(0f) }
            LaunchedEffect(true) {
                progressProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressProgress.value)
                    .background(DevilRed)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    val slides = listOf(
        OnboardingSlide(
            "😈",
            "Meet Your AI Agent",
            "Say hello to DEVIL GPT, a ruthlessly efficient, witty personal AI that reads and manages your life details. Type goals or expenses directly."
        ),
        OnboardingSlide(
            "📊",
            "Track Every Taka",
            "Instantly visualize monthly spending charts, set strict category cash limits, and avoid critical wallet emergencies before they hit."
        ),
        OnboardingSlide(
            "✅",
            "Own Your Day",
            "Keep neat checklists, schedule persistent reminders, and compose smart notes — all integrated in one cohesive pocket OS."
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Skip Button top right
        Text(
            text = "SKIP",
            fontFamily = FontFamily.Monospace,
            color = DevilRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .clickable { onFinished() }
                .padding(8.dp)
        )

        // Slide contents
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slide.icon,
                    fontSize = 90.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Text(
                    text = slide.title,
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = slide.description,
                    color = TextGrey,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // Action controls (Indicator dots + Button) at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(3) { idx ->
                    val isActive = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isActive) DevilRed else BorderColor)
                    )
                }
            }

            // Primary buttons
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == 2) "ENTER INFERNO" else "NEXT",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

data class OnboardingSlide(
    val icon: String,
    val title: String,
    val description: String
)
