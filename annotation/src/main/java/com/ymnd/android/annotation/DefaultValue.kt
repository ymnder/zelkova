package com.ymnd.android.annotation

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class DefaultValue(val defaultValue: Boolean = true)