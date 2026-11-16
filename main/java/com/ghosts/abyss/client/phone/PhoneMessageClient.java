package com.ghosts.abyss.client.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PhoneMessageClient {
    public record Message(String thread, String name, String body, boolean outgoing, boolean quest) {}
    public record Thread(String id, String name, boolean quest) {}
    private static final List<Message> MESSAGES = new ArrayList<>();
    private static boolean blocked = false;

    private PhoneMessageClient() {}
    public static boolean blocked() { return blocked; }
    public static void setBlocked(boolean value) { blocked = value; }

    public static void update(ListTag list) {
        MESSAGES.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            MESSAGES.add(new Message(t.getString("thread"), t.getString("name"),
                    t.getString("body"), t.getBoolean("outgoing"), t.getBoolean("quest")));
        }
    }

    public static List<Message> messages(String id) {
        return MESSAGES.stream().filter(m -> m.thread().equals(id)).toList();
    }

    public static List<Thread> threads() {
        Map<String, Thread> unique = new LinkedHashMap<>();
        for (int i = MESSAGES.size() - 1; i >= 0; i--) {
            Message m = MESSAGES.get(i);
            unique.putIfAbsent(m.thread(), new Thread(m.thread(), m.name(), m.quest()));
        }
        return new ArrayList<>(unique.values());
    }
}
