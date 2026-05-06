package io.github.amazan.springframework.factory;

import io.github.amazan.springframework.annotation.Autowired;
import io.github.amazan.springframework.annotation.PostConstruct;
import io.github.amazan.springframework.annotation.PreDestroy;
import io.github.amazan.springframework.annotation.Scope;
import io.github.amazan.springframework.exception.BeanNotFoundException;
import io.github.amazan.springframework.exception.BeansException;
import io.github.amazan.springframework.model.BeanDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DefaultBeanFactory implements BeanFactory {

    private final Map<String, BeanDefinition> definitions;
    private final Map<String, Object> singletons;

    public DefaultBeanFactory() {
        this.definitions = new LinkedHashMap<>();
        this.singletons = new LinkedHashMap<>();
    }

    @Override
    public void registerBeanDefinition(BeanDefinition beanDefinition) {
        definitions.put(beanDefinition.getName(), beanDefinition);
    }

    @Override
    public void instantiateSingletons() throws BeansException {
        for (var definition : definitions.entrySet()) {
            if (Scope.SINGLETON.equals(definition.getValue().getScope())) {
                try {
                    singletons.put(definition.getKey(), definition.getValue().getType().getDeclaredConstructor().newInstance());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new BeansException("Bean could not be created", e);
                }
            }
        }

        // populate beans
        for (var singleton : singletons.entrySet()) {
            var bean = singleton.getValue();
            injectFields(bean.getClass(), bean);
        }

        // postConstruct method invoke
        for (var bean : singletons.entrySet()) {
            invokeLifecycleMethod(bean.getValue(), PostConstruct.class);
        }
    }

    @Override
    public void destroySingletons() throws BeansException {
        var singletonsSnapshot = new ArrayList<>(singletons.values());
        Collections.reverse(singletonsSnapshot);

        for (var singleton : singletonsSnapshot) {
            invokeLifecycleMethod(singleton, PreDestroy.class);
        }

        singletons.clear();
    }

    @Override
    public <T> T getBean(Class<T> clazz) throws BeansException {
        assertNotNull(clazz);

        for (var bf : definitions.entrySet()) {
            if (bf.getValue().getType().isAssignableFrom(clazz)) {
                var instance = singletons.get(bf.getKey());

                if (instance == null) {
                    instance = createPrototypeBean(bf.getValue());
                }

                return (T) instance;
            }
        }

        throw new BeanNotFoundException("Bean is not found.");
    }

    @Override
    public <T> T getBean(String name) throws BeansException {
        assertNotNull(name);

        var instance = singletons.get(name);

        if (instance != null) {
            return (T) instance;
        } else {
            if (definitions.containsKey(name)) {
                return createPrototypeBean(definitions.get(name));
            } else {
                throw new BeanNotFoundException("Bean is not found.");
            }
        }
    }

    private <T> T createPrototypeBean(BeanDefinition bf) {
        var clazz = bf.getType();
        Object instance;

        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new BeansException("Bean could not be created", e);
        }

        injectFields(clazz, instance);
        invokeLifecycleMethod(instance, PostConstruct.class);

        return (T) instance;
    }

    private void injectFields(Class<?> clazz, Object instance) {
        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Autowired.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    var obj = getBean(field.getType());
                    try {
                        field.set(instance, obj);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void invokeLifecycleMethod(Object bean, Class<? extends Annotation> annotation) {
        Arrays.stream(bean.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotation))
                .findFirst()
                .ifPresent(method -> {
                    method.setAccessible(true);
                    try {
                        method.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void assertNotNull(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("Input parameter should not be null.");
        }
    }

}
