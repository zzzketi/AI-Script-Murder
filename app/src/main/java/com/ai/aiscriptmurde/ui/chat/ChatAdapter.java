package com.ai.aiscriptmurde.ui.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messageList;

    public ChatAdapter( List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    // 1. 核心：根据消息类型返回不同的 ViewType
    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    // 2. 核心：创建 ViewHolder
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // 【重点】直接从 parent 获取 Context

        if (viewType == ChatMessage.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_right, parent, false);
            return new UserViewHolder(view);
        }
        else if (viewType == ChatMessage.TYPE_PLOT) {
            View view = inflater.inflate(R.layout.item_chat_left, parent, false);
            return new NpcViewHolder(view);
        }
        else if (viewType == ChatMessage.TYPE_SYSTEM) {
            View view = inflater.inflate(R.layout.item_chat_intro, parent, false);
            return new SystemViewHolder(view);
        }

        throw new IllegalArgumentException("Invalid View Type");
    }

    // 3. 核心：绑定数据
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);
        } else if (holder instanceof NpcViewHolder) {
            ((NpcViewHolder) holder).bind(message);
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    // --- 辅助方法：添加单条消息并滚动 ---
    public void addMessage(ChatMessage message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    // --- ViewHolder 内部类定义 ---

    // 类型 1: 用户 (User)
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
        }

        void bind(ChatMessage message) {
            tvContent.setText(message.getContent());
        }
    }

    // 类型 2: NPC (AI)
    static class NpcViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvName;
        ImageView ivAvatar;

        public NpcViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvName = itemView.findViewById(R.id.tv_name);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }

        void bind(ChatMessage message) {
            tvContent.setText(message.getContent());
            tvName.setText(message.getSenderName());

            // TODO: 在这里加载头像，推荐使用 Glide 或 Picasso
            // Glide.with(itemView.getContext()).load(message.getAvatarUrl()).into(ivAvatar);

            // 默认设置个占位图，防止空白
            ivAvatar.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    // 类型 3: 系统消息 (System)
    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystemMsg;

        public SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMsg = itemView.findViewById(R.id.tv_intro_content);
        }

        void bind(ChatMessage message) {
            tvSystemMsg.setText(message.getContent());
        }
    }
}