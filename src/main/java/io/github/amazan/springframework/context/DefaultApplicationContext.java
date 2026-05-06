package io.github.amazan.springframework.context;

import io.github.amazan.springframework.annotation.Component;
import io.github.amazan.springframework.annotation.Scope;
import io.github.amazan.springframework.factory.BeanFactory;
import io.github.amazan.springframework.factory.DefaultBeanFactory;
import io.github.amazan.springframework.model.BeanDefinition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefaultApplicationContext implements ApplicationContext {

    private final BeanFactory beanFactory;
    private final String packagePath;

    public DefaultApplicationContext(Class<?> primarySource) {
        this.beanFactory = new DefaultBeanFactory();
        this.packagePath = primarySource.getPackageName();
    }

    @Override
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public void load() throws ClassNotFoundException {
        findAllClasses(packagePath).stream()
                .filter(c -> c.isAnnotationPresent(Component.class))
                .map(this::createBeanDefinition)
                .forEach(beanFactory::registerBeanDefinition);

        beanFactory.instantiateSingletons();
    }

    @Override
    public void close() {
        beanFactory.destroySingletons();
    }

    private List<Class<?>> findAllClasses(String packageName) throws ClassNotFoundException {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        List<String> lines = new BufferedReader(new InputStreamReader(stream)).lines().toList();
        List<Class<?>> classes = new ArrayList<>();

        for (var line : lines) {
            if (line.endsWith(".class")) {
                classes.add(Class.forName(packageName + "." + line.substring(0, line.lastIndexOf('.'))));
            } else {
                classes.addAll(findAllClasses(packageName + "." + line));
            }
        }

        return classes;
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
