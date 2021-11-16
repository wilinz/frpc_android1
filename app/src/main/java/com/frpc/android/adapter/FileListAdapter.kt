package com.frpc.android.adapter

import android.widget.RadioButton
import com.frpc.android.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.io.File

class FileListAdapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.item_recycler_main) {
    var selectItem: File? = null

    fun setSelectItem(selectItem: File?): FileListAdapter {
        this.selectItem = selectItem
        notifyDataSetChanged()
        return this
    }

    override fun removeAt(position: Int) {
        val item = getItem(position)
        if (selectItem != null && item.path == selectItem!!.path) {
            selectItem = null
        }
        super.removeAt(position)
    }

    override fun convert(baseViewHolder: BaseViewHolder, file: File) {
        baseViewHolder.setText(R.id.tv_name, file.name)
        val btnRadio: RadioButton = baseViewHolder.itemView.findViewById(R.id.btn_radio)
        btnRadio.isChecked = selectItem != null && selectItem!!.path == file.path
    }
}