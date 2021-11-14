package com.bing.baidumultimark

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.baidu.mapapi.map.MarkerOptions

/**
 *  @author: liangbinghao
 *  @date:  2021/11/14 22:34
 *  @desc:
 */
class MarkerViewModel : ViewModel() {

    companion object {
        //sp key
        const val SP_APP = "sp_app"
        const val SP_POINT_LIST = "sp_point_list"
        const val SP_IS_FIRST = "sp_is_first"

        //bundle key
        const val SP_ID = "sp_id"
        const val SP_STATE = "sp_state"
    }

    val options = MutableLiveData<HashMap<Long,MarkerOptions>>()
}