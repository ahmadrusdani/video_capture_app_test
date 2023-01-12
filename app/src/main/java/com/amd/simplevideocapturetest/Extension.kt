package com.amd.simplevideocapturetest

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.*
import java.nio.channels.FileChannel
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

fun Activity.showSnackBar(
    parent: View,
    message: String,
    action: (() -> Unit)? = null,
    actionText: String? = null
) {
    val snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
    action?.let {
        snackbar.setAction(actionText) {
            it()
        }
    }
    snackbar.show()
}

fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.getColorCompact(int: Int) = ContextCompat.getColor(this, int)


fun Long.getReadableFileSize(): String {
    if (this <= 0) {
        return "0"
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}

fun Any.logging(msg: String) {
    Timber.tag(this.javaClass.simpleName).d(msg)
}

fun Any.logging(tag: String, msg: String) {
    Timber.tag(tag).d(msg)
}

fun Any.logging(e: Exception) {
    Timber.tag(this.javaClass.simpleName).e(e)
}

@Throws(IOException::class)
fun copyOrMoveFile(file: File, dir: File, isCopy: Boolean) {
    val newFile = File(dir, file.name)
    var outChannel: FileChannel? = null
    var inputChannel: FileChannel? = null
    try {
        outChannel = FileOutputStream(newFile).channel
        inputChannel = FileInputStream(file).channel
        inputChannel.transferTo(0, inputChannel.size(), outChannel)
        inputChannel.close()
        if (!isCopy) file.delete()
    } finally {
        inputChannel?.close()
        outChannel?.close()
    }
}

fun commonDocumentDirPath(FolderName: String): File? {
    var dir: File? = null
    dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .toString() + "/" + FolderName
        )
    } else {
        File(Environment.getExternalStorageDirectory().toString() + "/" + FolderName)
    }
    if (!dir.exists()) {
        val success = dir.mkdirs()
        if (!success) {
            dir = null
        }
    }
    return dir
}
