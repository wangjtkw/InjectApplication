package com.example.inject_compiler

import com.squareup.javapoet.*
import java.io.IOException
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Modifier
import javax.tools.Diagnostic

object InjectGenerate {

    fun generateCode(
        autoProvideSet: HashSet<String>,
        provideMap: HashMap<String, ArrayList<String>>,
        injectMap: HashMap<String, ArrayList<HashMap<String, String>>>,
        filer: Filer,
        logUtil: Messager
    ) {
        /**
         * 构建InjectCreatorPool类
         * public class InjectCreatorPool{
         * }
         */
        val injectPoolClassName = INJECT_POOL_NAME
        val injectPoolTypeBuilder = TypeSpec.classBuilder(injectPoolClassName)
            .addModifiers(Modifier.PUBLIC)

        autoProvideSet.forEach { className ->
            val classNameSimpleNameLowerCase = getLowerCaseString(getSimpleName(className))
            val classNameSimpleNameUpperCase = getUpperCaseString(getSimpleName(className))

            val injectPoolReturnClass = ClassName.bestGuess(className)

            /**
             * public static Bean beanCreator(){
             *     return new Bean();
             * }
             */
            val injectPoolMethodBuilder =
                MethodSpec.methodBuilder("${classNameSimpleNameLowerCase}Creator")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(injectPoolReturnClass)
                    .addStatement("return new $classNameSimpleNameUpperCase()")
                    .build()

            //将方法添加到类中
            injectPoolTypeBuilder.addMethod(injectPoolMethodBuilder)
        }

        provideMap.forEach { (className, parametersTypeList) ->
            val classNameSimpleNameLowerCase = getLowerCaseString(getSimpleName(className))
            val classNameSimpleNameUpperCase = getUpperCaseString(getSimpleName(className))

            val injectPoolReturnClass = ClassName.bestGuess(className)

            /**
             * public static Bean beanCreator(){
             *     return new Bean(beanACreator(),beanBCreator());
             * }
             */
            val injectPoolMethodBuilder =
                MethodSpec.methodBuilder("${classNameSimpleNameLowerCase}Creator")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(injectPoolReturnClass)

            //构造构造函数参数
            var constructorParameter: String = ""
            parametersTypeList.forEach {
                val lowerCaseName = getLowerCaseString(getSimpleName(it))
                constructorParameter = "${constructorParameter}${lowerCaseName}Creator(),"
            }
            //去掉最末尾多出来的","
            constructorParameter =
                constructorParameter.substring(0, constructorParameter.length - 1)
            injectPoolMethodBuilder.addStatement("return new $classNameSimpleNameUpperCase(${constructorParameter})")

            //将方法添加到类中
            injectPoolTypeBuilder.addMethod(injectPoolMethodBuilder.build())

        }

        try {
            //生成java文件，PACKAGE_OF_GENERATE_FILE就是生成文件需要的路径
            JavaFile.builder(PACKAGE_PATH, injectPoolTypeBuilder.build()).build()
                .writeTo(filer)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        /**
         * ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑构建InjectCreatorPool类↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
         */


        injectMap.forEach { (classType, variableNameTypeMapList) ->
            val classSimpleNameUpperCase = getUpperCaseString(getSimpleName(classType))
            val classSimpleNameLowerCase = getLowerCaseString(getSimpleName(classType))

            /**
             * public class MainActivityParametersCreator {
             *
             * }
             */
            val className = "${classSimpleNameUpperCase}ParametersCreator"
//            logUtil.printMessage(Diagnostic.Kind.MANDATORY_WARNING, className)

            val typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)

            val injectClass = ClassName.bestGuess(classType)


            /**
             * public static void inject(MainActivity mainActivity){
             *
             * }
             */
            val parameterSpec = ParameterSpec.builder(injectClass, classSimpleNameLowerCase).build()
            val injectMethodBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(parameterSpec)

            variableNameTypeMapList.forEach { map ->
                map.forEach { (variableName, variableType) ->
                    val injectableClass = ClassName.bestGuess(variableType)
                    val variableNameUpperCase = getUpperCaseString(variableName)
                    val variableTypeSimpleName = getUpperCaseString(getSimpleName(variableType))
                    val variableNameLowerCase = getLowerCaseString(getSimpleName(variableType))

                    /**
                     * private static Bean createMainActivityBean() {
                     *      return new Bean();
                     *  }
                     */
                    val methodName = "create$classSimpleNameUpperCase$variableNameUpperCase"
                    val methodSpec =
                        MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                            .returns(injectableClass)


                    if (provideMap.containsKey(variableType)) {
                        methodSpec.addStatement("return ${INJECT_POOL_NAME}.${variableNameLowerCase}Creator()")
                    } else {
                        methodSpec.addStatement("return new $variableTypeSimpleName()")
                    }


                    /**
                     * public static void inject(MainActivity mainActivity){
                     *     mainActivity.bean = createMainActivityBean();
                     * }
                     */
                    injectMethodBuilder.addStatement("${classSimpleNameLowerCase}.${variableName} = ${methodName}()")

                    /**
                     * public class MainActivityParametersCreator {
                     * private static Bean createMainActivityBean() {
                     *      return new Bean();
                     *  }
                     * }
                     */
                    typeBuilder.addMethod(methodSpec.build())
                }
            }

            typeBuilder.addMethod(injectMethodBuilder.build())

            try {
                //生成java文件，PACKAGE_OF_GENERATE_FILE就是生成文件需要的路径
                JavaFile.builder(PACKAGE_PATH, typeBuilder.build()).build()
                    .writeTo(filer)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun getSimpleName(className: String): String {
        val index = className.indexOfLast { it == '.' }
        return className.substring(index + 1)
    }

    private fun getLowerCaseString(str: String): String {
        return str.substring(0, 1).toLowerCase() + str.substring(1)
    }

    private fun getUpperCaseString(str: String): String {
        return str.substring(0, 1).toUpperCase() + str.substring(1)
    }
}