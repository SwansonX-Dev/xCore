package dev.xsuite.core.api.messenger;

public interface NetworkSubscription {

    @org.jetbrains.annotations.NotNull
    Class<? extends NetworkMessage> messageType();

    void unsubscribe();

    boolean isActive();
}
