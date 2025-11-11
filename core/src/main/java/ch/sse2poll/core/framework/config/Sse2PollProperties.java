package ch.sse2poll.core.framework.config;

/**
 * Configuration properties for sse2poll. Simplified and framework-agnostic.
 */
public class Sse2PollProperties {
    private int waitMsDefault = 3000;
    private int maxWaitMs = 30000;
    private int statusOnTimeout = 202; // 202 or 204
    private final Cache cache = new Cache();

    public int getWaitMsDefault() { return waitMsDefault; }
    public void setWaitMsDefault(int v) { this.waitMsDefault = v; }

    public int getMaxWaitMs() { return maxWaitMs; }
    public void setMaxWaitMs(int v) { this.maxWaitMs = v; }

    public int getStatusOnTimeout() { return statusOnTimeout; }
    public void setStatusOnTimeout(int v) { this.statusOnTimeout = v; }

    public Cache getCache() { return cache; }

    public static class Cache {
        private String prefix = "s2p";

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }
}

