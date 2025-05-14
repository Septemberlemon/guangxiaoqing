package com.example.guangxiaoqing;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.EditText;
import android.widget.ImageButton;
import com.example.guangxiaoqing.utils.ToastHelper;

import com.example.guangxiaoqing.api.NetworkService;
import com.example.guangxiaoqing.api.NetworkCallback;
import com.example.guangxiaoqing.model.ChatMessage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// 添加所有需要的导入语句到这里
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Executor;

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnMenu;
    private List<Message> messageList;
    private List<ChatMessage> chatHistory;
    private MessageAdapter messageAdapter;
    // 使用主线程执行器替代Handler
    private Executor mainExecutor;
    private UserSession userSession;
    private LinearLayoutManager layoutManager;
    private DateTimeFormatter timeFormatter;
    private NetworkService networkService;
    private static final int MAX_HISTORY_SIZE = 10; // 保留最近的10条消息作为上下文

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 仅隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 使用更新后的布局文件
        setContentView(R.layout.activity_chat);

        // 使用新的API设置状态栏和导航栏
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), decorView);

        // 设置状态栏文字颜色为深色
        insetsController.setAppearanceLightStatusBars(true);

        // 不再直接调用setStatusBarColor，而是通过主题或布局文件控制
        // 如果需要兼容旧版本，可以使用以下代码
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            // 在Android 12之前的版本上使用旧方法
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.qq_bg_light));
        }

        // 设置软键盘行为
        setupKeyboardBehavior();

        // 初始化UserSession
        userSession = UserSession.getInstance(this);

        // 初始化时间格式化器
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // 初始化网络服务
        networkService = new NetworkService(this);

        // 使用主线程执行器替代Handler
        mainExecutor = ContextCompat.getMainExecutor(this);

        // 初始化聊天历史记录列表
        chatHistory = new ArrayList<>();

        initViews();
        setupRecyclerView();
        setListeners();

        // 添加欢迎消息
        addWelcomeMessage();
    }

    private void setupKeyboardBehavior() {
        // 使用新的API处理键盘
        View rootView = findViewById(android.R.id.content);

        // 使用OnApplyWindowInsetsListener替代WindowInsetsAnimationCallback
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // 获取IME（键盘）的insets
            int imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            // 如果键盘可见，可以进行额外处理
            if (imeInsets > systemInsets) {
                // 键盘可见，可以调整布局
            }

            // 返回insets以便继续分发
            return insets;
        });

        // 设置窗口软输入模式
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void initViews() {
        recyclerChat = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMenu = findViewById(R.id.btnMenu);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        // 添加这一行，提高滚动性能
        recyclerChat.setItemAnimator(null);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(messageAdapter);
    }

    private void addWelcomeMessage() {
        String currentTime = LocalTime.now().format(timeFormatter);
        Message message = new Message("您好！我是广小轻AI助手，请问有什么可以帮到您？", currentTime, false);
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();

        // 添加到聊天历史
        chatHistory.add(new ChatMessage("assistant", message.getText()));
    }

    private void setListeners() {
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                etMessage.setText("");
            }
        });

        btnMenu.setOnClickListener(v -> showPopupMenu());
    }

    private void showPopupMenu() {
        // 创建一个简单的弹出菜单
        PopupMenu popupMenu = new PopupMenu(this, btnMenu);

        // 添加菜单项
        popupMenu.getMenu().add(Menu.NONE, 1, Menu.NONE, "修改密码");
        popupMenu.getMenu().add(Menu.NONE, 2, Menu.NONE, "清空聊天记录");
        popupMenu.getMenu().add(Menu.NONE, 3, Menu.NONE, "退出登录");

        // 设置菜单项点击监听器
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    openChangePasswordActivity();
                    return true;
                case 2:
                    clearChatHistory();
                    return true;
                case 3:
                    logout();
                    return true;
                default:
                    return false;
            }
        });

        // 显示菜单
        popupMenu.show();
    }

    private void openChangePasswordActivity() {
        Intent intent = new Intent(ChatActivity.this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    private void clearChatHistory() {
        // 清空聊天记录
        int oldSize = messageList.size();
        messageList.clear();
        chatHistory.clear();
        // 使用更精确的方法替代notifyDataSetChanged
        messageAdapter.notifyItemRangeRemoved(0, oldSize);

        // 重新添加欢迎消息
        addWelcomeMessage();

        ToastHelper.showInfo(this, "聊天记录已清空");
    }

    private void logout() {
        // 清除登录状态
        userSession.clearLoginSession();

        // 直接跳转到登录界面，不显示通知
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void sendMessage(String text) {
        String currentTime = LocalTime.now().format(timeFormatter);
        // 添加用户消息
        Message userMessage = new Message(text, currentTime, true);
        messageList.add(userMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();

        // 调用后端API获取回复
        sendChatRequest(text);
    }

    private void sendChatRequest(String userMessage) {
        // 显示正在输入状态（可以添加一个正在输入的提示）
        String currentTime = LocalTime.now().format(timeFormatter);
        Message typingMessage = new Message("正在输入...", currentTime, false);
        messageList.add(typingMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();

        // 在发送请求前，先处理聊天历史，确保没有重复
        List<ChatMessage> cleanHistory = cleanupChatHistory(chatHistory);

        // 不在这里添加用户消息，而是在回调中添加
        // 删除以下代码块
        /*
        boolean userMessageExists = false;
        for (ChatMessage msg : cleanHistory) {
            if (msg.getRole().equals("user") && msg.getContent().equals(userMessage)) {
                userMessageExists = true;
                break;
            }
        }

        if (!userMessageExists) {
            cleanHistory.add(new ChatMessage("user", userMessage));
        }
        */

        // 调用后端API获取回复，同时发送处理过的历史消息
        networkService.chatWithHistory(userMessage, cleanHistory, new NetworkCallback<String>() {
            private Message assistantMessage = null;
            private boolean isThinking = false; // 用于标记是否在<think>标签内
            private StringBuilder currentResponse = new StringBuilder();
            private boolean isFirstChar = true; // 用于标记是否是第一个字符
            private boolean shouldUpdateUI = false; // 用于控制UI更新频率
            // 将这两个变量声明为类成员变量，而不是方法内的局部变量
            private boolean hasAddedUserMessage = false;
            private boolean hasAddedAssistantMessage = false;

            @Override
            public void onSuccess(String response) {
                mainExecutor.execute(() -> {
                    try {
                        // 删除这里的变量声明，因为已经在外部声明了
                        // boolean hasAddedUserMessage = false;
                        // boolean hasAddedAssistantMessage = false;

                        // 检查是否是think标签的开始
                        if (response.equals("<")) {
                            isThinking = true;
                            return;
                        } else if (response.equals(">") && isThinking) {
                            isThinking = false;
                            // 不再在think标签结束后添加换行符
                            return;
                        } else if (isThinking) {
                            // 在think标签内的内容不显示
                            return;
                        }

                        // 如果还有"正在输入..."消息，移除它
                        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getText().equals("正在输入...")) {
                            messageList.remove(messageList.size() - 1);
                            messageAdapter.notifyItemRemoved(messageList.size());
                        }

                        // 处理换行符
                        if (response.equals("\n")) {
                            if (isFirstChar || currentResponse.length() == 0) {
                                // 忽略开头的换行符
                                return;
                            }
                            // 只在非空行后添加换行符，并且避免连续的换行符
                            if (!currentResponse.toString().endsWith("\n")) {
                                currentResponse.append(response);
                                shouldUpdateUI = true;
                            }
                            return;
                        }

                        // 追加新字符
                        currentResponse.append(response);
                        isFirstChar = false;
                        shouldUpdateUI = true;

                        // 只在需要时更新UI
                        if (shouldUpdateUI) {
                            String currentTime = LocalTime.now().format(timeFormatter);

                            if (assistantMessage == null) {
                                // 创建新消息
                                assistantMessage = new Message(currentResponse.toString(), currentTime, false);
                                messageList.add(assistantMessage);
                                messageAdapter.notifyItemInserted(messageList.size() - 1);
                            } else {
                                // 更新现有消息
                                assistantMessage.setText(currentResponse.toString());
                                int position = messageList.indexOf(assistantMessage);
                                if (position != -1) {
                                    messageAdapter.notifyItemChanged(position);
                                }
                            }

                            // 确保每次更新后都能看到最新内容
                            scrollToBottom();

                            shouldUpdateUI = false;
                        }

                        // 只有在收到完整句子的结尾标记时，才添加到聊天历史
                        if (response.equals("。") || response.equals("？") || response.equals("！") || response.equals(".") || response.equals("?") || response.equals("!")) {
                            // 确保用户消息只添加一次
                            if (!hasAddedUserMessage) {
                                // 检查是否已存在相同的用户消息
                                boolean userMessageExists = false;
                                for (ChatMessage msg : chatHistory) {
                                    if (msg.getRole().equals("user") && msg.getContent().equals(userMessage)) {
                                        userMessageExists = true;
                                        break;
                                    }
                                }

                                if (!userMessageExists) {
                                    chatHistory.add(new ChatMessage("user", userMessage));
                                }
                                hasAddedUserMessage = true;
                            }

                            // 更新助手消息（如果已存在则替换，否则添加）
                            if (currentResponse.length() > 0) {
                                String finalResponse = currentResponse.toString().trim();

                                // 检查是否包含思考过程（通常以"好的，"或类似开头）
                                if (finalResponse.startsWith("好的，") || finalResponse.contains("我需要") || finalResponse.contains("我应该")) {
                                    // 尝试提取最终回复部分（通常在最后一个换行符之后）
                                    int lastNewlineIndex = finalResponse.lastIndexOf("\n\n");
                                    if (lastNewlineIndex > 0 && lastNewlineIndex < finalResponse.length() - 3) {
                                        finalResponse = finalResponse.substring(lastNewlineIndex + 2);
                                    }
                                }

                                // 检查是否已存在相同的助手消息
                                boolean assistantMessageExists = false;
                                for (ChatMessage msg : chatHistory) {
                                    if (msg.getRole().equals("assistant") && msg.getContent().equals(finalResponse)) {
                                        assistantMessageExists = true;
                                        break;
                                    }
                                }

                                // 只有当消息不存在时才添加或更新
                                if (!assistantMessageExists) {
                                    // 如果已经添加过助手消息，则更新最后一条
                                    if (hasAddedAssistantMessage && !chatHistory.isEmpty()) {
                                        for (int i = chatHistory.size() - 1; i >= 0; i--) {
                                            ChatMessage msg = chatHistory.get(i);
                                            if (msg.getRole().equals("assistant")) {
                                                chatHistory.set(i, new ChatMessage("assistant", finalResponse));
                                                break;
                                            }
                                        }
                                    } else {
                                        // 否则添加新的助手消息
                                        chatHistory.add(new ChatMessage("assistant", finalResponse));
                                        hasAddedAssistantMessage = true;

                                        // 添加日志，确认助手消息已添加到历史记录
                                        Log.d("ChatActivity", "Added assistant message to history: " + finalResponse);
                                    }
                                }

                                // 限制历史记录大小
                                while (chatHistory.size() > MAX_HISTORY_SIZE * 2) { // 乘以2是因为每次对话有用户和助手两条消息
                                    chatHistory.remove(0);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ChatActivity", "Error updating chat UI", e);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // 使用主线程执行器替代Handler
                mainExecutor.execute(() -> {
                    try {
                        // 移除"正在输入..."消息
                        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).getText().equals("正在输入...")) {
                            messageList.remove(messageList.size() - 1);
                            messageAdapter.notifyItemRemoved(messageList.size());
                        }

                        ToastHelper.showError(ChatActivity.this, errorMessage);
                        // 添加错误消息到聊天记录
                        String currentTime = LocalTime.now().format(timeFormatter);
                        Message errorMsg = new Message("抱歉，我遇到了一些问题，请稍后再试。", currentTime, false);
                        messageList.add(errorMsg);
                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                        scrollToBottom();

                        // 添加到聊天历史
                        chatHistory.add(new ChatMessage("assistant", errorMsg.getText()));
                    } catch (Exception e) {
                        Log.e("ChatActivity", "Error handling chat error", e);
                    }
                });
            }
        });
    }

    // 新增方法：清理聊天历史，去除重复项
    private List<ChatMessage> cleanupChatHistory(List<ChatMessage> history) {
        List<ChatMessage> cleanHistory = new ArrayList<>();

        // 使用Set来跟踪已添加的消息内容
        Set<String> addedMessages = new HashSet<>();

        for (ChatMessage msg : history) {
            // 创建唯一标识符：角色+内容
            String messageKey = msg.getRole() + ":" + msg.getContent();

            // 如果这个消息还没有添加过，则添加到清理后的历史中
            if (!addedMessages.contains(messageKey)) {
                cleanHistory.add(msg);
                addedMessages.add(messageKey);
            }
        }

        return cleanHistory;
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            // 直接滚动到底部，不使用post延迟
            recyclerChat.scrollToPosition(messageList.size() - 1);

            // 强制立即重新布局，确保滚动生效
            recyclerChat.requestLayout();
        }
    }
}

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
// }

// 注意：使用java.time API需要在build.gradle中启用核心库反糖（coreLibraryDesugaring）
// 如果项目的minSdkVersion < 26
// compileOptions {
//     sourceCompatibility JavaVersion.VERSION_1_8
//     targetCompatibility JavaVersion.VERSION_1_8
//     coreLibraryDesugaringEnabled true
// }
// dependencies {
//     coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.8'
