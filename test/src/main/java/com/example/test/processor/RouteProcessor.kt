package com.example.test.processor


import com.example.test.PACKAGE_PATH
import com.example.test.annotation.Route
import com.example.test.apt.IRouteGroup
import com.example.test.apt.IRouteRoot
import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import java.io.IOException
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.collections.HashMap

@AutoService(Processor::class)
class  RouteProcessor : AbstractProcessor() {

    private lateinit var filer: Filer
    private lateinit var moduleName:String
    private lateinit var logUtil:Messager

    override fun getSupportedAnnotationTypes() = setOf(Route::class.java.canonicalName)


    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        filer = processingEnvironment.filer
        logUtil = processingEnvironment.messager
        moduleName = processingEnvironment.options["moduleName"]?:""
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }
    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        if (set.isEmpty()){
            return false
        }
        val elements = roundEnvironment.getElementsAnnotatedWith(Route::class.java)

        val groups = HashMap<String,HashMap<String,String>>()
        for (element in elements) {
            logUtil.printMessage(Diagnostic.Kind.MANDATORY_WARNING,"hello")
            val typeElement = element as TypeElement
            val path = typeElement.getAnnotation(Route::class.java).path
            val groupName = getRootName(path)
            groups[groupName] = groups[groupName]?:HashMap()
            groups[groupName]!![path] = typeElement.qualifiedName.toString()
        }
        generate(groups)
        return true
    }



    private fun generate(routes: Map<String, Map<String,String>>){
        val roots = TreeMap<String,String>()
        for(entry in routes){
            val className = "Group${entry.key}Routes"
            generateGroup(entry.value,className)
            roots[entry.key] = className
        }
        generatedRoot(roots)
    }



    private fun getRootName(path:String):String{
        val index = path.indexOf("/")
        return path.substring(0,index)
    }

    private fun getClassName(name: String): ClassName {
        val index = name.lastIndexOf(".")
        val pkg = name.substring(0, index)
        val simple = name.substring(index + 1, name.length)
        return ClassName.get(pkg, simple)
    }

    private fun generatedRoot(rootMap:Map<String,String>) {
        //创建参数类型 Map<String,Class<? extends IRouteGroup>> routes>
        //Wildcard 通配符
        val iRouteGroup = ClassName.get(IRouteGroup::class.java)
        val iRouteRoot = ClassName.get(IRouteRoot::class.java)
        val parameterizedTypeName = ParameterizedTypeName.get(
            ClassName.get(HashMap::class.java),
            ClassName.get(String::class.java),
            ParameterizedTypeName.get(
                ClassName.get(Class::class.java),
                WildcardTypeName.subtypeOf(iRouteGroup)
            )
        )
        //生成参数 Map<String,Class<? extends IRouteGroup>> routes> routes
        val parameter = ParameterSpec.builder(parameterizedTypeName, "routes").build()

        //生成函数 public void loadInfo(Map<String,Class<? extends IRouteGroup>> routes> routes)
        val methodBuilder = MethodSpec.methodBuilder("loadInto")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(parameter)
        //生成函数体
        for (entry in rootMap) {
            methodBuilder.addStatement(
                "routes.put(\$S, \$T.class)",
                entry.key,
                ClassName.get(PACKAGE_PATH, entry.value)
            )
        }
        //生成$Root$类
        if(moduleName.isEmpty()){
            for(s in rootMap.keys){
                moduleName = s
                break
            }
        }
        val className = "Root${moduleName}Routes"
        val typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(iRouteRoot)
            .addModifiers(Modifier.PUBLIC)
            .addMethod(methodBuilder.build())
            .build()
        try {
            //生成java文件，PACKAGE_OF_GENERATE_FILE就是生成文件需要的路径
            JavaFile.builder(PACKAGE_PATH,typeSpec).build()
                .writeTo(filer)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun generateGroup(map: Map<String, String>,className:String) {
        val mapName = ClassName.get("java.util", "HashMap")
        val parameterSpec = ParameterSpec.builder(mapName, "routes").build()
        val methodBuilder = MethodSpec.methodBuilder("loadInto")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(parameterSpec)

        for ((key, value) in map) {
            methodBuilder.addStatement("routes.put(\$S, \$T.class)", key, getClassName(value))
        }

        val superClass = ClassName.get(PACKAGE_PATH,"IRouteGroup")
        val typeSpec = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(superClass)
            .addMethod(methodBuilder.build())
            .build()
        try {
            JavaFile.builder(PACKAGE_PATH, typeSpec)
                .build()
                .writeTo(filer)

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
