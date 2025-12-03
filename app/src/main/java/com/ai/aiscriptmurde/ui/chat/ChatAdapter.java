package com.ai.aiscriptmurde.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_AI = 1;
    private static final int TYPE_USER = 2;
    private static final int TYPE_SYSTEM = 3;

    private List<ChatMessage> messages = new ArrayList<>();
    private String backgroundStory;

    public void setBackgroundStory(String story) {
        this.backgroundStory = story;
        notifyDataSetChanged();
    }

    public void setMessages(List<ChatMessage> list) {
        this.messages = list;
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage msg) {
        this.messages.add(msg);
        notifyItemInserted(getItemCount() - 1);
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    /**
     * 🔥 终极修复：让 Adapter 自己负责根据复合唯一标识（时间戳+内容）查找位置
     */
    public int findPositionByTimestampAndContent(long timestamp, String content) {
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg.getTimestamp() == timestamp && Objects.equals(msg.getContent(), content)) {
                return backgroundStory != null ? i + 1 : i;
            }
        }
        return -1; // Not found
    }

    @Override
    public int getItemCount() {
        return backgroundStory != null ? messages.size() + 1 : messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (backgroundStory != null && position == 0) {
            return TYPE_HEADER;
        }
        int realPosition = backgroundStory != null ? position - 1 : position;
        ChatMessage msg = messages.get(realPosition);
        if (msg.senderName != null && (msg.senderName.contains("系统") || msg.senderName.contains("主持人"))) {
            return TYPE_SYSTEM;
        }
        return msg.isUser ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_chat_intro, parent, false));
        } else if (viewType == TYPE_SYSTEM) {
            return new SystemViewHolder(inflater.inflate(R.layout.item_chat_intro, parent, false));
        } else if (viewType == TYPE_USER) {
            return new UserViewHolder(inflater.inflate(R.layout.item_chat_right, parent, false));
        } else {
            return new AIViewHolder(inflater.inflate(R.layout.item_chat_left, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvContent.setText(backgroundStory);
            if (((HeaderViewHolder) holder).tvTitle != null) {
                ((HeaderViewHolder) holder).tvTitle.setText("📜 剧本背景");
            }
        } else {
            int realPosition = backgroundStory != null ? position - 1 : position;
            ChatMessage msg = messages.get(realPosition);
            if (holder instanceof UserViewHolder) {
                ((UserViewHolder) holder).tvContent.setText(msg.content);
            } else if (holder instanceof AIViewHolder) {
                ((AIViewHolder) holder).tvContent.setText(msg.content);
                ((AIViewHolder) holder).tvName.setText(msg.senderName);
            } else if (holder instanceof SystemViewHolder) {
                ((SystemViewHolder) holder).tvContent.setText(msg.content);
                if (((SystemViewHolder) holder).tvTitle != null) {
                    ((SystemViewHolder) holder).tvTitle.setText("📜 系统提示");
                }
            }
        }
    }

    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTitle;
        public SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_intro_content);
            tvTitle = itemView.findViewById(R.id.tv_intro_title);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTitle;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_intro_content);
            tvTitle = itemView.findViewById(R.id.tv_intro_title);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
        }
    }

    static class AIViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvName;
        public AIViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvName = itemView.findViewById(R.id.tv_name);
        }
    }
}