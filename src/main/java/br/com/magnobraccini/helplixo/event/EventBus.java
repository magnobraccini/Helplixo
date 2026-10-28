package br.com.magnobraccini.helplixo.event;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    public <T extends AppEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public <T extends AppEvent> void publish(T event) {
        Runnable notification = () -> listeners.getOrDefault(event.getClass(), List.of())
                .forEach(listener -> notifyListener(listener, event));
        if (SwingUtilities.isEventDispatchThread()) {
            notification.run();
        } else {
            SwingUtilities.invokeLater(notification);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends AppEvent> void notifyListener(Consumer<?> listener, T event) {
        ((Consumer<T>) listener).accept(event);
    }
}
