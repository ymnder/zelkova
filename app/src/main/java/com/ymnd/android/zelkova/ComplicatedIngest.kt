package com.ymnd.android.zelkova

import android.app.Activity
import com.ymnd.android.annotation.Builder

//@Builder
data class ComplicatedIngest(
//        var index: List<Int>?,
        var case: Map<String, Any?>,
        var id: List<*>,
        var hoge: Map<String, HashMap<String, Activity>>,
        var category: List<Category>
)