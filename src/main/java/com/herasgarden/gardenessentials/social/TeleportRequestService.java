package com.herasgarden.gardenessentials.social;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestService {
    private final long ttlMillis;
    private final Map<UUID, Request> incoming = new ConcurrentHashMap<>();

    public TeleportRequestService(long ttlSeconds) {
        this.ttlMillis = Math.max(10L, ttlSeconds) * 1000L;
    }

    public void request(UUID requester, UUID target) {
        incoming.put(target, new Request(requester, System.currentTimeMillis() + ttlMillis));
    }

    public Optional<UUID> take(UUID target) {
        Request request = incoming.remove(target);
        if (request == null || request.expiresAt < System.currentTimeMillis()) {
            return Optional.empty();
        }
        return Optional.of(request.requester);
    }

    public boolean deny(UUID target) {
        return take(target).isPresent();
    }

    public void clearFor(UUID playerId) {
        incoming.remove(playerId);
        incoming.entrySet().removeIf(entry -> entry.getValue().requester.equals(playerId));
    }

    private record Request(UUID requester, long expiresAt) {}
}
