package net.stackanvil.deep.account;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountStoreTest {

    private static final UUID PLAYER = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final String SECRET_TOKEN = "eyJ-very-secret-refresh-token";

    @TempDir
    private Path tempDir;

    @Test
    void firstUnlockSetsThePasswordAndLaterUnlocksCheckIt() throws Exception {
        AccountStore store = this.store();
        store.load();
        assertFalse(store.hasPassword());

        store.unlock("hunter2".toCharArray());

        AccountStore reloaded = this.store();
        reloaded.load();
        assertTrue(reloaded.hasPassword());
        assertFalse(reloaded.unlocked());
        assertThrows(AccountStore.WrongPasswordException.class, () -> reloaded.unlock("wrong".toCharArray()));
        assertFalse(reloaded.unlocked());
        reloaded.unlock("hunter2".toCharArray());
        assertTrue(reloaded.unlocked());
    }

    @Test
    void microsoftSessionsAreEncryptedAndRoundTrip() throws Exception {
        AccountStore store = this.store();
        store.load();
        store.unlock("hunter2".toCharArray());

        store.addMicrosoft("Notch", PLAYER, session());

        String file = Files.readString(this.tempDir.resolve("accounts.json"));
        assertFalse(file.contains(SECRET_TOKEN), "token must not be stored in plain text");
        AccountStore reloaded = this.store();
        reloaded.load();
        Account account = reloaded.accounts().getFirst();
        assertEquals("Notch", account.name());
        assertEquals(PLAYER, account.uuid());
        assertThrows(IllegalStateException.class, () -> reloaded.session(account));
        reloaded.unlock("hunter2".toCharArray());
        assertEquals(session(), reloaded.session(account));
    }

    @Test
    void loggingInAgainReplacesTheExistingAccount() throws Exception {
        AccountStore store = this.store();
        store.load();
        store.unlock("pw".toCharArray());

        Account first = store.addMicrosoft("OldName", PLAYER, session());
        Account second = store.addMicrosoft("NewName", PLAYER, session());

        assertEquals(1, store.accounts().size());
        assertEquals(first.id(), second.id());
        assertEquals("NewName", store.accounts().getFirst().name());
    }

    @Test
    void updateSessionStoresRefreshedTokensAndName() throws Exception {
        AccountStore store = this.store();
        store.load();
        store.unlock("pw".toCharArray());
        Account account = store.addMicrosoft("Notch", PLAYER, session());
        JsonObject refreshed = new JsonObject();
        refreshed.addProperty("refresh", "new");

        Account updated = store.updateSession(account, "Notch2", PLAYER, refreshed);

        assertEquals("Notch2", updated.name());
        assertEquals(refreshed, store.session(updated));
    }

    @Test
    void offlineAccountsNeedNoPasswordAndUseVanillaOfflineUuids() throws Exception {
        AccountStore store = this.store();
        store.load();

        Account steve = store.addOffline(" Steve ");

        assertEquals("Steve", steve.name());
        assertEquals(UUID.fromString("5627dd98-e6be-3c21-b8a8-e92344183641"), steve.uuid());
        assertNull(steve.encryptedSession());
        assertFalse(store.hasPassword());
        assertThrows(IllegalArgumentException.class, () -> store.addOffline("bad name!"));
        assertThrows(IllegalArgumentException.class, () -> store.addOffline("x".repeat(17)));
    }

    @Test
    void removeDeletesAnAccount() throws Exception {
        AccountStore store = this.store();
        store.load();
        Account steve = store.addOffline("Steve");
        Account alex = store.addOffline("Alex");

        store.remove(steve);

        AccountStore reloaded = this.store();
        reloaded.load();
        assertEquals(List.of(alex), reloaded.accounts());
    }

    private AccountStore store() {
        return new AccountStore(this.tempDir.resolve("accounts.json"), new SecureRandom(), 1_000);
    }

    private static JsonObject session() {
        JsonObject session = new JsonObject();
        session.addProperty("refresh_token", SECRET_TOKEN);
        return session;
    }
}
