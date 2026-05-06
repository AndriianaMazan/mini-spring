package io.github.amazan.springframework.exception;

public class BeanNotFoundException extends BeansException {
    public BeanNotFoundException(String message) {
        super(message);
    }
}
