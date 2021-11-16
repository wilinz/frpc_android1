package com.frpc.android.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.frpc.android.Constants
import com.frpc.android.FrpcService
import com.frpc.android.R
import com.frpc.android.adapter.FileListAdapter
import com.frpc.android.databinding.FragmentHomeBinding
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

class HomeFragment : Fragment() {

    private var listAdapter: FileListAdapter? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        init()
        return binding.root
    }

    private fun init() {
        binding.fab.setOnClickListener { onViewClicked() }
        listAdapter = FileListAdapter()
        listAdapter!!.addChildClickViewIds(R.id.iv_delete, R.id.iv_edit, R.id.info_container)
        listAdapter!!.setOnItemChildClickListener { adapter: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            when (view.id) {
                R.id.iv_edit -> {
                    editIni(position)
                }
                R.id.iv_delete -> {
                    deleteFile(position)
                }
                R.id.info_container -> {
                    if (isRunService(context)) {
                        Toast.makeText(context, R.string.needStopService, Toast.LENGTH_SHORT).show()
                        return@setOnItemChildClickListener
                    }
                    listAdapter!!.selectItem = listAdapter!!.getItem(position)
                }
            }
        }
        binding.recyclerView.adapter = listAdapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.refreshView.setOnRefreshListener { setData() }
        syncServiceState()
    }

    private fun syncServiceState() {
        if (!isRunService(context)) {
            setServiceState(R.color.colorPlay, R.drawable.ic_play_white, R.string.notOpened)
        } else {
            setServiceState(R.color.colorStop, R.drawable.ic_stop_white, R.string.hasOpened)
        }
    }

    private fun setServiceState(color: Int, res: Int, text: Int) {
        binding.fab.colorNormal = resources.getColor(color)
        binding.fab.setImageResource(res)
        binding.tvState.setText(text)
    }

    override fun onResume() {
        super.onResume()
        setData()
    }

    private fun editIni(position: Int) {
        val item = listAdapter!!.getItem(position)
        checkPermissions { aBoolean: Boolean? ->
            if (!aBoolean!!) {
                Constants.tendToSettings(context)
                return@checkPermissions
            }
            val intent = Intent(context, IniEditActivity::class.java)
            intent.putExtra(getString(R.string.intent_key_file), item.path)
            startActivity(intent)
        }
    }

    private fun deleteFile(position: Int) {
        val item = listAdapter!!.getItem(position)
        Observable.just(item)
            .map { file: File? -> item.delete() }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Boolean> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(aBoolean: Boolean) {
                    if (aBoolean) {
                        listAdapter!!.removeAt(position)
                    } else {
                        Toast.makeText(
                            context,
                            item.name + getString(R.string.actionDeleteFailed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }

                override fun onComplete() {}
            })
    }

    private fun setData() {
        files.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<List<File>> {
                override fun onSubscribe(d: Disposable) {
                    binding.refreshView.isRefreshing = true
                }

                override fun onNext(files: List<File>) {
                    binding.refreshView.isRefreshing = false
                    listAdapter!!.setList(files)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                }

                override fun onComplete() {
                    binding.refreshView.isRefreshing = false
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val files: Observable<List<File>>
        get() = Observable.create(
            ObservableOnSubscribe { emitter: ObservableEmitter<List<File>> ->
                val path = Constants.getIniFileParent(
                    context
                )
                val files = path.listFiles()
                emitter.onNext(if (files != null) Arrays.asList(*files) else ArrayList())
                emitter.onComplete()
            } as ObservableOnSubscribe<List<File>>).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    private fun checkPermissions(consumer: Consumer<Boolean>) {
        val subscribe = RxPermissions(this)
            .request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
            )
            .subscribe(consumer)
    }

    fun onViewClicked() {
        if (isRunService(context)) {
            context?.stopService(Intent(context, FrpcService::class.java))
            setServiceState(R.color.colorPlay, R.drawable.ic_play_white, R.string.notOpened)
        } else {
            if (listAdapter?.selectItem == null) {
                Toast.makeText(context, R.string.notSelectIni, Toast.LENGTH_SHORT).show()
                return
            }
            readLog()
            val service = Intent(context, FrpcService::class.java)
            service.putExtra(
                resources.getString(R.string.intent_key_file),
                listAdapter!!.selectItem!!.path
            )
            context?.startService(service)
            setServiceState(R.color.colorStop, R.drawable.ic_stop_white, R.string.hasOpened)
        }
    }

    fun isRunService(context: Context?): Boolean {
        val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            val simpleName = FrpcService::class.java.name
            if (simpleName == service.process) {
                return true
            }
        }
        return false
    }

    private var readingLog: Disposable? = null
    private fun readLog() {
        binding.tvLogcat.text = ""
        if (readingLog != null) return
        val lst: HashSet<String> = LinkedHashSet()
        lst.add("logcat")
        lst.add("-T")
        lst.add("0")
        lst.add("-v")
        lst.add("time")
        lst.add("-s")
        lst.add("GoLog,com.car.frpc_android.FrpcService")
        readingLog =
            Observable.create { emitter: ObservableEmitter<String?> ->
                val process = Runtime.getRuntime().exec(lst.toTypedArray())
                val `in` = InputStreamReader(process.inputStream)
                val bufferedReader = BufferedReader(`in`)
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    emitter.onNext(line!!)
                }
                `in`.close()
                bufferedReader.close()
                emitter.onComplete()
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s: String? ->
                    binding.tvLogcat.append(s)
                    binding.tvLogcat.append("\r\n")
                    binding.svLogcat.fullScroll(View.FOCUS_DOWN)
                }) { throwable: Throwable ->
                    throwable.printStackTrace()
                    binding.tvLogcat.append(throwable.toString())
                    binding.tvLogcat.append("\r\n")
                }
    }
}