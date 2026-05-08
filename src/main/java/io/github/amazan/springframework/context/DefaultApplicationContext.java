package io.github.amazan.springframework.context;

import io.github.amazan.springframework.annotation.Component;
import io.github.amazan.springframework.annotation.Scope;
import io.github.amazan.springframework.exception.ApplicationContextException;
import io.github.amazan.springframework.factory.BeanFactory;
import io.github.amazan.springframework.factory.DefaultBeanFactory;
import io.github.amazan.springframework.model.BeanDefinition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultApplicationContext implements ApplicationContext {

    private final AtomicBoolean closed;
    private final Thread shutdownHook;

    private final BeanFactory beanFactory;
    private final String packagePath;

    public DefaultApplicationContext(Class<?> primarySource) {
        closed = new AtomicBoolean(false);
        shutdownHook = new Thread(this::close, "spring-shutdown-hook");
        this.beanFactory = new DefaultBeanFactory();
        this.packagePath = primarySource.getPackageName();
    }

    @Override
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public void run() {
        load();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        if (Thread.currentThread() != shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        beanFactory.destroySingletons();
    }

    private void load() {
        try {
            findAllClasses(packagePath).stream()
                    .filter(c -> c.isAnnotationPresent(Component.class))
                    .map(this::createBeanDefinition)
                    .forEach(beanFactory::registerBeanDefinition);

            beanFactory.instantiateSingletons();

            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (RuntimeException e) {
            beanFactory.destroySingletons();
            throw e;
        }

    }

    private List<Class<?>> findAllClasses(String packageName) {
        var stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));

        if (stream == null) {
            throw new ApplicationContextException("Package " + packageName + " not found on classpath");
        }

        try (var bufferedReader = new BufferedReader(new InputStreamReader(stream))) {
            List<String> lines = bufferedReader.lines().toList();
            List<Class<?>> classes = new ArrayList<>();

            for (var line : lines) {
                if (line.endsWith(".class")) {
                    classes.add(Class.forName(packageName + "." + line.substring(0, line.lastIndexOf('.'))));
                } else {
                    classes.addAll(findAllClasses(packageName + "." + line));
                }
            }

            return classes;
        } catch (ClassNotFoundException | IOException e) {
            throw new ApplicationContextException("Failed to scan package '" + packageName + "'.", e);
        }
    }

    private <T> BeanDefinition createBeanDefinition(Class<T> clazz) {
        var name = Optional.of(clazz.getAnnotation(Component.class))
                .map(Component::value)
                .filter(value -> !value.isEmpty())
                .orElseGet(() -> Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1));

        var scope = Optional.ofNullable(clazz.getAnnotation(Scope.class))
                .map(Scope::value)
                .orElse(Scope.SINGLETON);

        return new BeanDefinition(name, clazz, scope);
    }
}
