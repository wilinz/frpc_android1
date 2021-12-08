package com.frpc.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import com.frpc.android.ui.MainActivity
import frpclib.Frpclib
import java.io.File

class FrpcService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "service create with pid " + Process.myPid())
        startForeground()
    }

    private fun startForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "my_service", "前台Service通知",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = NotificationCompat.Builder(
            this,
            "my_service"
        )
            .setContentTitle("frpc")
            .setContentText("frpc正在运行")
            .setSmallIcon(R.mipmap.ic_launcher)
//            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pi)
            .build()
        startForeground(1, notification)
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