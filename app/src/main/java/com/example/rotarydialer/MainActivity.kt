package com.example.rotarydialer

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rotarydialer.ui.theme.RotaryDialerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var passcode by remember {
                mutableStateOf("")
            }

            RotaryDialerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "ENTER\nPASSCODE \uD83E\uDD2B",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 70.dp)
                        )

                        Passcode(
                            passcode = passcode,
                            actualPasscode = "1357",
                            onSuccess = {
                                passcode = ""
                            },
                            onFailure = {
                                passcode = ""
                            },
                            modifier = Modifier.align(Alignment.End).padding(top = 50.dp)
                        )

                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            RotaryDialer {
                                passcode += it.toString()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RotaryDialer(onNewDigit: (Int) -> Unit) {
    val angle = remember {
        Animatable(0f)
    }

    var digitDialDone by remember {
        mutableStateOf(false)
    }

    var initialTheta = remember {
        0f
    }

    var canvasCenter = remember {
        Offset.Zero
    }

    val angleBetweenDigits = remember {
        30f
    }

    val startAngleOfWholeDigits = remember {
        -70f
    }

    val textRect = remember {
        android.graphics.Rect()
    }
    val digitButtonRadius = remember { 80f }
    val arcWidth = remember { (digitButtonRadius * 2) * 1.4f }

    val coroutine = rememberCoroutineScope()

    val digits: List<Digit> = remember {
        (1..9).plus(0).reversed().mapIndexed { index, value ->
            val theta =
                ((index * angleBetweenDigits) * Math.PI / 180) - startAngleOfWholeDigits * (Math.PI / 180f)
            Digit(thetaRad = theta, value = value)
        }
    }


    Canvas(
        modifier = Modifier
            .width(400.dp)
            .height(400.dp)
            .aspectRatio(1f)
            .onGloballyPositioned {
                val windowBounds = it.boundsInParent()
                canvasCenter = Offset(windowBounds.size.width / 2, windowBounds.size.height / 2)
            }
            .pointerInteropFilter { event ->
                val theta = atan2(
                    event.y - canvasCenter.y,
                    event.x - canvasCenter.x,
                ) * (180f / Math.PI).toFloat()


                var changeInTheta = theta - initialTheta

                if (changeInTheta < 0) {
                    changeInTheta += 360f
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        digitDialDone = false
                        initialTheta = theta
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!theta.isNaN()) {
                            if (digitDialDone) {
                                return@pointerInteropFilter true
                            }
                            val lowerBound = 360f - Math
                                .toDegrees(digits.last().thetaRad)
                                .toFloat() + digitButtonRadius * 0.18f
                            val upperBound = Math
                                .toDegrees(digits.first().thetaRad)
                                .toFloat() - digitButtonRadius * 0.18f

                            if (theta in lowerBound..upperBound) {
                                digitDialDone = true
                                val digit =
                                    round(changeInTheta / angleBetweenDigits).roundToInt() - 1
                                onNewDigit(if (digit > 9) 0 else digit)
                                false
                            } else {
                                coroutine.launch {
                                    angle.snapTo(changeInTheta)
                                }
                                true
                            }
                        } else false
                    }
                    MotionEvent.ACTION_UP -> {
                        coroutine.launch {
                            angle.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                        initialTheta = 0f
                        true
                    }
                    else -> false
                }
            }
    ) {
        val radius = size.width / 2

        val digitsCircleRadius = radius * 0.70f
        val textPaint = Paint().apply {
            color = Color.White.hashCode()
            textSize = size.width / 12
            typeface = Typeface.DEFAULT_BOLD
        }

        rotate(angle.value) {
            drawCircle(color = Color.Black, radius = size.width / 2)

            //I took the values in the arc by doing things experimentally. Please let me know if there are any good ways
            drawArc(
                color = Color.White,
                size = Size(width = size.width * 0.70f, height = size.height * 0.70f),
                topLeft = Offset(size.width * 0.148148f, size.width * 0.148148f),
                startAngle = -90f - startAngleOfWholeDigits,
                sweepAngle = -angleBetweenDigits * 9,
                useCenter = false,
                style = Stroke(arcWidth, cap = StrokeCap.Round),
                blendMode = BlendMode.Src
            )

            //We simply translate to the center and draws the digits circularly from here
            drawContext.canvas.translate(center.x, center.y)
            digits.forEach { digit ->
                val x = digitsCircleRadius * cos(digit.thetaRad)
                val y = digitsCircleRadius * sin(digit.thetaRad)

                drawCircle(
                    color = Color.Black,
                    radius = digitButtonRadius,
                    center = Offset(
                        x.toFloat(),
                        y.toFloat(),
                    ),
                    blendMode = BlendMode.SrcOut
                )
            }
            //Resetting the canvas back to it's position
            drawContext.canvas.translate(-center.x, -center.y)
        }

        drawContext.canvas.translate(center.x, center.y)
        digits.forEach { digit ->
            textPaint.getTextBounds(digit.value.toString(), 0, 1, textRect)

            //Point on a circle (x, y) = (r * cos(theta), r * sin(theta))
            val x = digitsCircleRadius * cos(digit.thetaRad) - textRect.width() / 2
            val y = digitsCircleRadius * sin(digit.thetaRad) + textRect.height() / 2

            drawContext.canvas
                .nativeCanvas
                .drawText(
                    digit.value.toString(),
                    x.toFloat(),
                    y.toFloat(),
                    textPaint
                )
        }

        //Drawing the dot after 0
        val theta =
            ((((digits.size + 1) * angleBetweenDigits) * Math.PI / 180) - startAngleOfWholeDigits * (Math.PI / 180f)).toFloat()
        val x = digitsCircleRadius * cos(theta) + 10f
        val y = digitsCircleRadius * sin(theta) - 10f
        drawCircle(color = Color.White, radius = 25f, center = Offset(x, y))
        drawContext.canvas.translate(-center.x, -center.y)


        drawCircle(color = Color.White, radius = size.width / 4.8f)
    }
}

@Composable
fun Passcode(
    passcode: String,
    actualPasscode: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fillColor = remember {
        Animatable(Color.White)
    }

    LaunchedEffect(key1 = actualPasscode, key2 = passcode, block = {
        if (passcode.length < 4) {
            if (fillColor.value != Color.White) fillColor.animateTo(
                Color.White,
                animationSpec = spring(dampingRatio = Spring.StiffnessLow)
            )
            return@LaunchedEffect
        }

        if (passcode == actualPasscode) {
            fillColor.animateTo(Color.Green, animationSpec = tween(1000))
            fillColor.snapTo(Color.White)
            onSuccess()
        } else {
            fillColor.animateTo(Color.Red, spring())
            fillColor.animateTo(Color.White, tween())
            fillColor.animateTo(Color.Red, spring())
            fillColor.animateTo(Color.White)
            onFailure()
        }
    })

    Row(modifier = modifier) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .padding(end = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (passcode.length - 1 >= it) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(color = fillColor.value)
                    )
                }
            }
        }
    }
}


data class Digit(
    val thetaRad: Double,
    val value: Int,
)