package io.github.amazan.springframework.scanme2;

import io.github.amazan.springframework.annotation.Component;
import io.github.amazan.springframework.annotation.PreDestroy;
import io.github.amazan.springframework.fixtures.LifecycleRecorder;

@Component
public class SiblingBean {

    @PreDestroy
    private void onDestroy() {
        LifecycleRecorder.preDestroyEvents.add("SiblingBean");
    }
}
