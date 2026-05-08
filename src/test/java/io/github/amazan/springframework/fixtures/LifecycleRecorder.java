package io.github.amazan.springframework.fixtures;

import java.util.ArrayList;
import java.util.List;

public final class LifecycleRecorder {

    public static final List<String> postConstructEvents = new ArrayList<>();
    public static final List<String> preDestroyEvents = new ArrayList<>();
    public static boolean dependencyVisibleAtPostConstruct = false;

    private LifecycleRecorder() {
    }

    public static void reset() {
        postConstructEvents.clear();
        preDestroyEvents.clear();
        dependencyVisibleAtPostConstruct = false;
    }
}
