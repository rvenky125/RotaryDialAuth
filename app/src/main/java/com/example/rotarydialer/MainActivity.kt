package com.example.rotarydialer

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.rotarydialer.ui.theme.RotaryDialerTheme
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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(text = passcode, style = MaterialTheme.typography.button)

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

    var startOffset = remember<Offset?> { null }

    val textRect = remember {
        android.graphics.Rect()
    }
    val digitButtonRadius = remember { 85f }
    val arcWidth = remember { (digitButtonRadius * 2) * 1.4f }
    var windowBounds = remember {
        Rect.Zero
    }

    val coroutine = rememberCoroutineScope()

    Canvas(
        modifier = Modifier
            .width(400.dp)
            .height(400.dp)
            .aspectRatio(1f)
            .onGloballyPositioned {
                windowBounds = it.boundsInWindow()
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

                            if (theta in 37.5f..60f) {
                                digitDialDone = true
                                val digit =
                                    floor(changeInTheta / angleBetweenDigits).roundToInt() - 1
                                onNewDigit(if (digit == 10) 0 else digit)
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
            drawArc(
                color = Color.White,
                size = Size(width = size.width * 0.70f, height = size.height * 0.70f),
                topLeft = Offset(160f, 160f),
                startAngle = -90f - startAngleOfWholeDigits,
                sweepAngle = -angleBetweenDigits * 9,
                useCenter = false,
                style = Stroke(arcWidth, cap = StrokeCap.Round),
                blendMode = BlendMode.Src
            )

            drawContext.canvas.translate(center.x, center.y)
            repeat(10) { index ->
                val theta =
                    (((index * angleBetweenDigits) * Math.PI / 180) - startAngleOfWholeDigits * (Math.PI / 180f)).toFloat()
                val x = digitsCircleRadius * cos(theta)
                val y = digitsCircleRadius * sin(theta)

                if (index == 0) {
                    startOffset = Offset(x, y)
                }

                drawCircle(
                    color = Color.Black,
                    radius = digitButtonRadius,
                    center = Offset(
                        x,
                        y,
                    ),
                    blendMode = BlendMode.SrcOut
                )
            }
            drawContext.canvas.translate(-center.x, -center.y)
        }

        drawContext.canvas.translate(center.x, center.y)
        (1..9).plus(0).reversed().run {
            forEachIndexed { index, num ->
                val theta =
                    (((index * angleBetweenDigits) * Math.PI / 180) - startAngleOfWholeDigits * (Math.PI / 180f)).toFloat()
                textPaint.getTextBounds(num.toString(), 0, 1, textRect)

                val x = digitsCircleRadius * cos(theta) - textRect.width() / 2
                val y = digitsCircleRadius * sin(theta) + textRect.height() / 2

                drawContext.canvas
                    .nativeCanvas
                    .drawText(
                        num.toString(),
                        x,
                        y,
                        textPaint
                    )
            }

            val theta =
                ((((size + 1) * angleBetweenDigits) * Math.PI / 180) - startAngleOfWholeDigits * (Math.PI / 180f)).toFloat()
            val x = digitsCircleRadius * cos(theta) + 10f
            val y = digitsCircleRadius * sin(theta) - 10f

            drawCircle(color = Color.White, radius = 25f, center = Offset(x, y))
        }
        drawContext.canvas.translate(-center.x, -center.y)

        drawCircle(color = Color.Black, radius = size.width / 2, blendMode = BlendMode.DstOver)
        drawCircle(color = Color.White, radius = size.width / 4.6f)
    }
}