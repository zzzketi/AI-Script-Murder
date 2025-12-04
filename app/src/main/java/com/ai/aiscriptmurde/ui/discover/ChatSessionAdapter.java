package com.ai.aiscriptmurde.ui.discover;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatSessionEntity;
import com.ai.aiscriptmurde.utils.TimeUtils;
import java.util.List;

public class ChatSessionAdapter extends RecyclerView.Adapter<ChatSessionAdapter.ViewHolder> {

    private final Context context;
    private final List<ChatSessionEntity> sessions;
    private final OnSessionInteractionListener interactionListener;

    public interface OnSessionInteractionListener {
        void onSessionClicked(ChatSessionEntity session);
        void onSessionLongClicked(ChatSessionEntity session);
    }

    public ChatSessionAdapter(Context context, List<ChatSessionEntity> sessions, OnSessionInteractionListener listener) {
        this.context = context;
        this.sessions = sessions;
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSessionEntity session = sessions.get(position);

        holder.tvGroupName.setText(session.getScriptTitle() != null ? session.getScriptTitle() : "未知剧本");
        holder.tvLastMessage.setText(session.getLastMessage());
        holder.tvTimestamp.setText(TimeUtils.getFriendlyTimeSpan(session.getTimestamp()));
        
        holder.ivAvatar.setImageResource(R.drawable.ic_group);

        // 🔥 核心UI逻辑：根据 unreadCount 更新红点
        if (session.getUnreadCount() > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            if (session.getUnreadCount() > 99) {
                holder.tvUnreadCount.setText("99+");
            } else {
                holder.tvUnreadCount.setText(String.valueOf(session.getUnreadCount()));
            }
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onSessionClicked(session);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (interactionListener != null) {
                interactionListener.onSessionLongClicked(session);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvGroupName;
        TextView tvLastMessage;
        TextView tvTimestamp;
        TextView tvUnreadCount; // 🔥 新增红点 TextView

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
        }
    }
}