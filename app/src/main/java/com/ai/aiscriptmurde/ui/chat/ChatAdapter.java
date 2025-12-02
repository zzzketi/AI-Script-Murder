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

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 定义三种类型
    private static final int TYPE_HEADER = 0;    // 顶部背景便签
    private static final int TYPE_AI = 1;        // 左边 AI
    private static final int TYPE_USER = 2;      // 右边 用户
    private static final int TYPE_SYSTEM = 3;   // 系统消息（暂未使用）

    private List<ChatMessage> messages = new ArrayList<>();
    private String backgroundStory; // 专门存背景故事

    // --- 1. 设置背景故事的方法 ---
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
        // 注意：因为有个头布局，所以插入位置是 size (不用 -1)
        notifyItemInserted(getItemCount() - 1);
    }

    // --- 2. 核心：数量要 +1 (为了放头布局) ---
    @Override
    public int getItemCount() {
        // 如果有背景故事，总数 = 消息数 + 1
        return backgroundStory != null ? messages.size() + 1 : messages.size();
    }

    // --- 3. 核心：判断类型 ---
    @Override
    public int getItemViewType(int position) {
        // 如果有背景故事，且当前是第 0 个，那就是 Header
        if (backgroundStory != null && position == 0) {
            return TYPE_HEADER;
        }

        // 注意：因为第0个被占了，所以取消息要 index - 1
        int realPosition = backgroundStory != null ? position - 1 : position;
        ChatMessage msg = messages.get(realPosition);

        // ✅ 如果发送者是“系统”，就用便签样式
        if (msg.senderName != null && msg.senderName.contains("系统") || msg.senderName.contains("主持人") ) {
            return TYPE_SYSTEM;
        }

        return msg.isUser ? TYPE_USER : TYPE_AI;
    }

    // --- 4. 创建 ViewHolder ---
    //  修改 onCreateViewHolder，复用便签布局
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_chat_intro, parent, false));
        } else if (viewType == TYPE_SYSTEM) {
            // ✅ 复用 item_chat_intro.xml，但我们需要一个新的ViewHolder来绑定不同的数据
            // 或者直接复用 HeaderViewHolder 也可以，只要 ID 一样
            return new SystemViewHolder(inflater.inflate(R.layout.item_chat_intro, parent, false));
        } else if (viewType == TYPE_USER) {
            return new UserViewHolder(inflater.inflate(R.layout.item_chat_right, parent, false));
        } else {
            return new AIViewHolder(inflater.inflate(R.layout.item_chat_left, parent, false));
        }
    }

    // 在 ChatAdapter.java 中添加
    public List<ChatMessage> getMessages() {
        return messages;
    }

    // 4. 修改 onBindViewHolder
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {



        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvContent.setText(backgroundStory);
            // ✅ 强制设回 "剧本背景" (防止被系统消息复用时改成了别的)
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
            }
            // ✅ 处理系统便签
            else if (holder instanceof SystemViewHolder) {
                ((SystemViewHolder) holder).tvContent.setText(msg.content);
                // ✅ 设置为 "系统提示"
                if (((SystemViewHolder) holder).tvTitle != null) {
                    ((SystemViewHolder) holder).tvTitle.setText("📜 系统提示");
                }
            }
        }
    }

    // 5. 新增一个 ViewHolder (其实结构和 HeaderViewHolder 一模一样)
    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvTitle; // 如果你的 item_chat_intro 里有标题的 ID，可以拿来改
        public SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_intro_content);
            // 假设你的 item_chat_intro.xml 里那个 "剧本背景" 的 TextView 没有 ID
            // 你可以去 xml 里给它加个 ID 叫 tv_intro_title，然后在这里 findViewById
            // 暂时先只绑定 content
            // 🛠️ 调试代码：如果找不到，在 Logcat 打印一下
            tvTitle = itemView.findViewById(R.id.tv_intro_title);
        }
    }

    // --- ViewHolder 类定义 ---
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;
        TextView tvTitle; // ✅ 也要加这个

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_intro_content);
            // ✅ 也要初始化
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