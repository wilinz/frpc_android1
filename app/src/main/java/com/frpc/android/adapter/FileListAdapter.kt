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

class FileListAdapter(var list: ArrayList<File>) :
    RecyclerView.Adapter<FileListAdapter.FileListViewHolder>() {

    companion object {
        private val TAG = javaClass.simpleName
    }

    var currentSelection = -1

    class FileListViewHolder(val binding: ItemRecyclerMainBinding) :
        RecyclerView.ViewHolder(binding.root)

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
                title(text = "删除${list[holder.adapterPosition].name}")
                message(text = "确定要删除此文件")
                positiveButton(text = "确定") {
                    val position = holder.adapterPosition
                    list[position].delete()
                    list.removeAt(position)
                    notifyItemRemoved(position)
                    Log.d(TAG, "onCreateViewHolder: size:$itemCount")
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