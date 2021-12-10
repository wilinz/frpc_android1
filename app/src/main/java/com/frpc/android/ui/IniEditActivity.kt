package com.frpc.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.frpc.android.Constants
import com.frpc.android.R
import com.frpc.android.databinding.ActivityIniEditBinding
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class IniEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIniEditBinding
    private var file: File? = null

    companion object {
        private val TAG = javaClass.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIniEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(getString(R.string.intent_key_file))

        if (!TextUtils.isEmpty(filePath)) file = File(filePath)
        initToolbar()
        initEdit()
    }

    private fun initEdit() {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                file?.readText() ?: Constants.getStringFromRaw(
                    this@IniEditActivity, R.raw.frpc
                )
            }
            binding.editText.setText(text)
        }
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { v: View? -> finish() }
        binding.toolbar.title = if (file == null) getString(R.string.noName) else file!!.name
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_template -> startActivity(
                Intent(
                    this@IniEditActivity,
                    TemplateActivity::class.java
                )
            )
            R.id.action_save -> actionSave()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveFile(file: File) {
        lifecycleScope.launch {
            Log.i(TAG, "main: ${Thread.currentThread().id}")
            try {
                withContext(Dispatchers.IO) {
                    file.writeText(binding.editText.text.toString())
                    Log.i(TAG, "io: ${Thread.currentThread().id}")
                }
                Toast.makeText(this@IniEditActivity, "保存成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@IniEditActivity, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("CheckResult")
    private fun actionSave() {
        MaterialDialog(this).show {
            cancelOnTouchOutside(false)
            noAutoDismiss()
            title(if (file == null) R.string.titleInputFileName else R.string.titleModifyFileName)
            negativeButton(res = R.string.cancel) { dismiss() }
            positiveButton(text = "确定") {}
            input("", prefill = if (file == null) "" else file!!.name) { dialog, input ->
                var filename = input.toString()
                if (!filename.endsWith(Constants.INI_FILE_SUF)) {
                    filename += Constants.INI_FILE_SUF
                }

                val file = File(Constants.getIniFileParent(this@IniEditActivity), filename)

                Log.d(TAG, "actionSave: ${file.path}")
                val isOverwrite = intent.getBooleanExtra("isOverwrite", false)
                if (file.exists() && !isOverwrite) {
                    Toast.makeText(this@IniEditActivity, "文件已存在", Toast.LENGTH_SHORT).show()
                    return@input
                }
                saveFile(file)
                dismiss()
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_add_text, menu)
        return true
    }
}