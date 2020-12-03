package com.example.test.annotation

import org.jetbrains.annotations.NotNull
import kotlin.reflect.KClass


/**
 * @Route 只能在activity中使用
 * @param path 应该包含分组，例如 path="login/cellphone",login为登录分组
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@kotlin.annotation.Retention(AnnotationRetention.BINARY)
annotation class Route(@NotNull val path: String,val clazz:KClass<*> = Any::class)