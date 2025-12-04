package com.ai.aiscriptmurde.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatMessage;
import com.ai.aiscriptmurde.utils.TimeUtils;

import java.util.List;

public class ChatSearchResultAdapter extends RecyclerView.Adapter<ChatSearchResultAdapter.ViewHolder> {

    private final List<ChatMessage> messages;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChatMessage message);
    }

    public ChatSearchResultAdapter(List<ChatMessage> messages, OnItemClickListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message, listener);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateData(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView sender;
        private final TextView content;
        private final TextView timestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.tv_result_sender);
            content = itemView.findViewById(R.id.tv_result_content);
            timestamp = itemView.findViewById(R.id.tv_result_timestamp);
        }

        void bind(final ChatMessage message, final OnItemClickListener listener) {
            sender.setText(message.getSenderName());
            content.setText(message.getContent());
            // 🔥 修复：调用新的、为搜索结果定制的时间格式化方法
            timestamp.setText(TimeUtils.getSearchItemTime(message.getTimestamp()));
            itemView.setOnClickListener(v -> listener.onItemClick(message));
        }
    }
}