package io.github.amazan.springframework.factory;

import io.github.amazan.springframework.model.BeanDefinition;

public interface BeanFactory {

    void registerBeanDefinition(BeanDefinition beanDefinition);

    void instantiateSingletons();

    void destroySingletons();

    <T> T getBean(Class<T> clazz);

    <T> T getBean(String name);
}
