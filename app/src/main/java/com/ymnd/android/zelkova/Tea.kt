package com.ymnd.android.zelkova

import com.ymnd.android.annotation.Builder

@Builder
data class Tea (
        val id: Int,
        val name: String,
        val price: Int?,
        val category: List<String>
)