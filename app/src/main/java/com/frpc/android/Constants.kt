package com.frpc.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.*

object Constants {
    const val INI_PATH = "ini"
    const val INI_FILE_SUF = ".ini"
    fun getIniFileParentPath(context: Context?): String {
        return context!!.cacheDir.path + File.separator + INI_PATH
    }

    fun getIniFileParent(context: Context?): File {
        val iniFileParentPath = getIniFileParentPath(context)
        val parent = File(iniFileParentPath)
        if (!parent.exists()) parent.mkdirs()
        return parent
    }

    fun tendToSettings(context: Context?) {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
        intent.data = Uri.fromParts("package", context!!.packageName, null)
        context.startActivity(intent)
        Toast.makeText(context, R.string.permissionReason, Toast.LENGTH_SHORT).show()
    }

    fun getStringFromFile(file: File?): String {
        val sb = StringBuilder()
        try {
            val inputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(inputStream, "gbk")
            val reader = BufferedReader(inputStreamReader)
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append("\n")
            }
            inputStream.close()
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    fun getStringFromRaw(context: Context, rawName: Int): String {
        try {
            val inputReader = InputStreamReader(context.resources.openRawResource(rawName))
            val bufReader = BufferedReader(inputReader)
            var line: String? = ""
            val result = StringBuilder()
            while (bufReader.readLine().also { line = it } != null) result.append(line).append("\n")
            return result.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun getFileFromAsserts(context: Context, fileName: String): String {
        try {
            val classLoader = context.javaClass.classLoader
            val isr = InputStreamReader(classLoader!!.getResourceAsStream("assets/$fileName"))
            val bfr = BufferedReader(isr)
            var line: String?
            val stringBuilder = StringBuilder()
            while (bfr.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            return stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}