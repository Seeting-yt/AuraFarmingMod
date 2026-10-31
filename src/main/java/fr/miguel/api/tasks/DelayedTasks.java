package fr.miguel.api.tasks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public final class DelayedTasks {

    private record Task(long runAt, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTickCount();

            List<Task> due = TASKS.stream().filter(t -> t.runAt() <= now).toList();

            TASKS.removeAll(due);
            due.forEach(task -> task.run().run());
        });
    }

    public static void schedule(MinecraftServer server, int ticks, Runnable runnable) {
        TASKS.add(new Task(server.getTickCount() + ticks, runnable));
    }
}
