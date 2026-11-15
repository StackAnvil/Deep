package net.stackanvil.deep.account;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A stored account.
 *
 * @param id               local identifier, stable across renames
 * @param type             account type
 * @param name             player name at the time of the last login
 * @param uuid             player UUID
 * @param encryptedSession encrypted MinecraftAuth session for Microsoft accounts, {@code null} for offline accounts
 */
public record Account(String id, Type type, String name, UUID uuid, @Nullable String encryptedSession) {

    public enum Type {
        MICROSOFT,
        OFFLINE
    }

    public Account withSession(final String name, final UUID uuid, final String encryptedSession) {
        return new Account(this.id, this.type, name, uuid, encryptedSession);
    }
}
