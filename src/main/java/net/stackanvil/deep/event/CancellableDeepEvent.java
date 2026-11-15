package net.stackanvil.deep.event;

import net.lenni0451.lambdaevents.types.ICancellableEvent;

public abstract class CancellableDeepEvent implements DeepEvent, ICancellableEvent {

    private boolean cancelled;

    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }
}
