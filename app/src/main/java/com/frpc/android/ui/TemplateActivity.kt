package com.frpc.android.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.frpc.android.Constants
import com.frpc.android.R
import com.frpc.android.databinding.ActivityIniEditBinding
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class TemplateActivity : AppCompatActivity() {

    private lateinit var binding:ActivityIniEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding= ActivityIniEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initEdit()
    }

    private fun initEdit() {
        Observable.create<String> { emitter ->
            emitter.onNext(Constants.getStringFromRaw(this@TemplateActivity, R.raw.frpc_full))
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
        binding.toolbar.setTitle(R.string.titleTemplate)
    }
}