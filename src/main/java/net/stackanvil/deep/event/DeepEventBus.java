package net.stackanvil.deep.event;

import net.lenni0451.lambdaevents.IEventFilter;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DeepEventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepEventBus.class);

    private final LambdaManager manager;

    public DeepEventBus() {
        this.manager = LambdaManager.basic(new ASMGenerator())
                .setEventFilter(DeepEventBus::allowEvent)
                .setExceptionHandler((handler, event, throwable) -> LOGGER.warn(
                        "Exception in Deep event handler {} for {}",
                        handler.getOwner().getName(),
                        event.getClass().getName(),
                        throwable
                ));
    }

    public void register(final Object listener) {
        this.manager.register(listener);
    }

    public void unregister(final Object listener) {
        this.manager.unregister(listener);
    }

    public <T extends DeepEvent> T call(final T event) {
        return this.manager.call(event);
    }

    private static boolean allowEvent(final Class<?> event, final IEventFilter.CheckType checkType) {
        return DeepEvent.class.isAssignableFrom(event);
    }
}
