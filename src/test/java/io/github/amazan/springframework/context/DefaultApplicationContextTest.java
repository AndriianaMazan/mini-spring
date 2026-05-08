package io.github.amazan.springframework.context;

import io.github.amazan.springframework.fixtures.LifecycleRecorder;
import io.github.amazan.springframework.scanme1.HealthyBean1;
import io.github.amazan.springframework.scanme1.HealthyBean2;
import io.github.amazan.springframework.scanme2.SiblingBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultApplicationContextTest {

    private DefaultApplicationContext context;

    @BeforeEach
    void setUp() {
        LifecycleRecorder.reset();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void shouldScanRegisterAndInstantiateAllComponentsInPackage() {
        context = new DefaultApplicationContext(HealthyBean1.class);
        context.run();

        HealthyBean1 bean1 = context.getBeanFactory().getBean(HealthyBean1.class);
        HealthyBean2 bean2 = context.getBeanFactory().getBean(HealthyBean2.class);

        assertThat(bean1).isNotNull();
        assertThat(bean2).isNotNull();
        assertThat(bean2.getHealthyBean1()).isSameAs(bean1);
    }

    @Test
    void shouldInvokePreDestroyOnCloseAfterSuccessfulRun() {
        context = new DefaultApplicationContext(HealthyBean1.class);
        context.run();

        context.close();

        assertThat(LifecycleRecorder.preDestroyEvents).contains("HealthyBean1");
    }

    @Test
    void shouldBeIdempotentWhenCloseCalledTwice() {
        context = new DefaultApplicationContext(HealthyBean1.class);
        context.run();

        context.close();
        context.close();

        assertThat(LifecycleRecorder.preDestroyEvents)
                .filteredOn(event -> event.equals("HealthyBean1"))
                .hasSize(1);
    }

    @Test
    void shouldCleanUpPartiallyCreatedBeansWhenPostConstructFails() {
        context = new DefaultApplicationContext(SiblingBean.class);

        assertThatThrownBy(() -> context.run()).isInstanceOf(RuntimeException.class);

        assertThat(LifecycleRecorder.preDestroyEvents).contains("SiblingBean");
    }
}
