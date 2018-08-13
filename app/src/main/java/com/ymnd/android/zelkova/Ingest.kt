package com.ymnd.android.zelkova

import com.ymnd.android.annotation.Builder
import com.ymnd.android.annotation.DefaultValue

@Builder
data class Ingest(
        @DefaultValue(defaultValue = true)
        var index: Int?,
        @DefaultValue
        val category: Long,
        @DefaultValue(defaultValue = false)
        var name: String,
        val action: Action
)

data class Action(
        val id: Int
)