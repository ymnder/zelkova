package com.ymnd.android.processor

import com.google.auto.service.AutoService
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.ymnd.android.annotation.Builder
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class JavaBuilderProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> {
        return hashSetOf(
                Builder::class.java.canonicalName
        )
    }

    override fun process(set: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(Builder::class.java)
                .forEach {
                    val className = "${it.simpleName}"
                    val packageName = "${processingEnv.elementUtils.getPackageOf(it)}"
                    generateClassFile(className, packageName)
                }
        return true
    }

    private fun generateClassFile(className: String, packageName: String) {
        val fileName = "${className}Builder"
        val file = JavaFile.builder(packageName, TypeSpec.classBuilder(fileName).build())
                .build()

        file.writeTo(processingEnv.filer)
    }
}