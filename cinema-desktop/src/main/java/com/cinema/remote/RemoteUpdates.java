package com.cinema.remote;

import javafx.application.Platform;
import javafx.scene.Node;
import java.lang.ref.WeakReference;
import java.net.http.*;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Singleton websocket with reconnect; visible screens refresh only when relevant topics change. */
public final class RemoteUpdates {
    private RemoteUpdates() {}
    private record Watch(WeakReference<Node> node, String topic, Runnable refresh) {}
    private static final CopyOnWriteArrayList<Watch> watchers = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t=new Thread(r,"cinema-live-updates");t.setDaemon(true);return t;
    });
    private static final HttpClient client=HttpClient.newBuilder().build();
    private static volatile WebSocket ws;
    private static volatile boolean enabled;
    private static volatile String token;
    private static volatile java.net.URI uri;
    private static volatile int attempts;
    private static final AtomicReference<ScheduledFuture<?>> retry = new AtomicReference<>();

    public static void start(CinemaApiClient api) {
        stop();
        token=api.sessionToken(); uri=api.updatesUri();
        if(token==null || token.isBlank()) return;
        enabled=true;attempts=0;connect();
    }
    public static void stop() {
        enabled=false;token=null;
        var old=ws;ws=null;
        if(old!=null) old.sendClose(WebSocket.NORMAL_CLOSURE,"logout");
        var pending=retry.getAndSet(null);if(pending!=null) pending.cancel(false);
        watchers.clear();
    }
    public static void watch(Node node,String topic,Runnable refresh) {
        if(!RemoteSession.enabled()) return;
        watchers.add(new Watch(new WeakReference<>(node),topic,refresh));
    }
    private static void connect() {
        if(!enabled) return;
        final String expectedToken=token;
        client.newWebSocketBuilder().buildAsync(uri,new WebSocket.Listener() {
            private final StringBuilder messages=new StringBuilder();
            @Override public void onOpen(WebSocket socket) {
                if(!enabled || !expectedToken.equals(token)) {socket.sendClose(WebSocket.NORMAL_CLOSURE,"inactive");return;}
                ws=socket;attempts=0;
                socket.sendText("AUTH " + expectedToken,true);
                socket.request(1);
            }
            @Override public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last) {
                messages.append(data);
                if(last) {
                    String msg=messages.toString();messages.setLength(0);
                    if(msg.startsWith("CHANGED ")) {
                        String[] parts=msg.split(" ",3);
                        if(parts.length>=2) notifyWatchers(parts[1]);
                    }
                }
                socket.request(1);
                return CompletableFuture.completedFuture(null);
            }
            @Override public CompletionStage<?> onClose(WebSocket socket,int code,String reason) {
                if(ws==socket) ws=null;
                if(enabled && expectedToken.equals(token) && code != WebSocket.NORMAL_CLOSURE && code != 1008) scheduleRetry();
                return CompletableFuture.completedFuture(null);
            }
            @Override public void onError(WebSocket socket,Throwable error) {
                if(ws==socket) ws=null;
                if(enabled && expectedToken.equals(token)) scheduleRetry();
            }
        }).exceptionally(ex->{ if(enabled && expectedToken.equals(token))scheduleRetry();return null; });
    }
    private static void scheduleRetry() {
        int seconds=Math.min(30,2 << Math.min(attempts++,4));
        var previous=retry.getAndSet(executor.schedule(RemoteUpdates::connect,seconds,TimeUnit.SECONDS));
        if(previous!=null)previous.cancel(false);
    }
    private static void notifyWatchers(String topic) {
        Platform.runLater(() -> {
            for(Watch watcher:watchers) {
                Node node=watcher.node().get();
                if(node==null) {watchers.remove(watcher);continue;}
                if(!watcher.topic().equals(topic)) continue;
                if(node.getScene()==null || node.getScene().getWindow()==null || !node.getScene().getWindow().isShowing()) continue;
                try {watcher.refresh().run();} catch(Exception ignored) {}
            }
        });
    }
}
