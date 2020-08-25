package net.shiniwa.hellomap.logging

import net.shiniwa.hellomap.MainListActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class Log {
    private constructor(isOutput: Boolean, root: String?, subdir: String?, prefix: String?) {
        sIsOutput = isOutput
        sFilePath = root + "/" + subdir
        sFileNamePrefix = prefix
        sOsw?.close()
        if (sFilePath != null) {
            val oldFileName = sFileName
            sOsw?.close()
            if (File(sFilePath).mkdirs() || File(sFilePath).exists()) {
                sFileName = sFilePath + "/" + prefix + MainListActivity.getNowTimeString() + ".log"
                val logFile = File(sFileName)
                val fis = FileOutputStream(logFile)
                sOsw = OutputStreamWriter(fis)
                deleteFileIfSizeZero(oldFileName)
            } else {
                android.util.Log.e("FileLogger", "mkdirs failed for " + sFilePath)
            }
        }
    }

    private fun deleteFileIfSizeZero(filename: String?) {
        if (filename != null) {
            val f = File(filename)
            if (f.length() <= 0) {
                f.delete()
            }
        }
    }

    companion object {
        private var sLog: Log? = null
        private var sIsOutput: Boolean = false
        private var sFilePath: String? = null
        private var sFileName: String? = null
        private var sFileNamePrefix: String? = null
        private var sStringBuffer: StringBuffer? = null
        private var sOsw: OutputStreamWriter? = null

        fun getLogPath(): String? {
            return sFileName
        }

        fun initiate(isOutput: Boolean, root: String?, subdir: String?, prefix: String?) {
            sLog = Log(isOutput, root, subdir, prefix)
        }

        fun d(t: String, m: String): Int {
            writeLog("D", t, m)
            return if (sIsOutput) android.util.Log.d(t, m) else -1
        }

        fun d(t: String, m: String, tr: Throwable): Int {
            writeLog("D", t, m)
            writeLog(android.util.Log.getStackTraceString(tr))
            return if (sIsOutput) android.util.Log.d(t, m, tr) else -1
        }

        fun e(t: String, m: String, tr: Throwable): Int {
            writeLog("E", t, m)
            writeLog(android.util.Log.getStackTraceString(tr))
            return if (sIsOutput) android.util.Log.e(t, m, tr) else -1
        }

        fun e(t: String, m: String): Int {
            writeLog("E", t, m)
            return if (sIsOutput) android.util.Log.e(t, m) else -1
        }

        fun i(t: String, m: String): Int {
            writeLog("i", t, m)
            return if (sIsOutput) android.util.Log.i(t, m) else -1
        }

        fun w(t: String, m: String): Int {
            writeLog("w", t, m)
            return if (sIsOutput) android.util.Log.w(t, m) else -1
        }

        fun v(t: String, m: String): Int {
            writeLog("V", t, m)
            return if (sIsOutput) android.util.Log.v(t, m) else -1
        }


        private fun writeLog(type: String, tag: String, m: String) {
            if (sOsw != null) {
                if (sStringBuffer == null) {
                    sStringBuffer = StringBuffer(512)
                } else {
                    // Reduce the cost of creating instance.
                    sStringBuffer?.setLength(0)
                }
                sStringBuffer?.append(MainListActivity.getNowTimeString())
                sStringBuffer?.append(" ")
                sStringBuffer?.append(type)
                sStringBuffer?.append("/")
                sStringBuffer?.append(tag)
                sStringBuffer?.append("\t")
                sStringBuffer?.append(m)
                sStringBuffer?.append("\n")
                writeLog(sStringBuffer.toString())
            }
        }

        private fun writeLog(content: String?) {
            sOsw?.write(content)
            sOsw?.flush()
        }
    }
}