package com.example.injectapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.inject_compiler.InjectUtil
import com.example.inject_compiler.annotation.Inject
import com.example.inject_compiler.MainActivityParametersCreator
import com.example.inject_compiler.annotation.Provide


class MainActivity @Provide constructor() : AppCompatActivity() {

    @Inject
    lateinit var beanA: Bean

    @Inject
    lateinit var beanB: Bean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MainActivityParametersCreator.inject(this)
//        val beans = Bean(_class = 0)
//        Log.e("main",bean.toString())
    }

}