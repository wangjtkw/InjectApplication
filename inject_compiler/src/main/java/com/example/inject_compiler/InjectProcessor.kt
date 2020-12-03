package com.example.inject_compiler

import com.example.inject_compiler.annotation.AutoProvide
import com.example.inject_compiler.annotation.Inject
import com.example.inject_compiler.annotation.Injectable
import com.example.inject_compiler.annotation.Provide
import com.google.auto.service.AutoService
import java.lang.RuntimeException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
class InjectProcessor : AbstractProcessor() {
    private val injectableSet = HashSet<String>()
    private val injectMap = HashMap<String, ArrayList<HashMap<String, String>>>()
    private val autoProvideSet = HashSet<String>()
    private val provideMap = HashMap<String, ArrayList<String>>()


    private lateinit var filer: Filer
    private lateinit var logUtil: Messager

    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        filer = processingEnvironment.filer
        logUtil = processingEnvironment.messager
    }

    override fun getSupportedAnnotationTypes() =
        setOf(
            Inject::class.java.canonicalName,
            Injectable::class.java.canonicalName,
            Provide::class.java.canonicalName,
            AutoProvide::class.java.canonicalName
        )

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    /**
     * Element博客
     * https://blog.csdn.net/u010405231/article/details/52210401
     */

    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        if (set == null || set.isEmpty() || roundEnvironment == null) {
            return false
        }


        getAutoProvideAnnotation(roundEnvironment)
        getProvideAnnotation(roundEnvironment)
        getInjectableAnnotation(roundEnvironment)
        getInjectAnnotation(roundEnvironment)
        getProvideAutoProvideMap()

        InjectGenerate.generateCode(
            autoProvideSet,
            provideMap,
            injectMap,
            filer,
            logUtil
        )
        return true
    }

    private fun getInjectAnnotation(roundEnvironment: RoundEnvironment) {
        val injectElements = roundEnvironment.getElementsAnnotatedWith(Inject::class.java)

        for (element in injectElements) {
            //variableElement 修饰属性、类成员
            val variableElement = element as VariableElement

            //将variableElement 转为typeElement
            val typeElement = element.enclosingElement as TypeElement

            val variableNameTypeMap = HashMap<String, String>()

            //属性名
            val variableName = variableElement.simpleName.toString()
            //属性类型
            val variableType = variableElement.asType().toString()

//            logUtil.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "variableType$variableType")

            if (!injectableSet.contains(variableType)) {
                throw RuntimeException("$variableType is not annotated by @injectable;")
            }

            variableNameTypeMap.put(variableName, variableType)

            //所属类全类名
            val classType = typeElement.qualifiedName.toString()

            if (injectMap.containsKey(classType)) {
                val variableNameTypeMapList = injectMap[classType]
                if (variableNameTypeMapList != null) {
                    variableNameTypeMapList.add(variableNameTypeMap)
                    injectMap[classType] = variableNameTypeMapList
                } else {
                    val newList = ArrayList<HashMap<String, String>>()
                    newList.add(variableNameTypeMap)
                    injectMap[classType] = newList
                }
            } else {
                val newList = ArrayList<HashMap<String, String>>()
                newList.add(variableNameTypeMap)
                injectMap.put(classType, newList)
            }
        }



    }

    private fun getInjectableAnnotation(roundEnvironment: RoundEnvironment) {
        val injectableElements = roundEnvironment.getElementsAnnotatedWith(Injectable::class.java)
        for (element in injectableElements) {
            //typeElement 定义类、接口、枚举
            val typeElement = element as TypeElement

            val typeElementName = typeElement.qualifiedName.toString()
            //将类的全类名添加到set集合中
            injectableSet.add(typeElementName)

//            logUtil.printMessage(
//                Diagnostic.Kind.MANDATORY_WARNING,
//                "typeElementName$typeElementName"
//            )
        }
    }

    private fun getAutoProvideAnnotation(roundEnvironment: RoundEnvironment) {
        val autoProvideElements = roundEnvironment.getElementsAnnotatedWith(AutoProvide::class.java)

        for (element in autoProvideElements) {
            val typeElement = element as TypeElement

            //类的全类名
            val typeElementName = typeElement.qualifiedName.toString()
            //将类的全类名添加到set集合中
            autoProvideSet.add(typeElementName)

//            logUtil.printMessage(
//                Diagnostic.Kind.MANDATORY_WARNING,
//                "typeElementName$typeElementName"
//            )
        }
    }

    private fun getProvideAnnotation(roundEnvironment: RoundEnvironment) {
        val provideElements = roundEnvironment.getElementsAnnotatedWith(Provide::class.java)

        for (element in provideElements) {
            //获取该方法所在类的Element
            val classElement = element.enclosingElement as TypeElement
            //获取该方法所在类的全类名
            val fullClassName = classElement.qualifiedName.toString()

            //注解作用于方法或构造函数
            val executableElement = element as ExecutableElement
            //获取该方法的参数列表
            val methodParameters = executableElement.parameters
            //存放构造函数参数类型列表
            val parametersTypeList = ArrayList<String>();

            for (type in methodParameters) {
                checkParameters(type, fullClassName)
                //全类名
                val typeName = type.asType().toString()
                logUtil.printMessage(Diagnostic.Kind.WARNING, "typeName $typeName \n")

                parametersTypeList.add(typeName)
            }
            if (parametersTypeList.isNotEmpty()) {
                provideMap.put(fullClassName, parametersTypeList)
            }

        }
    }

    private fun checkParameters(element: VariableElement, fullClassName: String) {
        val elementType = element.asType().toString()
        if (autoProvideSet.contains(elementType)) {
            return
        }
        if (element.constantValue != null) {
            return
        }
        throw RuntimeException("$fullClassName constructor parameters is illegal")
    }

    private fun getProvideAutoProvideMap() {
        val iterator = autoProvideSet.iterator()
        while (iterator.hasNext()) {
            val autoClassName = iterator.next()
            //如果自动提供的构造函数所在类也需要自动提供
            if (provideMap.containsKey(autoClassName)) {
//                provideAutoProvide.put(autoClassName, provideMap[autoClassName]!!)
                iterator.remove()

                //避免重复构造代码
//                provideMap.remove(autoClassName)
            }
        }

    }


}