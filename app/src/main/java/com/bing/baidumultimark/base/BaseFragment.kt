package com.at.festivalcamera.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * @author liangbinghao
 * @date 2021/11/3 14:22
 * @desc
 */
abstract class BaseFragment<VB:ViewBinding>(private val mInflater: (LayoutInflater,ViewGroup,Boolean)->VB) : Fragment() {

    val TAG = "BaseFragment"
    lateinit var binding: VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = mInflater.invoke(inflater,container!!,false)
        return binding.root
    }
}