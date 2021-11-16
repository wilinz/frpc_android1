package com.frpc.android.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.frpc.android.Constants
import com.frpc.android.R
import com.frpc.android.databinding.ActivityIniEditBinding
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileWriter

class IniEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIniEditBinding
    private var file: File? = null

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
        Observable.create { emitter: ObservableEmitter<String?> ->
            emitter.onNext(
                if (file != null) Constants.getStringFromFile(file) else Constants.getStringFromRaw(
                    this@IniEditActivity,
                    R.raw.frpc
                )
            )
            emitter.onComplete()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<String?> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(s: String) {
                    binding.editText.setText(s)
                }

                override fun onError(e: Throwable) {}
                override fun onComplete() {}
            })
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

    private fun actionSave() {
        MaterialDialog.Builder(this)
            .title(if (file == null) R.string.titleInputFileName else R.string.titleModifyFileName)
            .canceledOnTouchOutside(false)
            .autoDismiss(false)
            .negativeText(R.string.cancel)
            .onNegative { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }
            .input("", if (file == null) "" else file!!.name, false) { dialog, input ->
                val fileName = if (!input.toString()
                        .endsWith(Constants.INI_FILE_SUF)
                ) input.toString() + Constants.INI_FILE_SUF else input.toString()
                saveFile(fileName, object : Observer<File?> {
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(file: File) {
                        Toast.makeText(
                            this@IniEditActivity.applicationContext,
                            R.string.tipSaveSuccess,
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                        finish()
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        Toast.makeText(this@IniEditActivity, e.message, Toast.LENGTH_SHORT).show()
                    }

                    override fun onComplete() {}
                })
            }.show()
    }

    private fun saveFile(fileName: String, observer: Observer<File?>) {
        Observable.create(ObservableOnSubscribe { emitter: ObservableEmitter<File?> ->
            val file = File(Constants.getIniFileParent(this), fileName)
            val writer = FileWriter(file)
            writer.write(binding.editText.text.toString())
            writer.close()
            emitter.onNext(file)
            emitter.onComplete()
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(observer)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_add_text, menu)
        return true
    }
}