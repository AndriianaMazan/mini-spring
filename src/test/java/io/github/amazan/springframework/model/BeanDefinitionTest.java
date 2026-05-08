package io.github.amazan.springframework.model;

import io.github.amazan.springframework.annotation.Scope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeanDefinitionTest {

    @Test
    void shouldExposeNameTypeAndScopeFromConstructor() {
        BeanDefinition definition = new BeanDefinition("any", Object.class, Scope.SINGLETON);

        assertThat(definition.getName()).isEqualTo("any");
        assertThat(definition.getType()).isEqualTo(Object.class);
        assertThat(definition.getScope()).isEqualTo(Scope.SINGLETON);
    }
}
