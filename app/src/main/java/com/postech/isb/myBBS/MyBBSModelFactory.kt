package com.postech.isb.ui.mybbs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.postech.isb.util.IsbSession

class MyBBSViewModelFactory(
    private val isbSession: IsbSession
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyBBSViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyBBSViewModel(isbSession) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
