package com.example.inject_compiler;

import com.example.injectapplication.Bean;
import com.example.injectapplication.BeanA;
import com.example.injectapplication.BeanB;

public class InjectCreatorPool {
  public static BeanB beanBCreator() {
    return new BeanB();
  }

  public static BeanA beanACreator() {
    return new BeanA(beanBCreator());
  }

  public static Bean beanCreator() {
    return new Bean(beanBCreator(),beanACreator());
  }
}
