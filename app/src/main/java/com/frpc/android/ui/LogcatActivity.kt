package com.frpc.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.frpc.android.R
import com.frpc.android.databinding.ActivityLogcatBinding
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class LogcatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogcatBinding
            
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityLogcatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initToolbar()
        readLog(false)
    }

    private fun readLog(flush: Boolean) {
        val lst: HashSet<String> = LinkedHashSet()
        lst.add("logcat")
        lst.add("-d")
        lst.add("-v")
        lst.add("time")
        lst.add("-s")
        lst.add("GoLog,com.car.frpc_android.FrpcService")
        Observable.create { emitter: ObservableEmitter<String?> ->
            if (flush) {
                val lst2: HashSet<String> = LinkedHashSet()
                lst2.add("logcat")
                lst2.add("-c")
                val process = Runtime.getRuntime().exec(lst2.toTypedArray())
                process.waitFor()
            }
            val process = Runtime.getRuntime().exec(lst.toTypedArray())
            val `in` = InputStreamReader(process.inputStream)
            val bufferedReader = BufferedReader(`in`)
            var line: String? = null
            while (bufferedReader.readLine().also { line = it } != null) {
                emitter.onNext(line!!)
            }
            `in`.close()
            bufferedReader.close()
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(s: String) {
                    binding.tvLogcat.append(s)
                    binding.tvLogcat.append("\r\n")
                    binding.svLogcat.fullScroll(View.FOCUS_DOWN)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {}
            })
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { v: View? -> finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_logcat, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.copy -> {
                setClipboard(binding.tvLogcat.text.toString())
                Toast.makeText(this, R.string.copySuccess, Toast.LENGTH_SHORT).show()
            }
            R.id.delete -> {
                readLog(true)
                binding.tvLogcat.text = ""
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setClipboard(content: String?) {
        try {
            val cmb = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("logcat", content)
            cmb.setPrimaryClip(clipData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}