package com.ymnd.android.zelkova

import com.ymnd.android.annotation.Builder
import com.ymnd.android.annotation.DefaultValue

@Builder
data class Ingest(
        @DefaultValue(defaultValue = true)
        var index: Int?,
        @DefaultValue
        val millSec: Long,
        @DefaultValue(defaultValue = false)
        var name: String,
        val action: Action,
        var id: Long,
        var category: Category
)

data class Action(
        val id: Int
)

data class Category(
        val id: Int
)