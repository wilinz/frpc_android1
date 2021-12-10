package com.frpc.android.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.frpc.android.Constants
import com.frpc.android.FrpcService
import com.frpc.android.R
import com.frpc.android.adapter.FileListAdapter
import com.frpc.android.databinding.FragmentHomeBinding
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import android.content.Context.MODE_PRIVATE
import android.os.Build


class HomeFragment : Fragment() {

    private var listAdapter: FileListAdapter? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "HomeFragment"
        private const val ADD_FILE=0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        init()

        Log.d(TAG, "onCreateView: ${context?.getExternalFilesDir("")}")
        return binding.root
    }

    private fun init() {
        val defaultSelected = requireContext().getSharedPreferences("defaultSelected", MODE_PRIVATE)
            .getInt("position", 0)

        binding.fab.setOnClickListener { onViewClicked() }
        listAdapter = FileListAdapter(getFiles(), defaultSelected)
        binding.recyclerView.adapter = listAdapter
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.refreshView.setOnRefreshListener {
            binding.refreshView.isRefreshing = true
            updateData()
            binding.refreshView.isRefreshing = false
        }
        syncServiceState()
    }

    private fun getFiles(): ArrayList<File> {
        val path = Constants.getIniFileParent(requireContext())
        val files = path.listFiles() ?: arrayOf()
        return ArrayList(files.toList())
    }

    private fun syncServiceState() {
        if (!serviceIsRuning(context)) {
            setServiceState(R.color.colorPlay, R.drawable.ic_play_white, R.string.notOpened)
        } else {
            setServiceState(R.color.colorStop, R.drawable.ic_stop_white, R.string.hasOpened)
        }
    }

    private fun setServiceState(color: Int, res: Int, text: Int) {
        context?.let {
            binding.fab.colorNormal = ContextCompat.getColor(it, color)
            binding.fab.setImageResource(res)
            binding.tvState.setText(text)
        }
    }

    override fun onResume() {
        super.onResume()
        updateData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode==Activity.RESULT_OK){
            when(requestCode){
                ADD_FILE->{

                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateData() {
        listAdapter?.update(getFiles())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter?.let {
            val sp = requireContext().getSharedPreferences("defaultSelected", MODE_PRIVATE)
                .edit()
            sp.putInt("position", it.currentSelection)
            sp.apply()
        }

        _binding = null
        listAdapter = null
    }

    private fun onViewClicked() {
        Log.d(TAG, "onViewClicked: ${serviceIsRuning(context)}")
        if (serviceIsRuning(context)) {
            context?.stopService(Intent(context, FrpcService::class.java))
            setServiceState(R.color.colorPlay, R.drawable.ic_play_white, R.string.notOpened)
        } else {
            val position = listAdapter!!.currentSelection
            if (position == -1) {
                Toast.makeText(context, R.string.notSelectIni, Toast.LENGTH_SHORT).show()
                return
            }
            readLog()
            val service = Intent(context, FrpcService::class.java)
            service.putExtra(
                resources.getString(R.string.intent_key_file),
                listAdapter!!.list[position].path
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context?.startForegroundService(service)
            } else {
                context?.startService(service)
            }
            setServiceState(R.color.colorStop, R.drawable.ic_stop_white, R.string.hasOpened)
        }
    }

    fun serviceIsRuning(context: Context?): Boolean {
        val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            val simpleName = FrpcService::class.java.name
            if (simpleName == service.service.className) {
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