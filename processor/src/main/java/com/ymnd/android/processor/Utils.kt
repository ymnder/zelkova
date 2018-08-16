package com.ymnd.android.processor

import javax.lang.model.element.Element

fun Element.isNullable(): Boolean {
    return this.annotationMirrors.any { it.annotationType.asElement().simpleName.contains("Nullable") }
}