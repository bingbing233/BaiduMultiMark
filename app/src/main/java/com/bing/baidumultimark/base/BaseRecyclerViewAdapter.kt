package com.at.festivalcamera.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * @author liangbinghao
 * @date 2021/11/3 10:52
 * @desc  recyclerview基类    holder 通过holder.getXXX()来获取控件
 */
abstract class BaseRecyclerViewAdapter<T>(var context: Context,var list: List<T>,var layoutId:Int) :
    RecyclerView.Adapter<BaseRecyclerViewAdapter.BaseViewHolder>() {

    lateinit var holder:BaseViewHolder

    class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var item = itemView

        fun getTextView( id:Int):TextView = item.findViewById(id)

        fun getImageView(id :Int):ImageView = item.findViewById(id)

        fun getEditText(id:Int):EditText = item.findViewById(id)

        fun <T:View> getView(id:Int):T = item.findViewById(id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(context).inflate(layoutId,null,false)
        holder = BaseViewHolder(view)
        return holder
    }

    override fun getItemCount(): Int = list.size


}