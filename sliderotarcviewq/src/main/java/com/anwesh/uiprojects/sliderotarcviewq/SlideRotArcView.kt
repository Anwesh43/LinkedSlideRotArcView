package com.anwesh.uiprojects.sliderotarcviewq

/**
 * Created by anweshmishra on 01/06/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import android.app.Activity
import android.util.Log

val nodes : Int = 5
val lines : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#311B92")
val backColor : Int = Color.parseColor("#BDBDBD")
val rotDeg : Float = 45f
val rFactor : Float = 3.6f

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) / a + k / b
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawLineArc(i : Int, sc1 : Float, sc2 : Float, size : Float, paint : Paint) {
    val r : Float = size / rFactor
    val y : Float = -size * sc1
    save()
    rotate(rotDeg * sc2.divideScale(i, lines) * (1f - 2 * i))
    drawLine(0f, 0f, 0f, y, paint)
    save()
    translate(0f, y)
    drawArc(RectF(-r, -r, r, r), -90f + 180f * i, 180f, true, paint)
    restore()
    restore()
}

fun Canvas.drawSRANode(i : Int, scale : Float, paint : Paint) {
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(lines - 1)) {
        drawLineArc(j, sc1, sc2, size, paint)
    }
    restore()
}

class SlideRotArcView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap {
                    Log.d("stopped updating at", "${System.currentTimeMillis() / 1000}")
                }
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb  : (Float) -> Unit) {
            scale += scale.updateValue(dir, 1, lines)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class SRANode(var i : Int, val state : State = State()) {

        private var next : SRANode? = null
        private var prev : SRANode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = SRANode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSRANode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SRANode {
            var curr : SRANode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class SlideRotArc(var i : Int) {

        private val root : SRANode = SRANode(0)
        private var curr : SRANode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : SlideRotArcView) {

        private val animator : Animator = Animator(view)
        private val sra : SlideRotArc = SlideRotArc(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            sra.draw(canvas, paint)
            animator.animate {
                sra.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap(cb : () -> Unit) {
            sra.startUpdating {
                animator.start()
                cb()
            }
        }
    }

    companion object {

        fun create(activity: Activity) : SlideRotArcView {
            val view : SlideRotArcView = SlideRotArcView(activity)
            activity.setContentView(view)
            return view
        }
    }
}