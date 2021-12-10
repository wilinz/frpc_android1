package com.frpc.android.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.frpc.android.R
import com.frpc.android.databinding.ItemRecyclerMainBinding
import com.frpc.android.ui.IniEditActivity
import java.io.File

class FileListAdapter(var list: ArrayList<File>, defaultSelected: Int) :
    RecyclerView.Adapter<FileListAdapter.FileListViewHolder>() {

    companion object {
        private val TAG = "FileListAdapter"
    }

    var currentSelection = defaultSelected
        set(value) {
            field = if (value >= 0) value else 0
        }

    private fun adjustSelection() {
        val lastIndex = list.lastIndex
        if (currentSelection > lastIndex) {
            currentSelection = lastIndex
        }
    }

    class FileListViewHolder(val binding: ItemRecyclerMainBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun remove(position: Int) {
        list[position].delete()
        list.removeAt(position)

        notifyItemRemoved(position)
        if (position < currentSelection) {
            currentSelection -= 1
        }
        adjustSelection()
        notifyItemChanged(currentSelection)
        Log.d(TAG, "remove: $currentSelection")
    }

    fun add(list: ArrayList<File>) {
        list.addAll(list)
        notifyItemChanged(list.lastIndex)
        Log.d(TAG, "add: $currentSelection")
    }

    fun update(list0: ArrayList<File>) {
        list = list0
        notifyDataSetChanged()
        Log.d(TAG, "update: $currentSelection")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListViewHolder {

        val binding =
            ItemRecyclerMainBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val holder = FileListViewHolder(binding)

        binding.root.setOnClickListener {
            if (holder.adapterPosition != currentSelection) {
                val lastSelection = currentSelection
                currentSelection = holder.adapterPosition
                notifyItemChanged(lastSelection)
                notifyItemChanged(currentSelection)
            }
        }
        binding.ivDelete.setOnClickListener {
            MaterialDialog(parent.context).show {
                title(text = list[holder.adapterPosition].name)
                message(text = "确定要删除此文件")
                positiveButton(text = "确定") {
                    remove(holder.adapterPosition)
                }
                negativeButton(text = "取消") { }
            }

        }
        binding.ivEdit.setOnClickListener {
            val intent = Intent(parent.context, IniEditActivity::class.java)
            intent.putExtra(
                parent.context.getString(R.string.intent_key_file),
                list[holder.adapterPosition].path
            )
            intent.putExtra("isOverwrite", true)
            parent.context.startActivity(intent)
        }
        return holder
    }

    override fun onBindViewHolder(holder: FileListViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: $position")
        val context = holder.itemView.context
        holder.binding.tvName.text = list[position].name
        if (position == currentSelection) {
            holder.binding.card.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
        } else {
            holder.binding.card.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.white
                )
            )
        }
    }

    override fun getItemCount(): Int = list.size

    data class ConfigInfo(val name: String, val filepath: String)
}