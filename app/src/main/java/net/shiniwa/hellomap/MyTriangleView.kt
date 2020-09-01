package net.shiniwa.hellomap

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyTriangleView(context: Context, attrib: AttributeSet) : GLSurfaceView(context, attrib) {
    private var mRenderer: TriangleRenderer? =
        null
    private var mHandler: Handler? = null
    private var mAngle = 0f
    private var mIsDestroyed = false

    companion object {
        const val COORDS_PER_VERTEX = 3
        private const val vertexStride = 12
        private const val vertexCount = 3
    }

    init {
        mHandler = Handler()
        mAngle = 0.0f
        mIsDestroyed = false
        setEGLContextClientVersion(2)
        mRenderer = TriangleRenderer()
        setRenderer(mRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        mHandler?.postDelayed(object: Runnable {
            override fun run() {
                if (!mIsDestroyed) {
                    mAngle += 2.0f
                    mRenderer?.angle = mAngle
                    requestRender()
                    mHandler!!.postDelayed(this, 20)
                }
            }
        }, 20)
    }

    inner class TriangleRenderer : Renderer {
        private var mTriangle: Triangle? = null
        private val mMVPMatrix = FloatArray(16)
        private val mProjectionMatrix = FloatArray(16)
        private val mViewMatrix = FloatArray(16)
        private val mRotationMatrix = FloatArray(16)
        val PRESENTATION_BG_COLOR_A = 1.0f
        val PRESENTATION_BG_COLOR_B = 0x3c.toFloat() / 0xff.toFloat()
        val PRESENTATION_BG_COLOR_G = 0x3c.toFloat() / 0xff.toFloat()
        val PRESENTATION_BG_COLOR_R = 0x96.toFloat() / 0xff.toFloat()

        override fun onDrawFrame(unused: GL10) {
            val scratch = FloatArray(16)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            val time = SystemClock.uptimeMillis() % 4000L
            angle = 0.090f * time.toInt()
            Matrix.setRotateM(mRotationMatrix, 0, angle, 0f, 0f, -1.0f)
            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
            Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0)
            mTriangle!!.draw(scratch)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val ratio = width.toFloat() / height
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        }

        override fun onSurfaceCreated(unusedgl: GL10, config: EGLConfig) {
            GLES20.glClearColor(
                PRESENTATION_BG_COLOR_R,
                PRESENTATION_BG_COLOR_G,
                PRESENTATION_BG_COLOR_B,
                PRESENTATION_BG_COLOR_A
            )
            mTriangle = Triangle()
        }

        fun loadShader(type: Int, shaderCode: String?): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }

        @Volatile
        var angle = 0f

        private inner class Triangle {
            private val vertexShaderCode = "attribute vec4 vPosition;" +
                    "uniform mat4 uMVPMatrix;" +
                    "void main() {" +
                    "  gl_Position = vPosition * uMVPMatrix;" +
                    "}"
            private val fragmentShaderCode = "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}"
            private val vertexBuffer: FloatBuffer
            var triangleCoords = floatArrayOf(
                0.0f, 0.622008459f, 0.0f,
                -0.5f, -0.311004243f, 0.0f,
                0.5f, -0.311004243f, 0.0f
            )
            var color = floatArrayOf(
                0.1f,
                0x44.toFloat() / 0xff.toFloat(),
                0x72.toFloat() / 0xff.toFloat(),
                0xc4.toFloat() / 0xff.toFloat()
            )
            private var mProgram = 0
            private var mPositionHandle = 0
            private var mColorHandle = 0
            private var mMVPMatrixHandle = 0
            fun init() {
                val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
                val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
                mProgram = GLES20.glCreateProgram()
                GLES20.glAttachShader(mProgram, vertexShader)
                GLES20.glAttachShader(mProgram, fragmentShader)
                GLES20.glLinkProgram(mProgram)
            }

            fun draw(mvpMatrix: FloatArray?) {
                GLES20.glUseProgram(mProgram)
                mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
                GLES20.glEnableVertexAttribArray(mPositionHandle)
                GLES20.glVertexAttribPointer(
                    mPositionHandle, Companion.COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    Companion.vertexStride, vertexBuffer
                )
                mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
                mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
                GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
                GLES20.glUniform4fv(mColorHandle, 1, color, 0)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, Companion.vertexCount)
                GLES20.glDisableVertexAttribArray(mPositionHandle)
            }

            init {
                val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
                bb.order(ByteOrder.nativeOrder())
                vertexBuffer = bb.asFloatBuffer()
                vertexBuffer.put(triangleCoords)
                vertexBuffer.position(0)
                init()
            }
        }
    }

}