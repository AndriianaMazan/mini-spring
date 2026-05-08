package io.github.amazan.springframework.context;

import io.github.amazan.springframework.fixtures.LifecycleRecorder;
import io.github.amazan.springframework.scanme1.HealthyBean1;
import io.github.amazan.springframework.scanme1.HealthyBean2;
import io.github.amazan.springframework.scanme2.SiblingBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultApplicationContextTest {

    private DefaultApplicationContext context;
    private Thread runner;

    @BeforeEach
    void setUp() {
        LifecycleRecorder.reset();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (context != null) {
            context.close();
        }
        if (runner != null) {
            runner.join(2000);
        }
    }

    @Test
    void shouldScanRegisterAndInstantiateAllComponentsInPackage() throws InterruptedException {
        context = new DefaultApplicationContext(HealthyBean1.class);
        startRunnerAndWaitUntilBlocked();

        HealthyBean1 bean1 = context.getBeanFactory().getBean(HealthyBean1.class);
        HealthyBean2 bean2 = context.getBeanFactory().getBean(HealthyBean2.class);

        assertThat(bean1).isNotNull();
        assertThat(bean2).isNotNull();
        assertThat(bean2.getHealthyBean1()).isSameAs(bean1);
    }

    @Test
    void shouldUnblockRunWhenCloseIsCalled() throws InterruptedException {
        context = new DefaultApplicationContext(HealthyBean1.class);
        startRunnerAndWaitUntilBlocked();

        context.close();
        runner.join(2000);

        assertThat(runner.isAlive()).isFalse();
    }

    @Test
    void shouldInvokePreDestroyOnCloseAfterSuccessfulRun() throws InterruptedException {
        context = new DefaultApplicationContext(HealthyBean1.class);
        startRunnerAndWaitUntilBlocked();

        context.close();
        runner.join(2000);

        assertThat(LifecycleRecorder.preDestroyEvents).contains("HealthyBean1");
    }

    @Test
    void shouldBeIdempotentWhenCloseCalledTwice() throws InterruptedException {
        context = new DefaultApplicationContext(HealthyBean1.class);
        startRunnerAndWaitUntilBlocked();

        context.close();
        context.close();
        runner.join(2000);

        assertThat(LifecycleRecorder.preDestroyEvents)
                .filteredOn(event -> event.equals("HealthyBean1"))
                .hasSize(1);
    }

    @Test
    void shouldCleanUpPartiallyCreatedBeansWhenPostConstructFails() throws InterruptedException {
        context = new DefaultApplicationContext(SiblingBean.class);
        runner = new Thread(context::run);
        runner.setUncaughtExceptionHandler((t, e) -> { /* swallow expected failure */ });
        runner.start();
        runner.join(2000);

        assertThat(runner.isAlive()).isFalse();
        assertThat(LifecycleRecorder.preDestroyEvents).contains("SiblingBean");
    }

    private void startRunnerAndWaitUntilBlocked() throws InterruptedException {
        runner = new Thread(context::run);
        runner.start();
        waitForState(runner, Thread.State.WAITING, 2000);
    }

    private static void waitForState(Thread thread, Thread.State target, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (thread.getState() != target && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        if (thread.getState() != target) {
            throw new AssertionError("Thread did not reach state " + target + " within " + timeoutMillis + "ms (was " + thread.getState() + ")");
        }
    }
}
