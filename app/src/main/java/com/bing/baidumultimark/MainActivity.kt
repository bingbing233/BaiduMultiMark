package com.bing.baidumultimark

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.at.festivalcamera.base.BaseActivity
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.sug.SuggestionSearch
import com.baidu.mapapi.search.sug.SuggestionSearchOption
import com.bing.baidumultimark.MarkerViewModel.Companion.SP_COMMENT
import com.bing.baidumultimark.MarkerViewModel.Companion.SP_ID
import com.bing.baidumultimark.MarkerViewModel.Companion.SP_IS_FIRST
import com.bing.baidumultimark.MarkerViewModel.Companion.SP_POINT_LIST
import com.bing.baidumultimark.MarkerViewModel.Companion.SP_STATE
import com.bing.baidumultimark.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.permissionx.guolindev.PermissionX
import com.tencent.mmkv.MMKV

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private lateinit var mapView: MapView
    private lateinit var baiduMap: BaiduMap
    private val locationClient by lazy { LocationClient(this) }
    private val option = LocationClientOption()
    lateinit var dialog: Dialog
    lateinit var dialogView: View
    val mm = MMKV.mmkvWithID("points")

    //popupwindow
    private val listPopupWindow by lazy {
        ListPopupWindow(this)
    }

    //viewmodel
    lateinit var viewModel: MarkerViewModel
    private val address = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this@MainActivity).get(MarkerViewModel::class.java)
        initPermission()
        initView()
        initMyLocation()
    }

    private fun initView() {
        binding.apply {
            dialog = Dialog(this@MainActivity)
            dialogView =
                LayoutInflater.from(this@MainActivity).inflate(R.layout.comment_dialog, null)
            dialog.setContentView(dialogView)
            this@MainActivity.mapView = mapView
            baiduMap = mapView.map

            fabLocation.setOnClickListener {
                locationClient.start()
            }

            editQuery.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    search(editQuery.text.toString())
                    //Toast.makeText(this@MainActivity, "${editQuery.text.toString()}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            listPopupWindow.anchorView = binding.editQuery

            baiduMap.setOnMarkerClickListener {
                createInfoWindow(marker = it)
                true
            }

            //点击地图隐藏全部infowindow
            baiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
                override fun onMapClick(p0: LatLng?) {
                    baiduMap.hideInfoWindow()
                }

                override fun onMapPoiClick(p0: MapPoi?) {

                }
            })
        }
        //首次进入弹出提示
        var isFirst = mm.getBoolean(SP_IS_FIRST, true)
        if (isFirst)
            showGuide()
        mm.putBoolean(SP_IS_FIRST, false)
        //恢复上次数据
        restorePoints()
    }

    /**获取当前我的地址*/
    private fun initMyLocation() {
        baiduMap.isMyLocationEnabled = true
        option.apply {
            isOpenGps = true
            setCoorType("bd09ll")
            setScanSpan(1000)
        }
        locationClient.locOption = option
        locationClient.registerLocationListener(object : BDAbstractLocationListener() {
            override fun onReceiveLocation(location: BDLocation?) {
                //mapView 销毁后不在处理新接收的位置
                if (location == null || mapView == null) {
                    return
                }
                val locData = MyLocationData.Builder()
                    .accuracy(location.radius) // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.direction).latitude(location.getLatitude())
                    .longitude(location.longitude).build()
                baiduMap.setMyLocationData(locData)
                val latlng = LatLng(location.latitude, locData.longitude)
                val mapStatus = MapStatus.Builder()
                    //.zoom(18f)
                    .target(latlng).build()
                val update = MapStatusUpdateFactory.newMapStatus(mapStatus)
                if (location.latitude != 4.9E-324) {
                    baiduMap.animateMapStatus(update)
                    locationClient.stop()
                }
                Log.d(
                    TAG,
                    "onReceiveLocation: latitude = ${location.latitude} longitude = ${location.longitude}"
                )
            }
        })
    }

    /*获取权限**/
    private fun initPermission() {
        PermissionX.init(this).permissions(
            Manifest.permission.LOCATION_HARDWARE,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).request(null)
    }

    /**
     * 搜索地址
     */
    private fun search(key: String) {
        val search = SuggestionSearch.newInstance()
        search.setOnGetSuggestionResultListener { it ->
            address.clear()
            it.allSuggestions.onEach {
                var info = it.address
                if (!it.poiChildrenInfoList.isNullOrEmpty()) {
                    info += it.poiChildrenInfoList[0].name
                }
                address.add(info)
                Log.d(TAG, "search: ${it.poiChildrenInfoList}")
            }
            listPopupWindow.setAdapter(
                ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    address
                )
            )
            //点击item后添加mark
            listPopupWindow.setOnItemClickListener { parent, view, position, id ->
                val latlng = it.allSuggestions.get(position).pt
                val mapStatus = MapStatus.Builder()
                    .zoom(13f)
                    .target(latlng).build()
                //定位到将要标记的地方
                val update = MapStatusUpdateFactory.newMapStatus(mapStatus)
                baiduMap.animateMapStatus(update)

                val bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.icon_marker_75x100)
                val optionId = System.currentTimeMillis()
                val bundle = Bundle()
                bundle.putBoolean(SP_STATE, true)
                bundle.putLong(SP_ID, optionId)
                val overLayOption = MarkerOptions()
                    .position(latlng)
                    .icon(bitmap)
                    .extraInfo(bundle)
                viewModel.options.put(optionId, overLayOption)
                updateCount()
                //添加一个标记
                baiduMap.addOverlay(overLayOption)
                listPopupWindow.dismiss()
            }
            listPopupWindow.show()
        }

        var suggestionSearchOption = SuggestionSearchOption()
        suggestionSearchOption.mCity = "江门市"
        suggestionSearchOption.keyword(key)
        search.requestSuggestion(suggestionSearchOption)
    }

    /**
     * 清除全部标记
     */
    private fun clearMarks() {
        baiduMap.clear()
    }

    /**
     * 保存已绘制的标记
     */
    private fun savePoints() {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("是否确认覆盖上次记录并保存？")
            .setPositiveButton("确认") { _, _ ->
                mm.putString(SP_POINT_LIST, Gson().toJson(viewModel.options))
                Log.d(TAG, "savePoints: ${viewModel.options}")
                Toast.makeText(this@MainActivity, "保存成功", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("再想想") { _, _ -> }
            .show()
    }

    /**
     * 恢复保存过的标记
     */
    fun restorePoints() {
        var jsonStr = mm.getString(SP_POINT_LIST, null)

        if (jsonStr != null) {
            var lastOption: MarkerOptions? = null
            val typeToken = object : TypeToken<HashMap<Long, MarkerOptions>>() {}.type
            viewModel.options = Gson().fromJson(jsonStr, typeToken)
            Log.d(TAG, "restorePoints: ${viewModel.options.size}")
            viewModel.options.forEach {
                lastOption = it.value
                val state = it.value.extraInfo.getBoolean(SP_STATE, true)
                if (state)
                    it.value.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_marker_75x100))
                else
                    it.value.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon_marker_done_120x120))

                baiduMap.addOverlay(it.value)
            }
            val mapStatus = MapStatus.Builder()
                .zoom(13f)
                .target(lastOption!!.position).build()
            val update = MapStatusUpdateFactory.newMapStatus(mapStatus)
            baiduMap.animateMapStatus(update)
            updateCount()
        }
    }

    /**
     * 标改变记的状态
     */
    fun changeMarkState(marker: Marker) {
        val id = marker.extraInfo.getLong(SP_ID)
        val state = marker.extraInfo.getBoolean(SP_STATE, true)
        val bundle = Bundle()
        bundle.putBoolean(SP_STATE, !state)
        marker.extraInfo = bundle
        if (state) {
            marker.icon = BitmapDescriptorFactory.fromResource(R.mipmap.icon_marker_75x100)
        } else {
            marker.icon = BitmapDescriptorFactory.fromResource(R.mipmap.icon_marker_done_120x120)
        }
        viewModel.options.get(id)?.extraInfo(bundle)
    }

    /**
     * 删除标记
     */
    fun deleteMarker(marker: Marker, infoWindow: InfoWindow) {
        val id = marker.extraInfo.getLong(SP_ID)
        viewModel.options.remove(id)
        marker.remove()
        baiduMap.hideInfoWindow(infoWindow)
        updateCount()
    }

    /**
     * 添加备注
     */
    fun addComment(marker: Marker, msg: String) {

    }

    /**
     * 更新标记数
     */
    fun updateCount() {
        supportActionBar?.subtitle = "当前已标记${viewModel.options.size}个"
    }

    /**
     * 展示引导
     */
    private fun showGuide() {
        AlertDialog.Builder(this)
            .setTitle("批量标记工具使用说明")
            .setMessage(
                "1、点击下方按钮定位到当前位置\n" +
                        "2、标记结束后记得在菜单保存，否则数据会丢失\n" +
                        "3、保存会覆盖上次数据"
            )
            .setPositiveButton("知道了", DialogInterface.OnClickListener { dialog, which -> })
            .show()
    }

    private fun createInfoWindow(marker: Marker) {
        val view = LayoutInflater.from(this).inflate(R.layout.popupwindow, null)
        val position = marker.position
        val p = baiduMap.projection.toScreenLocation(position)
        p.y -= 100
        val llInfo = baiduMap.projection.fromScreenLocation(p)
        val infoWindow = InfoWindow(view, llInfo, 0)
        val tvComment = view.findViewById<TextView>(R.id.tv_comment)
        val tvState = view.findViewById<TextView>(R.id.tv_state)
        val tvDelete = view.findViewById<TextView>(R.id.tv_delete)
        tvComment.setOnClickListener {
            val bundle = marker.extraInfo
            val editText = dialogView.findViewById<EditText>(R.id.edit_comment)
            editText.setText(marker.extraInfo.getString(MarkerViewModel.SP_COMMENT, ""))
            dialog.setOnDismissListener {
                bundle.putString(SP_COMMENT, editText.text.toString())
                marker.extraInfo = bundle
                viewModel.options.get(bundle.getLong(SP_ID))?.extraInfo?.putString(SP_COMMENT,editText.text.toString())
            }
            dialog.show()
        }
        tvState.setOnClickListener {
            changeMarkState(marker)
        }
        tvDelete.setOnClickListener {
            deleteMarker(marker, infoWindow)
        }
        baiduMap.showInfoWindow(infoWindow)
    }

    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        locationClient.stop()
        baiduMap.isMyLocationEnabled = false
        mapView.onDestroy()
        super.onDestroy()
        //savePoints()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                clearMarks()
            }
            R.id.save -> {
                savePoints()
            }
            R.id.guide -> {
                showGuide()
            }
        }
        return true
    }
}


