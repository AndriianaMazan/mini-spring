package io.github.amazan.springframework.scanme2;

import io.github.amazan.springframework.annotation.Component;
import io.github.amazan.springframework.annotation.PostConstruct;

@Component
public class FailingScanBean {

    @PostConstruct
    private void boom() {
        throw new IllegalStateException("boom");
    }
}
