package com.ai.aiscriptmurde.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 定义常量：区分消息类型
    private static final int TYPE_AI_LEFT = 0;   // 左边：AI发的消息
    private static final int TYPE_USER_RIGHT = 1; // 右边：用户发的消息

    // 数据源：存放所有的聊天记录
    private List<ChatMessage> messages = new ArrayList<>();

    // --- 1. 数据操作方法 ---

    /**
     * 重新设置整个列表数据 (通常用于刚进页面加载历史记录)
     */
    public void setMessages(List<ChatMessage> list) {
        this.messages = list;
        notifyDataSetChanged(); // 通知刷新
    }

    /**
     * 添加单条消息 (通常用于发送或接收新消息时)
     * 这样比 notifyDataSetChanged 性能更好，且有动画效果
     */
    public void addMessage(ChatMessage msg) {
        this.messages.add(msg);
        notifyItemInserted(messages.size() - 1); // 只刷新最后一行
    }

    // --- 2. 核心逻辑：决定是用左边布局还是右边布局 ---

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        // 如果 isUser 为 true，返回右边类型，否则返回左边
        if (msg.isUser) {
            return TYPE_USER_RIGHT;
        } else {
            return TYPE_AI_LEFT;
        }
    }

    // --- 3. 核心逻辑：创建 (ViewHolder) ---

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_USER_RIGHT) {
            // 加载右边的 XML
            View view = inflater.inflate(R.layout.item_chat_right, parent, false);
            return new UserViewHolder(view);
        } else {
            // 加载左边的 XML
            View view = inflater.inflate(R.layout.item_chat_left, parent, false);
            return new AIViewHolder(view);
        }
    }

    // --- 4. 核心逻辑： (绑定数据) ---

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (holder instanceof UserViewHolder) {
            // 处理右边用户逻辑
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvContent.setText(msg.content);
            // 如果你想给用户设个固定头像，可以在这里设
            // userHolder.ivAvatar.setImageResource(R.drawable.ic_user_avatar);

        } else if (holder instanceof AIViewHolder) {
            // 处理左边 AI 逻辑
            AIViewHolder aiHolder = (AIViewHolder) holder;
            aiHolder.tvContent.setText(msg.content);

            // 设置 NPC 名字 (比如 "管家")
            aiHolder.tvName.setText(msg.senderName);

            // 以后可以在这里根据 roleId 设置不同的头像
            // if ("npc_01".equals(msg.roleId)) { ... }
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    // --- 5. 内部类：定义 ViewHolder ---

    // 右边 (用户) 的 ViewHolder
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        ImageView ivAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // 这里 R.id.xxx 必须和你 item_chat_right.xml 里的 ID 一致
            tvContent = itemView.findViewById(R.id.tv_content);
            // ivAvatar = itemView.findViewById(R.id.iv_avatar); // 如果 XML 里有头像就加上
        }
    }

    // 左边 (AI) 的 ViewHolder
    static class AIViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvName;
        ImageView ivAvatar;

        public AIViewHolder(@NonNull View itemView) {
            super(itemView);
            // 这里 R.id.xxx 必须和你 item_chat_left.xml 里的 ID 一致
            tvContent = itemView.findViewById(R.id.tv_content);
            tvName = itemView.findViewById(R.id.tv_name);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }
    }
}