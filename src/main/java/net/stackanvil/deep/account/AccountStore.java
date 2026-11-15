package net.stackanvil.deep.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.stackanvil.deep.Deep;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores accounts in accounts.json. Microsoft sessions are encrypted with a key derived from a password the player
 * chooses, so a stolen config folder (or client source) is not enough to use the accounts. Offline accounts hold no
 * secrets and are stored in plain text.
 */
public final class AccountStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 1;
    private static final byte[] CHECK_VALUE = "deep-accounts".getBytes(StandardCharsets.UTF_8);

    private final Path path;
    private final SecureRandom random;
    private final int iterations;
    private final List<Account> accounts = new ArrayList<>();
    private byte @Nullable [] salt;
    private @Nullable String check;
    private int storedIterations;
    private @Nullable SecretKey key;

    public AccountStore(final Path path, final SecureRandom random, final int iterations) {
        this.path = path;
        this.random = random;
        this.iterations = iterations;
    }

    public static AccountStore defaults() {
        return new AccountStore(FabricLoader.getInstance().getConfigDir().resolve(Deep.MOD_ID).resolve("accounts.json"),
                new SecureRandom(), AccountCrypto.DEFAULT_ITERATIONS);
    }

    public synchronized void load() throws IOException {
        this.accounts.clear();
        this.salt = null;
        this.check = null;
        this.key = null;
        if (!Files.exists(this.path)) {
            return;
        }
        JsonObject root = JsonParser.parseString(Files.readString(this.path)).getAsJsonObject();
        JsonObject kdf = root.getAsJsonObject("kdf");
        if (kdf != null) {
            this.salt = Base64.getDecoder().decode(kdf.get("salt").getAsString());
            this.storedIterations = kdf.get("iterations").getAsInt();
            this.check = kdf.get("check").getAsString();
        }
        JsonArray list = root.getAsJsonArray("accounts");
        if (list == null) {
            return;
        }
        for (JsonElement element : list) {
            JsonObject json = element.getAsJsonObject();
            JsonElement session = json.get("session");
            this.accounts.add(new Account(
                    json.get("id").getAsString(),
                    Account.Type.valueOf(json.get("type").getAsString().toUpperCase(Locale.ROOT)),
                    json.get("name").getAsString(),
                    UUID.fromString(json.get("uuid").getAsString()),
                    session == null || session.isJsonNull() ? null : session.getAsString()
            ));
        }
    }

    public synchronized void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        if (this.salt != null && this.check != null) {
            JsonObject kdf = new JsonObject();
            kdf.addProperty("algorithm", "PBKDF2WithHmacSHA256");
            kdf.addProperty("iterations", this.storedIterations);
            kdf.addProperty("salt", Base64.getEncoder().encodeToString(this.salt));
            kdf.addProperty("check", this.check);
            root.add("kdf", kdf);
        }
        JsonArray list = new JsonArray();
        for (Account account : this.accounts) {
            JsonObject json = new JsonObject();
            json.addProperty("id", account.id());
            json.addProperty("type", account.type().name().toLowerCase(Locale.ROOT));
            json.addProperty("name", account.name());
            json.addProperty("uuid", account.uuid().toString());
            if (account.encryptedSession() != null) {
                json.addProperty("session", account.encryptedSession());
            }
            list.add(json);
        }
        root.add("accounts", list);
        Files.createDirectories(this.path.getParent());
        Path temp = this.path.resolveSibling(this.path.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(root));
        Files.move(temp, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public synchronized List<Account> accounts() {
        return List.copyOf(this.accounts);
    }

    /**
     * @return {@code true} when a password was set before, so unlocking checks it instead of creating it
     */
    public synchronized boolean hasPassword() {
        return this.salt != null;
    }

    public synchronized boolean unlocked() {
        return this.key != null;
    }

    /**
     * Unlocks the store. On first use this sets the password.
     *
     * @throws WrongPasswordException when the password does not match the one set before
     */
    public synchronized void unlock(final char[] password) throws GeneralSecurityException, IOException {
        if (password.length == 0) {
            throw new WrongPasswordException("Password must not be empty");
        }
        if (this.salt == null) {
            byte[] newSalt = AccountCrypto.newSalt(this.random);
            SecretKey newKey = AccountCrypto.deriveKey(password, newSalt, this.iterations);
            this.salt = newSalt;
            this.storedIterations = this.iterations;
            this.check = AccountCrypto.encrypt(newKey, CHECK_VALUE, this.random);
            this.key = newKey;
            this.save();
            return;
        }
        SecretKey candidate = AccountCrypto.deriveKey(password, this.salt, this.storedIterations);
        try {
            AccountCrypto.decrypt(candidate, this.check);
        } catch (GeneralSecurityException exception) {
            throw new WrongPasswordException("Wrong password");
        }
        this.key = candidate;
    }

    public synchronized void lock() {
        this.key = null;
    }

    public synchronized Account addOffline(final String name) throws IOException {
        String trimmed = name.trim();
        if (!validOfflineName(trimmed)) {
            throw new IllegalArgumentException("Offline names use 1 to 16 letters, digits, or underscores");
        }
        Account account = new Account(UUID.randomUUID().toString(), Account.Type.OFFLINE, trimmed, offlineUuid(trimmed), null);
        this.accounts.add(account);
        this.save();
        return account;
    }

    public synchronized Account addMicrosoft(final String name, final UUID uuid, final JsonObject session)
            throws GeneralSecurityException, IOException {
        Optional<Account> existing = this.accounts.stream()
                .filter(account -> account.type() == Account.Type.MICROSOFT && account.uuid().equals(uuid))
                .findFirst();
        String encrypted = this.encrypt(session);
        Account account = existing
                .map(old -> old.withSession(name, uuid, encrypted))
                .orElseGet(() -> new Account(UUID.randomUUID().toString(), Account.Type.MICROSOFT, name, uuid, encrypted));
        existing.ifPresentOrElse(old -> this.accounts.set(this.accounts.indexOf(old), account), () -> this.accounts.add(account));
        this.save();
        return account;
    }

    public synchronized JsonObject session(final Account account) throws GeneralSecurityException {
        if (account.encryptedSession() == null) {
            throw new IllegalArgumentException(account.name() + " has no stored session");
        }
        byte[] plain = AccountCrypto.decrypt(this.requireKey(), account.encryptedSession());
        return JsonParser.parseString(new String(plain, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /**
     * Stores refreshed tokens and the current name for an account.
     */
    public synchronized Account updateSession(final Account account, final String name, final UUID uuid, final JsonObject session)
            throws GeneralSecurityException, IOException {
        int index = this.indexOf(account);
        Account updated = this.accounts.get(index).withSession(name, uuid, this.encrypt(session));
        this.accounts.set(index, updated);
        this.save();
        return updated;
    }

    public synchronized void remove(final Account account) throws IOException {
        this.accounts.remove(this.indexOf(account));
        this.save();
    }

    static boolean validOfflineName(final String name) {
        return name.matches("[A-Za-z0-9_]{1,16}");
    }

    static UUID offlineUuid(final String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private String encrypt(final JsonObject session) throws GeneralSecurityException {
        return AccountCrypto.encrypt(this.requireKey(), GSON.toJson(session).getBytes(StandardCharsets.UTF_8), this.random);
    }

    private SecretKey requireKey() {
        if (this.key == null) {
            throw new IllegalStateException("Account store is locked");
        }
        return this.key;
    }

    private int indexOf(final Account account) {
        for (int index = 0; index < this.accounts.size(); index++) {
            if (this.accounts.get(index).id().equals(account.id())) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown account: " + account.name());
    }

    public static final class WrongPasswordException extends GeneralSecurityException {

        public WrongPasswordException(final String message) {
            super(message);
        }
    }
}
