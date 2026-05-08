package io.github.amazan.springframework.model;

public class BeanDefinition {

    private final String name;
    private final Class<?> type;
    private final String scope;

    public BeanDefinition(String name, Class<?> type, String scope) {
        this.name = name;
        this.type = type;
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }
}
