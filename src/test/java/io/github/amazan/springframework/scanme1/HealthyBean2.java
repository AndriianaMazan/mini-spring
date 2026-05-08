package io.github.amazan.springframework.scanme1;

import io.github.amazan.springframework.annotation.Autowired;
import io.github.amazan.springframework.annotation.Component;

@Component
public class HealthyBean2 {

    @Autowired
    private HealthyBean1 healthyBean1;

    public HealthyBean1 getHealthyBean1() {
        return healthyBean1;
    }
}
