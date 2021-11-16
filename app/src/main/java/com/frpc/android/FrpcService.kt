package com.frpc.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.text.TextUtils
import android.util.Log
import frpclib.Frpclib
import java.io.File

class FrpcService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "service create with pid " + Process.myPid())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val stringExtra = intent.getStringExtra(resources.getString(R.string.intent_key_file))
        if (TextUtils.isEmpty(stringExtra)) {
            Process.killProcess(Process.myPid())
            return START_STICKY
        }
        startProxy(stringExtra!!)
        return START_STICKY
    }

    private fun startProxy(path: String) {
        object : Thread() {
            override fun run() {
                Log.i(TAG, "service running with file " + File(path).name)
                try {
                    Frpclib.run(path)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "service running error $e")
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "service destroy kill pid " + Process.myPid())
        Process.killProcess(Process.myPid())
    }

    companion object {
        private val TAG = FrpcService::class.java.name
    }
}