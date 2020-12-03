package com.example.inject_compiler

import com.squareup.javapoet.ClassName

object InjectUtil {

    fun get(clazz: Class<out Any>) {
        val clazzName = ClassName.bestGuess(clazz.canonicalName)
        System.out.println(clazzName)
    }

}