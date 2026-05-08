package io.github.amazan.springframework.factory;

import io.github.amazan.springframework.annotation.Autowired;
import io.github.amazan.springframework.annotation.PostConstruct;
import io.github.amazan.springframework.annotation.PreDestroy;
import io.github.amazan.springframework.annotation.Scope;
import io.github.amazan.springframework.exception.BeanNotFoundException;
import io.github.amazan.springframework.exception.BeansException;
import io.github.amazan.springframework.fixtures.LifecycleRecorder;
import io.github.amazan.springframework.model.BeanDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultBeanFactoryTest {

    private DefaultBeanFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultBeanFactory();
        LifecycleRecorder.reset();
    }

    @Test
    void shouldCreateSingletonInstancesOnInstantiate() {
        factory.registerBeanDefinition(new BeanDefinition("simple", SimpleBean.class, Scope.SINGLETON));

        factory.instantiateSingletons();

        SimpleBean bean = factory.getBean("simple");
        assertThat(bean).isNotNull();
    }

    @Test
    void shouldNotInstantiatePrototypesEagerly() {
        factory.registerBeanDefinition(new BeanDefinition("proto", SimpleBean.class, Scope.PROTOTYPE));

        factory.instantiateSingletons();

        SimpleBean first = factory.getBean("proto");
        SimpleBean second = factory.getBean("proto");
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void shouldInjectAutowiredFieldsAfterAllSingletonsAreCreated() {
        factory.registerBeanDefinition(new BeanDefinition("dependency", Dependency.class, Scope.SINGLETON));
        factory.registerBeanDefinition(new BeanDefinition("consumer", BeanWithAutowiredField.class, Scope.SINGLETON));

        factory.instantiateSingletons();

        BeanWithAutowiredField consumer = factory.getBean("consumer");
        Dependency dependency = factory.getBean("dependency");
        assertThat(consumer.getDependency()).isSameAs(dependency);
    }

    @Test
    void shouldInvokePostConstructAfterFieldInjection() {
        factory.registerBeanDefinition(new BeanDefinition("dependency", Dependency.class, Scope.SINGLETON));
        factory.registerBeanDefinition(new BeanDefinition("checker", BeanCheckingDepAtPostConstruct.class, Scope.SINGLETON));

        factory.instantiateSingletons();

        assertThat(LifecycleRecorder.dependencyVisibleAtPostConstruct).isTrue();
        assertThat(LifecycleRecorder.postConstructEvents).contains("BeanCheckingDepAtPostConstruct");
    }

    @Test
    void shouldThrowBeansExceptionWhenBeanHasNoNoArgConstructor() {
        factory.registerBeanDefinition(new BeanDefinition("broken", BeanWithoutNoArgCtor.class, Scope.SINGLETON));

        assertThatThrownBy(() -> factory.instantiateSingletons())
                .isInstanceOf(BeansException.class);
    }

    @Test
    void shouldPropagateExceptionWhenPostConstructThrows() {
        factory.registerBeanDefinition(new BeanDefinition("explosive", BeanWhosePostConstructThrows.class, Scope.SINGLETON));

        assertThatThrownBy(() -> factory.instantiateSingletons())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldInvokePreDestroyOnAllSingletons() {
        factory.registerBeanDefinition(new BeanDefinition("a", OrderedBeanA.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        factory.destroySingletons();

        assertThat(LifecycleRecorder.preDestroyEvents).containsExactly("A");
    }

    @Test
    void shouldInvokePreDestroyInReverseInstantiationOrder() {
        factory.registerBeanDefinition(new BeanDefinition("a", OrderedBeanA.class, Scope.SINGLETON));
        factory.registerBeanDefinition(new BeanDefinition("b", OrderedBeanB.class, Scope.SINGLETON));
        factory.registerBeanDefinition(new BeanDefinition("c", OrderedBeanC.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        factory.destroySingletons();

        assertThat(LifecycleRecorder.preDestroyEvents).containsExactly("C", "B", "A");
    }

    @Test
    void shouldClearSingletonsAfterDestroy() {
        factory.registerBeanDefinition(new BeanDefinition("a", OrderedBeanA.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        factory.destroySingletons();

        assertThatThrownBy(() -> factory.getBean(OrderedBeanA.class))
                .isInstanceOf(BeansException.class)
                .hasMessageContaining("not been instantiated");
    }

    @Test
    void shouldNotInvokePreDestroyOnPrototypes() {
        factory.registerBeanDefinition(new BeanDefinition("proto", PrototypeBeanWithPreDestroy.class, Scope.PROTOTYPE));
        factory.instantiateSingletons();
        factory.getBean(PrototypeBeanWithPreDestroy.class);

        factory.destroySingletons();

        assertThat(LifecycleRecorder.preDestroyEvents).doesNotContain("Prototype");
    }

    @Test
    void shouldBeIdempotentWhenDestroyCalledTwice() {
        factory.registerBeanDefinition(new BeanDefinition("a", OrderedBeanA.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        factory.destroySingletons();
        factory.destroySingletons();

        assertThat(LifecycleRecorder.preDestroyEvents).containsExactly("A");
    }

    @Test
    void shouldReturnSameSingletonInstanceOnMultipleCallsByClass() {
        factory.registerBeanDefinition(new BeanDefinition("simple", SimpleBean.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        SimpleBean first = factory.getBean(SimpleBean.class);
        SimpleBean second = factory.getBean(SimpleBean.class);

        assertThat(first).isSameAs(second);
    }

    @Test
    void shouldResolveInterfaceToImplementation() {
        factory.registerBeanDefinition(new BeanDefinition("impl", DemoInterfaceImpl.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        DemoInterface bean = factory.getBean(DemoInterface.class);

        assertThat(bean).isInstanceOf(DemoInterfaceImpl.class);
    }

    @Test
    void shouldInjectImplementationIntoFieldDeclaredAsInterface() {
        factory.registerBeanDefinition(new BeanDefinition("impl", DemoInterfaceImpl.class, Scope.SINGLETON));
        factory.registerBeanDefinition(new BeanDefinition("consumer", BeanRequiringInterface.class, Scope.SINGLETON));

        factory.instantiateSingletons();

        BeanRequiringInterface consumer = factory.getBean("consumer");
        assertThat(consumer.getDemoInterface()).isInstanceOf(DemoInterfaceImpl.class);
    }

    @Test
    void shouldCreateFreshPrototypeOnEachCallByClass() {
        factory.registerBeanDefinition(new BeanDefinition("proto", SimpleBean.class, Scope.PROTOTYPE));

        SimpleBean first = factory.getBean(SimpleBean.class);
        SimpleBean second = factory.getBean(SimpleBean.class);

        assertThat(first).isNotSameAs(second);
    }

    @Test
    void shouldThrowBeanNotFoundExceptionWhenNoTypeMatches() {
        assertThatThrownBy(() -> factory.getBean(SimpleBean.class))
                .isInstanceOf(BeanNotFoundException.class)
                .hasMessageContaining("SimpleBean");
    }

    @Test
    void shouldThrowWhenSingletonHasNotYetBeenInstantiatedByClass() {
        factory.registerBeanDefinition(new BeanDefinition("simple", SimpleBean.class, Scope.SINGLETON));

        assertThatThrownBy(() -> factory.getBean(SimpleBean.class))
                .isInstanceOf(BeansException.class)
                .hasMessageContaining("not been instantiated");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenClassIsNull() {
        assertThatThrownBy(() -> factory.getBean((Class<Object>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnSingletonByName() {
        factory.registerBeanDefinition(new BeanDefinition("simple", SimpleBean.class, Scope.SINGLETON));
        factory.instantiateSingletons();

        SimpleBean bean = factory.getBean("simple");

        assertThat(bean).isNotNull();
    }

    @Test
    void shouldThrowBeanNotFoundExceptionWhenNameIsUnknown() {
        assertThatThrownBy(() -> factory.getBean("unknown"))
                .isInstanceOf(BeanNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void shouldCreateFreshPrototypeOnEachCallByName() {
        factory.registerBeanDefinition(new BeanDefinition("proto", SimpleBean.class, Scope.PROTOTYPE));

        SimpleBean first = factory.getBean("proto");
        SimpleBean second = factory.getBean("proto");

        assertThat(first).isNotSameAs(second);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> factory.getBean((String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static class SimpleBean {
    }

    static class Dependency {
    }

    static class BeanWithAutowiredField {
        @Autowired
        private Dependency dependency;

        public Dependency getDependency() {
            return dependency;
        }
    }

    interface DemoInterface {
    }

    static class DemoInterfaceImpl implements DemoInterface {
    }

    static class BeanRequiringInterface {
        @Autowired
        private DemoInterface demoInterface;

        public DemoInterface getDemoInterface() {
            return demoInterface;
        }
    }

    static class BeanCheckingDepAtPostConstruct {
        @Autowired
        private Dependency dependency;

        @PostConstruct
        private void verifyDepInjected() {
            LifecycleRecorder.dependencyVisibleAtPostConstruct = (dependency != null);
            LifecycleRecorder.postConstructEvents.add("BeanCheckingDepAtPostConstruct");
        }
    }

    static class OrderedBeanA {
        @PreDestroy
        private void onDestroy() {
            LifecycleRecorder.preDestroyEvents.add("A");
        }
    }

    static class OrderedBeanB {
        @PreDestroy
        private void onDestroy() {
            LifecycleRecorder.preDestroyEvents.add("B");
        }
    }

    static class OrderedBeanC {
        @PreDestroy
        private void onDestroy() {
            LifecycleRecorder.preDestroyEvents.add("C");
        }
    }

    static class PrototypeBeanWithPreDestroy {
        @PreDestroy
        private void onDestroy() {
            LifecycleRecorder.preDestroyEvents.add("Prototype");
        }
    }

    static class BeanWithoutNoArgCtor {
        BeanWithoutNoArgCtor(String value) {
        }
    }

    static class BeanWhosePostConstructThrows {
        @PostConstruct
        private void boom() {
            throw new IllegalStateException("boom");
        }
    }
}
