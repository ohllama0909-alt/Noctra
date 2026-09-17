package de.snenjih.noctra.auth;

import com.google.gson.JsonObject;
import de.snenjih.noctra.hud.NotificationManager;
import de.snenjih.noctra.mixin.accessor.MinecraftClientSessionAccessor;
import net.lenni0451.commons.httpclient.HttpClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class AccountManager {

    private static AccountManager INSTANCE;

    private final AccountStorage storage;
    private final SkinCache      skinCache;

    private volatile Thread authThread;

    public interface DeviceCodeHandler {
        void onCode(String userCode, String verificationUri, int expiresInSeconds);
    }

    private AccountManager(AccountStorage storage, SkinCache skinCache) {
        this.storage   = storage;
        this.skinCache = skinCache;
    }

    public static void init(AccountStorage storage, SkinCache skinCache) {
        INSTANCE = new AccountManager(storage, skinCache);
    }

    public static AccountManager getInstance() { return INSTANCE; }

    // ---- Queries ------------------------------------------------------------

    public List<AccountEntry> getAll()    { return storage.getAll(); }
    public AccountEntry       getActive() { return storage.getActive(); }

    // ---- Add account via device-code flow -----------------------------------

    public boolean isAuthRunning() {
        return authThread != null && authThread.isAlive();
    }

    public void startAuth(DeviceCodeHandler codeHandler,
                          Consumer<AccountEntry> onSuccess,
                          Consumer<String> onError) {
        if (isAuthRunning()) return;

        authThread = new Thread(() -> {
            try {
                HttpClient httpClient = MinecraftAuth.createHttpClient();

                StepFullJavaSession.FullJavaSession session =
                        MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(
                                httpClient,
                                new StepMsaDeviceCode.MsaDeviceCodeCallback(code -> {
                                    // Convert expire epoch millis to remaining seconds
                                    int remaining = (int) Math.max(0,
                                            (code.getExpireTimeMs() - System.currentTimeMillis()) / 1000L);
                                    MinecraftClient.getInstance().execute(() ->
                                            codeHandler.onCode(
                                                    code.getUserCode(),
                                                    code.getVerificationUri(),
                                                    remaining
                                            ));
                                })
                        );

                String     username    = session.getMcProfile().getName();
                UUID       uuid        = session.getMcProfile().getId();
                String     skinUrl     = session.getMcProfile().getSkinUrl();
                JsonObject sessionJson = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session);

                AccountEntry entry = new AccountEntry(uuid, username, skinUrl, sessionJson);
                storage.addOrReplace(entry);
                storage.save();
                skinCache.ensureLoaded(uuid, skinUrl);

                MinecraftClient.getInstance().execute(() -> onSuccess.accept(entry));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                MinecraftClient.getInstance().execute(() -> onError.accept("cancelled"));
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    MinecraftClient.getInstance().execute(() -> onError.accept("cancelled"));
                } else {
                    MinecraftClient.getInstance().execute(() -> onError.accept(buildErrorMessage(e)));
                }
            }
        }, "NoctraMod-Auth");
        authThread.setDaemon(true);
        authThread.start();
    }

    public void cancelAuth() {
        if (authThread != null) authThread.interrupt();
    }

    // ---- Switch account -----------------------------------------------------

    public void switchToAsync(AccountEntry entry, Runnable onSuccess, Consumer<String> onError) {
        Thread t = new Thread(() -> {
            try {
                if (entry.sessionJson() == null) {
                    MinecraftClient.getInstance().execute(() -> onError.accept("session_missing"));
                    return;
                }

                HttpClient httpClient = MinecraftAuth.createHttpClient();

                StepFullJavaSession.FullJavaSession session =
                        MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(entry.sessionJson());

                // MinecraftAuth only re-fetches expired steps
                session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, session);

                // Persist refreshed session
                entry.setSessionJson(MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session));
                storage.save();

                String username    = session.getMcProfile().getName();
                UUID   uuid        = session.getMcProfile().getId();
                String accessToken = session.getMcProfile().getMcToken().getAccessToken();

                // Session in 1.21.11: no AccountType parameter
                Session mcSession = new Session(username, uuid, accessToken,
                        Optional.empty(), Optional.empty());

                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    ((MinecraftClientSessionAccessor) mc).setSession(mcSession);
                    storage.setActive(uuid);
                    storage.save();
                    NotificationManager.push("Switched to " + username, NotificationManager.Type.SUCCESS);
                    onSuccess.run();
                });

            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> onError.accept(buildErrorMessage(e)));
            }
        }, "NoctraMod-Switch");
        t.setDaemon(true);
        t.start();
    }

    // ---- Remove account -----------------------------------------------------

    public void removeAccount(UUID uuid) {
        skinCache.unload(uuid);
        storage.remove(uuid);
        storage.save();
    }

    // ---- Error message mapping -----------------------------------------------

    private static String buildErrorMessage(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("does not own") || msg.contains("not_allowed") || msg.contains("forbid")) {
            return "no_license";
        }
        if (msg.contains("expired") || msg.contains("code_expired")) {
            return "code_expired";
        }
        if (msg.contains("network") || e instanceof java.net.ConnectException
                || e instanceof java.net.UnknownHostException) {
            return "no_network";
        }
        return "unknown:" + e.getClass().getSimpleName();
    }
}
