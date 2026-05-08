package io.github.amazan.springframework.scanme1;

import io.github.amazan.springframework.annotation.Component;
import io.github.amazan.springframework.annotation.PreDestroy;
import io.github.amazan.springframework.fixtures.LifecycleRecorder;

@Component
public class HealthyBean1 {

    @PreDestroy
    private void onDestroy() {
        LifecycleRecorder.preDestroyEvents.add("HealthyBean1");
    }
}
