package io.github.cloudburst.grayscaler

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class ShizukuRunner {
    fun interface CommandResultListener {
        /*
        * Runs after the command executes, at least partially. Does not run with 'done' if the command throws an error.
        * output: The output of the command
        * done: If the command has finished executing
        * error: If the command has thrown an error
        */
        fun onCommandResult(output: String, done: Boolean, error: Boolean)
    }

    companion object {
        fun command(command: String, lineBundle: Int = 50, listener: CommandResultListener) {
            Thread {
                try {
                    val process = IShizukuService.Stub.asInterface(Shizuku.getBinder())
                        .newProcess(arrayOf("sh","-c",command), null, null)
                    val reader = BufferedReader(InputStreamReader(FileInputStream(process.inputStream.fileDescriptor)))
                    val err = BufferedReader(InputStreamReader(FileInputStream(process.errorStream.fileDescriptor)))
                    val output = StringBuilder()
                    val errordata = StringBuilder()
                    var line: String?
                    var linecount = 0
                    while (reader.readLine().also { line = it } != null) {
                        linecount++
                        output.append(line).append("\n")
                        if (linecount == lineBundle) {
                            linecount = 0
                            listener.onCommandResult(output.toString(), false, false)
                        }
                    }
                    while (err.readLine().also { line = it } != null) {
                        errordata.append(line).append("\n")
                    }
                    if(errordata.isNotBlank()) listener.onCommandResult(errordata.toString(), true, true)
                    else listener.onCommandResult(output.toString(), true, false)
                    process.waitFor()
                } catch (e: Exception) {
                    listener.onCommandResult(e.message ?: "No Shizuku", true, true)
                }

            }.start()
        }

        fun shizukuEnabled(ctx: Context): Boolean {
            var shizukuIsRun = true
            var shizukuIsAccept = false
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(
                    0
                ) else shizukuIsAccept = true
            } catch (e: Exception) {
                if (ContextCompat.checkSelfPermission(
                        ctx, "moe.shizuku.manager.permission.API_V23"
                    ) == PackageManager.PERMISSION_GRANTED
                ) shizukuIsAccept = true
                if (e.javaClass == IllegalStateException::class.java) {
                    shizukuIsRun = false
                }
            }
            return shizukuIsRun && shizukuIsAccept
        }
    }
}