package net.pascalhp.webprobe;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventRouter {
    protected LinkedList<EventListener> listeners = new LinkedList<>();

    public void addEventListener(EventListener listener) {
        this.listeners.add(listener);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    public void pushEvent(Event event) {
        for (EventListener listener : this.listeners) {
            try {
                listener.onEvent(event);
            } catch (Throwable e) {
                Logger logger = Logger.getGlobal();
                logger.log(Level.WARNING, e.toString());
            }
        }
    }
}
