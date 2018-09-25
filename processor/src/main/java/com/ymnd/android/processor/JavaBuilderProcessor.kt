package com.ymnd.android.processor

import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import com.ymnd.android.annotation.Builder
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class JavaBuilderProcessor : AbstractProcessor() {

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
                .forEach(::generateClassFile)
        return true
    }

    private fun generateClassFile(targetElement: Element) {
        val className = targetElement.simpleName.toString()
        val packageName = "${processingEnv.elementUtils.getPackageOf(targetElement)}"
        val fileName = "${className}Builder"
        val builderName = ClassName.get(packageName, fileName)
        val targetName = ClassName.get(packageName, className)

        val typeSpecBuilder = TypeSpec.classBuilder(fileName)

        val fieldNames = mutableListOf<String>()
        targetElement.enclosedElements
                .filter { it.kind == ElementKind.FIELD }
                .forEach {
                    val fieldName = it.simpleName.toString()
                    val fieldType = TypeName.get(it.asType())
                    val annotation =
                            if (it.isNullable()) {
                                Nullable::class.java
                            } else {
                                NotNull::class.java
                            }

                    val fieldSpec = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE)
                            .addAnnotation(annotation)
                            .build()

                    val parameterSpec = ParameterSpec.builder(fieldType, fieldName)
                            .addAnnotation(annotation)
                            .build()

                    val methodSpec = MethodSpec.methodBuilder(fieldName)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(parameterSpec)
                            .returns(builderName)
                            .addStatement("this.\$N = \$N", fieldName, fieldName)
                            .addStatement("return this")
                            .build()


                    typeSpecBuilder.addField(fieldSpec)
                            .addMethod(methodSpec)

                    fieldNames.add(fieldName)
                }

        val buildMethodSpec = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(targetName)
                .addStatement(
                        "return new \$T(${Collections.nCopies(fieldNames.size, "\$N").joinToString()})",
                        targetName,
                        *fieldNames.toTypedArray()
                )
                .build()

        typeSpecBuilder.addMethod(buildMethodSpec)

        val file = JavaFile.builder(packageName, typeSpecBuilder.build())
                .build()

        file.writeTo(processingEnv.filer)
    }
}