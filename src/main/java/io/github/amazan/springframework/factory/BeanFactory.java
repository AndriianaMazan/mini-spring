package io.github.amazan.springframework.factory;

import io.github.amazan.springframework.exception.BeansException;
import io.github.amazan.springframework.model.BeanDefinition;

public interface BeanFactory {

    void registerBeanDefinition(BeanDefinition beanDefinition);

    void instantiateSingletons();

    void destroySingletons();

    <T> T getBean(Class<T> clazz) throws BeansException;

    <T> T getBean(String name) throws BeansException;
}
