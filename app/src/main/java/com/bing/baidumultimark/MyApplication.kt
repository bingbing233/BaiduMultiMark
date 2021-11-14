package com.bing.baidumultimark

import android.app.Application
import com.baidu.mapapi.SDKInitializer
import com.tencent.mmkv.MMKV

/**
 *  @author: liangbinghao
 *  @date:  2021/11/13 13:36
 *  @desc:
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SDKInitializer.initialize(this)
        MMKV.initialize(this)

    }
}