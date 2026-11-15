package net.stackanvil.deep.ui;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.stackanvil.deep.account.Account;
import net.stackanvil.deep.account.AccountManager;
import net.stackanvil.deep.account.AccountStore;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * Account manager. Microsoft sessions stay encrypted until the player enters the account password.
 */
public final class AccountsScreen extends Screen {

    static final int ROWS = 6;
    private static final int WIDTH = 300;
    private static final int ROW_HEIGHT = 22;

    private final @Nullable Screen parent;
    private final AccountManager accounts;
    private Component status = Component.empty();
    private int page;
    private boolean busy;
    private @Nullable Runnable afterUnlock;
    private @Nullable EditBox offlineName;
    private @Nullable EditBox password;

    public AccountsScreen(final @Nullable Screen parent, final AccountManager accounts) {
        super(Component.translatable("screen.deep.accounts"));
        this.parent = parent;
        this.accounts = accounts;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - WIDTH / 2;
        this.addRenderableWidget(new StringWidget(left, 12, WIDTH, 12, this.title, this.font));
        this.addRenderableWidget(new StringWidget(left, 26, WIDTH, 12,
                Component.translatable("screen.deep.accounts.current", this.minecraft.getUser().getName()), this.font));
        if (this.afterUnlock != null) {
            this.initPasswordPrompt(left);
        } else {
            this.initAccountList(left);
        }
        this.addRenderableWidget(new StringWidget(left, this.height - 24, WIDTH, 12, this.status, this.font));
    }

    private void initAccountList(final int left) {
        AccountStore store = this.accounts.store();
        List<Account> all = store.accounts();
        int pages = Math.max(1, (all.size() + ROWS - 1) / ROWS);
        this.page = Math.max(0, Math.min(this.page, pages - 1));
        int top = 44;
        if (all.isEmpty()) {
            this.addRenderableWidget(new StringWidget(left, top + 6, WIDTH, 12,
                    Component.translatable("screen.deep.accounts.empty"), this.font));
        }
        List<Account> visible = all.subList(this.page * ROWS, Math.min(all.size(), this.page * ROWS + ROWS));
        for (int index = 0; index < visible.size(); index++) {
            Account account = visible.get(index);
            int y = top + index * ROW_HEIGHT;
            Button login = this.addRenderableWidget(Button.builder(label(account), button -> this.login(account))
                    .bounds(left, y, WIDTH - 24, 20)
                    .build());
            login.active = !this.busy;
            Button remove = this.addRenderableWidget(Button.builder(Component.literal("X"), button -> this.remove(account))
                    .bounds(left + WIDTH - 20, y, 20, 20)
                    .build());
            remove.active = !this.busy;
        }

        int footer = top + ROWS * ROW_HEIGHT + 4;
        Button previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.page--;
            this.rebuildWidgets();
        }).bounds(left, footer, 20, 20).build());
        previous.active = this.page > 0;
        Button next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            this.page++;
            this.rebuildWidgets();
        }).bounds(left + 24, footer, 20, 20).build());
        next.active = this.page < pages - 1;
        this.offlineName = new EditBox(this.font, left + 48, footer, 156, 20, this.offlineName,
                Component.translatable("screen.deep.accounts.offline_name"));
        this.offlineName.setHint(Component.translatable("screen.deep.accounts.offline_name"));
        this.offlineName.setMaxLength(16);
        this.addRenderableWidget(this.offlineName);
        Button addOffline = this.addRenderableWidget(Button.builder(Component.translatable("screen.deep.accounts.add_offline"),
                        button -> this.addOffline())
                .bounds(left + 208, footer, 92, 20)
                .build());
        addOffline.active = !this.busy;

        int actions = footer + 24;
        Button addMicrosoft = this.addRenderableWidget(Button.builder(Component.translatable("screen.deep.accounts.add_microsoft"),
                        button -> this.requireUnlock(this::addMicrosoft))
                .bounds(left, actions, 148, 20)
                .build());
        addMicrosoft.active = !this.busy;
        Button lock = this.addRenderableWidget(Button.builder(Component.translatable("screen.deep.accounts.lock"), button -> {
            store.lock();
            this.setStatus(Component.translatable("screen.deep.accounts.locked"));
        }).bounds(left + 152, actions, 72, 20).build());
        lock.active = store.unlocked();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(left + 228, actions, 72, 20)
                .build());
    }

    private void initPasswordPrompt(final int left) {
        boolean firstUse = !this.accounts.store().hasPassword();
        int top = 60;
        this.addRenderableWidget(new StringWidget(left, top, WIDTH, 12, Component.translatable(firstUse
                ? "screen.deep.accounts.password_new"
                : "screen.deep.accounts.password_enter"), this.font));
        this.password = new EditBox(this.font, left, top + 16, WIDTH, 20, Component.translatable("screen.deep.accounts.password"));
        this.password.setMaxLength(128);
        this.password.addFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
        this.addRenderableWidget(this.password);
        this.setInitialFocus(this.password);
        this.addRenderableWidget(Button.builder(Component.translatable("screen.deep.accounts.unlock"), button -> this.unlock())
                .bounds(left, top + 40, 148, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
            this.afterUnlock = null;
            this.rebuildWidgets();
        }).bounds(left + 152, top + 40, 148, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private void requireUnlock(final Runnable action) {
        if (this.accounts.store().unlocked()) {
            action.run();
            return;
        }
        this.afterUnlock = action;
        this.rebuildWidgets();
    }

    private void unlock() {
        if (this.password == null || this.afterUnlock == null) {
            return;
        }
        char[] chars = this.password.getValue().toCharArray();
        try {
            this.accounts.store().unlock(chars);
        } catch (AccountStore.WrongPasswordException exception) {
            this.setStatus(Component.literal(exception.getMessage()));
            return;
        } catch (GeneralSecurityException | IOException exception) {
            this.setStatus(Component.translatable("screen.deep.accounts.error", exception.getMessage()));
            return;
        } finally {
            Arrays.fill(chars, '\0');
            this.password.setValue("");
        }
        Runnable action = this.afterUnlock;
        this.afterUnlock = null;
        this.rebuildWidgets();
        action.run();
    }

    private void login(final Account account) {
        if (account.type() == Account.Type.MICROSOFT && !this.accounts.store().unlocked()) {
            this.requireUnlock(() -> this.login(account));
            return;
        }
        this.setBusy(Component.translatable("screen.deep.accounts.logging_in", account.name()));
        this.accounts.login(this.minecraft, account).whenComplete((updated, error) -> this.minecraft.execute(() -> {
            this.busy = false;
            this.setStatus(error == null
                    ? Component.translatable("screen.deep.accounts.logged_in", updated.name())
                    : Component.literal(cause(error)));
        }));
    }

    private void addMicrosoft() {
        this.setBusy(Component.translatable("screen.deep.accounts.requesting_code"));
        this.accounts.addMicrosoft(code -> this.minecraft.execute(() -> this.showDeviceCode(code)))
                .whenComplete((account, error) -> this.minecraft.execute(() -> {
                    this.busy = false;
                    this.setStatus(error == null
                            ? Component.translatable("screen.deep.accounts.added", account.name())
                            : Component.literal(cause(error)));
                }));
    }

    private void showDeviceCode(final MsaDeviceCode code) {
        this.minecraft.keyboardHandler.setClipboard(code.getUserCode());
        Blaze3D.openUri(URI.create(code.getDirectVerificationUri()));
        this.setStatus(Component.translatable("screen.deep.accounts.device_code", code.getVerificationUri(), code.getUserCode()));
    }

    private void addOffline() {
        if (this.offlineName == null) {
            return;
        }
        try {
            Account account = this.accounts.store().addOffline(this.offlineName.getValue());
            this.offlineName.setValue("");
            this.setStatus(Component.translatable("screen.deep.accounts.added", account.name()));
        } catch (IllegalArgumentException | IOException exception) {
            this.setStatus(Component.literal(exception.getMessage()));
        }
    }

    private void remove(final Account account) {
        try {
            this.accounts.store().remove(account);
            this.setStatus(Component.translatable("screen.deep.accounts.removed", account.name()));
        } catch (IOException exception) {
            this.setStatus(Component.translatable("screen.deep.accounts.error", exception.getMessage()));
        }
    }

    private void setBusy(final Component message) {
        this.busy = true;
        this.setStatus(message);
    }

    private void setStatus(final Component message) {
        this.status = message;
        if (this.minecraft.gui.screen() == this) {
            this.rebuildWidgets();
        }
    }

    static Component label(final Account account) {
        return Component.translatable(account.type() == Account.Type.MICROSOFT
                ? "screen.deep.accounts.microsoft_entry"
                : "screen.deep.accounts.offline_entry", account.name());
    }

    private static String cause(final Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
