package org.example;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ChatHistoryBuffer implements Listener {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Deque<String> buffer;
    private final int maxSize;

    public ChatHistoryBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new ArrayDeque<>(maxSize + 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String timestamp = TIME_FORMAT.format(ZonedDateTime.now());
        String line = "[" + timestamp + "] <" + playerName + "> " + message;
        addLine(line);
    }

    private synchronized void addLine(String line) {
        buffer.addLast(line);
        if (buffer.size() > maxSize) {
            buffer.removeFirst();
        }
    }

    /** Returns an immutable snapshot of current chat history, oldest first. */
    public synchronized List<String> snapshot() {
        return new ArrayList<>(buffer);
    }
}

