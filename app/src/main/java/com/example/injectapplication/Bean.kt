package com.example.injectapplication

import com.example.inject_compiler.annotation.Inject
import com.example.inject_compiler.annotation.Injectable
import com.example.inject_compiler.annotation.Provide


@Injectable
class Bean @Provide constructor(val beanB: BeanB,val beanA: BeanA) {


//    override fun toString(): String {
//        return "Bean(name='$name', age=$age)"
//    }

}