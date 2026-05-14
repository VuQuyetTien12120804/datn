package com.example.frontend_bookingcare.ui.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.frontend_bookingcare.api.RetrofitClient;
import com.example.frontend_bookingcare.ui.support.chat.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatStore {

    private static final String PREFS = "bookingcare_prefs";
    private static final String KEY_THREADS_META = "chat_threads_meta_json";
    private static final String KEY_MSG_PREFIX = "chat_msgs_";

    private final SharedPreferences prefs;
    private final Gson gson = RetrofitClient.gson();

    public ChatStore(Context ctx) {
        this.prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void upsertThread(ChatThreadMeta meta) {
        if (meta == null || TextUtils.isEmpty(meta.threadId)) return;
        Map<String, ChatThreadMeta> map = loadMetaMap();
        ChatThreadMeta old = map.get(meta.threadId);
        if (old != null) {
            // keep latest updatedAt / lastMessage unless new overrides
            if (meta.updatedAtMs <= 0) meta.updatedAtMs = old.updatedAtMs;
            if (TextUtils.isEmpty(meta.lastMessage)) meta.lastMessage = old.lastMessage;
            if (meta.unreadCount <= 0) meta.unreadCount = Math.max(0, old.unreadCount);
        }
        map.put(meta.threadId, meta);
        saveMetaMap(map);
    }

    public synchronized List<ChatThreadMeta> listThreadsSorted() {
        Map<String, ChatThreadMeta> map = loadMetaMap();
        List<ChatThreadMeta> out = new ArrayList<>(map.values());
        out.sort(Comparator.comparingLong((ChatThreadMeta m) -> m.updatedAtMs).reversed());
        return out;
    }

    public synchronized List<ChatMessage> getMessages(String threadId) {
        if (TextUtils.isEmpty(threadId)) return Collections.emptyList();
        String json = prefs.getString(KEY_MSG_PREFIX + threadId, null);
        if (json == null) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<ChatMessage>>() {}.getType();
            List<ChatMessage> list = gson.fromJson(json, t);
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public synchronized void append(String threadId, ChatMessage msg) {
        if (TextUtils.isEmpty(threadId) || msg == null) return;
        List<ChatMessage> list = getMessages(threadId);
        list.add(msg);
        saveMessages(threadId, list);

        // update meta
        Map<String, ChatThreadMeta> map = loadMetaMap();
        ChatThreadMeta meta = map.get(threadId);
        if (meta == null) {
            meta = new ChatThreadMeta(threadId, threadId, "", false, msg.createdAtMs, msg.text);
        }
        meta.updatedAtMs = Math.max(meta.updatedAtMs, msg.createdAtMs);
        meta.lastMessage = msg.text;
        if (!msg.fromMe) {
            meta.unreadCount = Math.max(0, meta.unreadCount) + 1;
        }
        map.put(threadId, meta);
        saveMetaMap(map);
    }

    public synchronized void markRead(String threadId) {
        if (TextUtils.isEmpty(threadId)) return;
        Map<String, ChatThreadMeta> map = loadMetaMap();
        ChatThreadMeta meta = map.get(threadId);
        if (meta == null) return;
        meta.unreadCount = 0;
        map.put(threadId, meta);
        saveMetaMap(map);
    }

    public synchronized void seedIfEmpty(String threadId, ChatThreadMeta meta, List<ChatMessage> seedMessages) {
        if (TextUtils.isEmpty(threadId)) return;
        List<ChatMessage> existing = getMessages(threadId);
        if (!existing.isEmpty()) {
            upsertThread(meta);
            return;
        }
        if (seedMessages != null && !seedMessages.isEmpty()) {
            saveMessages(threadId, seedMessages);
            ChatMessage last = seedMessages.get(seedMessages.size() - 1);
            meta.updatedAtMs = last.createdAtMs;
            meta.lastMessage = last.text;
        }
        upsertThread(meta);
    }

    private void saveMessages(String threadId, List<ChatMessage> list) {
        prefs.edit().putString(KEY_MSG_PREFIX + threadId, gson.toJson(list)).apply();
    }

    private Map<String, ChatThreadMeta> loadMetaMap() {
        String json = prefs.getString(KEY_THREADS_META, null);
        if (json == null) return new HashMap<>();
        try {
            Type t = new TypeToken<Map<String, ChatThreadMeta>>() {}.getType();
            Map<String, ChatThreadMeta> map = gson.fromJson(json, t);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void saveMetaMap(Map<String, ChatThreadMeta> map) {
        prefs.edit().putString(KEY_THREADS_META, gson.toJson(map)).apply();
    }
}

