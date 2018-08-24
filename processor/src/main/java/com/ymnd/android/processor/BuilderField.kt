package com.ymnd.android.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class BuilderField(
        private val fieldElement: Element,
        private val annotatedFieldElement: Element?
) {

    private val typeConverter = TypeConverter()
    private val type = typeConverter.convert(fieldElement)
    internal val isVal: Boolean = Modifier.FINAL in this.fieldElement.modifiers
    private val isDefaultValue = this.annotatedFieldElement != null
    private val isPrimitive = type.isPrimitive
    private val isNullable = fieldElement.isNullable()
    internal val simpleName = fieldElement.simpleName
    internal val simpleNameString = "$simpleName"
    private val className = type.typeName.let { if (isNullable) it.asNullable() else it }

    fun createParamSpec(): ParameterSpec? {
        if (!isVal) return null
        return ParameterSpec
                .builder(simpleNameString, className)
                .build()
    }

    private fun createPropertySpec(typeSpecBuilder: TypeSpec.Builder) {
        if (isVal) {
            typeSpecBuilder
                    .addProperty(
                            PropertySpec.builder(simpleNameString, className)
                                    .addModifiers(KModifier.PRIVATE)
                                    .initializer(simpleNameString)
                                    .build()
                    )
        } else {
            if (isDefaultValue && type.typeName is ClassName) {
                typeSpecBuilder.addProperty(
                        PropertySpec.varBuilder(simpleNameString, className)
                                .addModifiers(KModifier.PRIVATE)
                                .initializer("${type.defaultValue}")
                                .build()
                )
            } else {
                typeSpecBuilder.addProperty(
                        PropertySpec.varBuilder(simpleNameString, className)
                                .apply {
                                    if (isPrimitive) {
                                        this.addModifiers(KModifier.PRIVATE)
                                        if (isNullable) {
                                            this.initializer("null")
                                        } else {
                                            this.delegate("kotlin.properties.Delegates.notNull()")
                                        }
                                    } else {
                                        if (isNullable) {
                                            this.addModifiers(KModifier.PRIVATE)
                                            this.initializer("null")
                                        } else {
                                            this.addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                                        }
                                    }
                                }
                                .build()
                )
            }
        }
    }

    private fun createFunctionSpec(typeSpecBuilder: TypeSpec.Builder) {
        if (isVal) return
        typeSpecBuilder.addFunction(
                FunSpec.builder(simpleNameString)
                        .addParameter(simpleNameString, className)
                        .addStatement("return apply { this.$simpleNameString = $simpleNameString }")
                        .build()
        )
    }

    fun addSpec(typeSpecBuilder: TypeSpec.Builder): TypeSpec.Builder {
        return typeSpecBuilder
                .apply {
                    createPropertySpec(this)
                    createFunctionSpec(this)
                }
    }
}