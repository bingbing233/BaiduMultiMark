package com.at.festivalcamera.base

import android.app.Activity
import android.app.ProgressDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.bing.baidumultimark.R

/**
 * @author liangbinghao
 * @date 2021/11/3 13:53
 * @desc baseActivity
 */
//构造方法传入一个参数为LayoutInflater、返回值为VB的方法
abstract class BaseActivity<VB:ViewBinding> (private val inflater: (LayoutInflater)-> VB): AppCompatActivity() {

    val progressDialog by lazy {
        ProgressDialog(this)
    }
    val TAG = "BaseActivity"
    lateinit var binding:VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflater.invoke(layoutInflater)
        setContentView(binding.root)
        //setStatusBarFontColor(this,true)
        //setStatusBarColor(this, R.color.white)
    }

    /**
     * 修改状态栏字体颜色（原生）
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setStatusBarFontColor(activity: Activity, isDark:Boolean?=true){
        var decorView = activity.window.decorView
        if(isDark!!)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setStatusBarColor(activity: Activity, colorId:Int){
        var window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = activity.resources.getColor(colorId,null)
    }

    fun showLoading(){
        progressDialog.show()
    }

    fun hideLoading(){
        progressDialog.hide()
    }
}