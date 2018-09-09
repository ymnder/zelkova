package com.ymnd.android.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.ymnd.android.annotation.Builder
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class BProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return hashSetOf(
                Builder::class.java.canonicalName
        )
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(Builder::class.java)
                .forEach {
                    val className = it.simpleName.toString()
                    val pack = processingEnv.elementUtils.getPackageOf(it).toString()
                    generateClass(className, pack)
                }
        return true
    }

    private fun generateClass(className: String, pack: String) {
        val fileName = "Generated_$className"
        val file = FileSpec.builder(pack, fileName)
                .addType(TypeSpec.classBuilder(fileName)
                        .addFunction(FunSpec.builder("getName")
                                .addStatement("return \"World\"")
                                .build())
                        .build())
                .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}