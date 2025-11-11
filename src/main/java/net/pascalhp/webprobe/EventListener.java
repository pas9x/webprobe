package net.pascalhp.webprobe;

@FunctionalInterface
public interface EventListener {
    public void onEvent(Event event);
}
