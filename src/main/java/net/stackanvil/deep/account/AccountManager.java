package net.stackanvil.deep.account;

import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import net.lenni0451.commons.httpclient.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.stackanvil.deep.mixin.MinecraftAccessor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Logs in to stored accounts and swaps the game's session. Network work runs on a background thread; the session
 * swap itself runs on the client thread.
 */
public final class AccountManager {

    private static final String OFFLINE_TOKEN = "0";

    private final AccountStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Deep account login");
        thread.setDaemon(true);
        return thread;
    });
    private HttpClient httpClient;

    public AccountManager(final AccountStore store) {
        this.store = store;
    }

    public AccountStore store() {
        return this.store;
    }

    /**
     * Starts a Microsoft device code login and stores the account when it finishes.
     *
     * @param deviceCode receives the code and link to show to the player
     */
    public CompletableFuture<Account> addMicrosoft(final Consumer<MsaDeviceCode> deviceCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JavaAuthManager auth = JavaAuthManager.create(this.httpClient())
                        .login(DeviceCodeMsaAuthService::new, deviceCode);
                MinecraftProfile profile = auth.getMinecraftProfile().getUpToDate();
                return this.store.addMicrosoft(profile.getName(), profile.getId(), JavaAuthManager.toJson(auth));
            } catch (Exception exception) {
                throw new AccountException("Microsoft login failed: " + message(exception), exception);
            }
        }, this.executor);
    }

    /**
     * Refreshes the account's tokens if needed and switches the game to it.
     */
    public CompletableFuture<Account> login(final Minecraft client, final Account account) {
        return CompletableFuture.supplyAsync(() -> {
            if (account.type() == Account.Type.OFFLINE) {
                Session session = new Session(account.name(), account.uuid(), OFFLINE_TOKEN, UserApiService.OFFLINE);
                client.execute(() -> apply(client, session));
                return account;
            }
            try {
                JsonObject stored = this.store.session(account);
                JavaAuthManager auth = JavaAuthManager.fromJson(this.httpClient(), stored);
                MinecraftToken token = auth.getMinecraftToken().getUpToDate();
                MinecraftProfile profile = auth.getMinecraftProfile().getUpToDate();
                Account updated = this.store.updateSession(account, profile.getName(), profile.getId(), JavaAuthManager.toJson(auth));
                UserApiService userApi = MinecraftServicesDiscoveryService.create(client.getProxy(), true)
                        .createUserApiService(token.getToken());
                Session session = new Session(profile.getName(), profile.getId(), token.getToken(), userApi);
                client.execute(() -> apply(client, session));
                return updated;
            } catch (Exception exception) {
                throw new AccountException("Login failed for " + account.name() + ": " + message(exception), exception);
            }
        }, this.executor);
    }

    private static void apply(final Minecraft client, final Session session) {
        User user = new User(session.name(), session.uuid(), session.accessToken(), Optional.empty(), Optional.empty());
        MinecraftAccessor accessor = (MinecraftAccessor) client;
        accessor.deep$setUser(user);
        accessor.deep$setUserApiService(session.userApi());
        accessor.deep$setProfileKeyPairManager(session.userApi() == UserApiService.OFFLINE
                ? ProfileKeyPairManager.EMPTY_KEY_MANAGER
                : ProfileKeyPairManager.create(session.userApi(), user, client.gameDirectory.toPath()));
        // getGameProfile() falls back to the user's name and UUID when no fetched profile is cached.
        accessor.deep$setProfileFuture(CompletableFuture.completedFuture(null));
        accessor.deep$setReportingContext(ReportingContext.create(ReportEnvironment.local(), session.userApi()));
    }

    private synchronized HttpClient httpClient() {
        if (this.httpClient == null) {
            this.httpClient = MinecraftAuth.createHttpClient();
        }
        return this.httpClient;
    }

    private static String message(final Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private record Session(String name, UUID uuid, String accessToken, UserApiService userApi) {
    }

    public static final class AccountException extends RuntimeException {

        public AccountException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
