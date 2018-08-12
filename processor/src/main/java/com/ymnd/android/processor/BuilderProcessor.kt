package com.ymnd.android.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.ymnd.android.annotation.Builder
import java.io.File
import javax.annotation.processing.Messager
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.util.Elements

@AutoService(Processor::class)
class BuilderProcessor : BasicAnnotationProcessor() {
    companion object {
        private const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!

    override fun getSupportedOptions() = setOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)

    override fun initSteps(): MutableIterable<ProcessingStep> {
        val outputDirectory = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                ?.let { File(it) }
                ?: throw IllegalArgumentException("No output directory!")

        return mutableListOf(
                BuilderProcessingStep(
                        elements = processingEnv.elementUtils,
                        messager = processingEnv.messager,
                        outputDir = outputDirectory
                )
        )
    }
}

class BuilderProcessingStep(private val elements: Elements, private val messager: Messager, private val outputDir: File) : BasicAnnotationProcessor.ProcessingStep {

    override fun annotations() = mutableSetOf(Builder::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>?): MutableSet<Element> {
        elementsByAnnotation ?: return mutableSetOf()

        val klass = TypeSpec
                .classBuilder("IngestBuilder")
                .build()

        FileSpec.builder("com.ymnd.android.builder", klass.name!!)
                .addType(klass)
                .build()
                .writeTo(outputDir)

        return mutableSetOf()
    }
}