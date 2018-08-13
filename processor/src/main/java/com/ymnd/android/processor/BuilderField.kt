package com.ymnd.android.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

class BuilderField(
        private val fieldElement: Element,
        private val annotatedFieldElement: Element?
) {

    private val isVal: Boolean = Modifier.FINAL in this.fieldElement.modifiers
    private val isDefaultValue = this.annotatedFieldElement != null
    private val simpleName = fieldElement.simpleName
    internal val simpleNameString = "$simpleName"
    // TypeName as ClassNameはダウンキャストだから危ない
    private val className = convertClassNameAsKotlin(fieldElement.asType().asTypeName() as ClassName)

    private fun createPropertySpec(typeSpecBuilder: TypeSpec.Builder) {
        if (isVal) {
            typeSpecBuilder
                    .primaryConstructor(
                            FunSpec.constructorBuilder()
                                    .addParameter(simpleNameString, className)
                                    .build()
                    )
                    .addProperty(
                            PropertySpec.builder(simpleNameString, className)
                                    .addModifiers(KModifier.PRIVATE)
                                    .initializer(simpleNameString)
                                    .build()
                    )
        } else {
            typeSpecBuilder.addProperty(
                    PropertySpec.varBuilder(simpleNameString, className)
                            .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                            .build()
            )
        }
    }

    private fun createFunctionSpec(typeSpecBuilder: TypeSpec.Builder) {
        if (isVal) return
        typeSpecBuilder.addFunction(
                FunSpec.builder(simpleNameString)
                        .addParameter(simpleNameString, className)
                        .addStatement("return apply{ this.$simpleNameString = $simpleNameString }")
                        .build()
        )
    }

    fun addSpec(typeSpecBuilder: TypeSpec.Builder): TypeSpec.Builder {
        return typeSpecBuilder
                .apply { createPropertySpec(this) }
                .apply { createFunctionSpec(this) }
    }

    private fun convertClassNameAsKotlin(className: ClassName): ClassName {
        return when (className.canonicalName) {
            String::class.java.name -> String::class.asClassName()
            else -> className
        }
    }
}