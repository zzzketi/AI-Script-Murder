package com.ai.aiscriptmurde.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatMessage;
import com.ai.aiscriptmurde.utils.DBHelper;
import com.ai.aiscriptmurde.utils.DataCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatSearchActivity extends AppCompatActivity {

    public static final String EXTRA_SCRIPT_ID = "EXTRA_SCRIPT_ID";
    public static final String RESULT_TIMESTAMP = "RESULT_TIMESTAMP";
    public static final String RESULT_CONTENT = "RESULT_CONTENT"; // 🔥 新增：用于传递内容的 Key

    private EditText etSearchInput;
    private RecyclerView rvSearchResults;
    private ChatSearchResultAdapter adapter;
    private String scriptId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_search);

        scriptId = getIntent().getStringExtra(EXTRA_SCRIPT_ID);

        etSearchInput = findViewById(R.id.et_search_input);
        rvSearchResults = findViewById(R.id.rv_search_results);
        ImageView ivBack = findViewById(R.id.iv_search_back);

        ivBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupSearchListener();
    }

    private void setupRecyclerView() {
        adapter = new ChatSearchResultAdapter(new ArrayList<>(), message -> {
            Intent resultIntent = new Intent();
            // 🔥 修复：同时传递时间戳和内容，构成复合唯一标识
            resultIntent.putExtra(RESULT_TIMESTAMP, message.getTimestamp());
            resultIntent.putExtra(RESULT_CONTENT, message.getContent());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupSearchListener() {
        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        DBHelper.searchMessages(this, scriptId, query, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                adapter.updateData(messages);
            }

            @Override
            public void onFailure(String errorMessage) {
                // Handle error
            }
        });
    }
}