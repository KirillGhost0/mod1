package ru.constructforspace;

/** Keep all Minecraft client types out of classes loaded by a dedicated server. */
public class CommonProxy {
    public void init() { }

    public void onDuplicateResult(int resultCode, int added, int requested) { }

    public void onGiveResult(int resultCode, int added, int requested) { }

    public void onRegenerationStatus(boolean enabled, int resultCode) { }
}
