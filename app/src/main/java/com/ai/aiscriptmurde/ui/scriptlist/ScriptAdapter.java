package com.ai.aiscriptmurde.ui.scriptlist;
// 你的包名

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.model.ScriptModel;
import com.ai.aiscriptmurde.utils.ScriptUtils;

import java.util.List;

public class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.ViewHolder> {

    private List<ScriptModel> mData;
    private Context context;
    private OnItemClickListener mListener; //监听器

    //点击事件接口
    public interface OnItemClickListener {
        void onItemClick(ScriptModel script);
    }

    //向Fragment暴露设置监听器的方法
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public ScriptAdapter(List<ScriptModel> data) {
        this.mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_script, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScriptModel script = mData.get(position);

        // 1. 设置标题
        if (holder.tvTitle != null) {
            holder.tvTitle.setText(script.getTitle());
        }

        // 2. 设置描述
        if (holder.tvDesc != null) {
            holder.tvDesc.setText(script.getDesc());
        }

        // 3. 设置标签
        if (holder.tvTags != null && script.getTags() != null && !script.getTags().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String tag : script.getTags()) {
                sb.append(tag).append(" / ");
            }
            holder.tvTags.setText(sb.toString());
        }

        // 4. 图片加载

        String imageUrl = "http://10.0.2.2:8000/static/images/" + script.getImage() + ".png";

        // 使用 Glide 加载
        com.bumptech.glide.Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background) // 加载占位图
                .error(R.drawable.ic_launcher_background)       // 错误占位图
                .into(holder.ivCover);

        //设置点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    // 只要有人点了这个 Item，就按铃通知 Fragment
                    mListener.onItemClick(script);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvTags; // 声明变量
        ImageView ivCover;

        public ViewHolder(View itemView) {
            super(itemView);
            // 尝试查找控件
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            tvTags = itemView.findViewById(R.id.tv_tags);
            ivCover = itemView.findViewById(R.id.iv_cover);
        }
    }
}
