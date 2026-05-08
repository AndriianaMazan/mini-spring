package io.github.amazan.springframework.context;

import io.github.amazan.springframework.factory.BeanFactory;

public interface ApplicationContext {

    BeanFactory getBeanFactory();

    void run();

    void close();

}
