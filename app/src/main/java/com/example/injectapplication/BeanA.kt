package com.example.injectapplication

import com.example.inject_compiler.annotation.AutoProvide
import com.example.inject_compiler.annotation.Injectable
import com.example.inject_compiler.annotation.Provide

@AutoProvide
class BeanA @Provide constructor(val beanB: BeanB){
    private val name:String = ""
}