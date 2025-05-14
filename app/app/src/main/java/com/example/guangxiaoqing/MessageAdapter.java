package com.example.guangxiaoqing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guangxiaoqing.utils.MarkdownHelper;
import com.example.guangxiaoqing.utils.MathExpressionView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);

        if (message.isSent()) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        LinearLayout messageContainer;
        Context context;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tvMessage);
            timeText = itemView.findViewById(R.id.tvTimestamp);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            context = itemView.getContext();
        }

        void bind(Message message) {
            String text = message.getText();

            // 提取数学表达式
            List<String> mathExpressions = MarkdownHelper.extractMathExpressions(text);

            if (!mathExpressions.isEmpty()) {
                // 有数学表达式，使用MathJaxView显示
                // 首先清除消息容器中的所有视图
                messageContainer.removeAllViews();

                // 处理文本中的数学表达式
                String[] textParts = text.split("\\$.*?\\$");
                int expressionIndex = 0;

                // 添加第一部分文本（如果有）
                if (textParts.length > 0 && !textParts[0].isEmpty()) {
                    TextView textView = new TextView(context);
                    textView.setText(MarkdownHelper.formatMarkdown(textParts[0]));
                    textView.setTextColor(messageText.getTextColors());
                    textView.setTextSize(16);
                    // 设置TextView可滚动
                    textView.setMaxLines(1000);
                    textView.setVerticalScrollBarEnabled(true);
                    textView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
                    messageContainer.addView(textView);
                }

                // 添加数学表达式和文本
                for (String expression : mathExpressions) {
                    // 添加数学表达式
                    MathExpressionView mathView = MarkdownHelper.createMathExpressionView(context, "$" + expression + "$");
                    messageContainer.addView(mathView);

                    // 添加下一部分文本（如果有）
                    if (expressionIndex + 1 < textParts.length && !textParts[expressionIndex + 1].isEmpty()) {
                        TextView textView = new TextView(context);
                        textView.setText(MarkdownHelper.formatMarkdown(textParts[expressionIndex + 1]));
                        textView.setTextColor(messageText.getTextColors());
                        textView.setTextSize(16);
                        // 设置TextView可滚动
                        textView.setMaxLines(1000);
                        textView.setVerticalScrollBarEnabled(true);
                        textView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
                        messageContainer.addView(textView);
                    }

                    expressionIndex++;
                }

                // 隐藏原始消息文本视图
                messageText.setVisibility(View.GONE);
            } else {
                // 没有数学表达式，使用普通文本显示
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(MarkdownHelper.formatMarkdown(text));
                // 设置TextView可滚动
                messageText.setMovementMethod(new android.text.method.ScrollingMovementMethod());
            }

            timeText.setText(message.getTimestamp());
        }
    }

    private static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        LinearLayout messageContainer;
        Context context;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tvMessage);
            timeText = itemView.findViewById(R.id.tvTimestamp);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            context = itemView.getContext();
        }

        void bind(Message message) {
            String text = message.getText();

            // 提取数学表达式
            List<String> mathExpressions = MarkdownHelper.extractMathExpressions(text);

            if (!mathExpressions.isEmpty()) {
                // 有数学表达式，使用MathJaxView显示
                // 首先清除消息容器中的所有视图
                messageContainer.removeAllViews();

                // 处理文本中的数学表达式
                String[] textParts = text.split("\\$.*?\\$");
                int expressionIndex = 0;

                // 添加第一部分文本（如果有）
                if (textParts.length > 0 && !textParts[0].isEmpty()) {
                    TextView textView = new TextView(context);
                    textView.setText(MarkdownHelper.formatMarkdown(textParts[0]));
                    textView.setTextColor(messageText.getTextColors());
                    textView.setTextSize(16);
                    // 设置TextView可滚动
                    textView.setMaxLines(1000);
                    textView.setVerticalScrollBarEnabled(true);
                    textView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
                    messageContainer.addView(textView);
                }

                // 添加数学表达式和文本
                for (String expression : mathExpressions) {
                    // 添加数学表达式
                    MathExpressionView mathView = MarkdownHelper.createMathExpressionView(context, "$" + expression + "$");
                    messageContainer.addView(mathView);

                    // 添加下一部分文本（如果有）
                    if (expressionIndex + 1 < textParts.length && !textParts[expressionIndex + 1].isEmpty()) {
                        TextView textView = new TextView(context);
                        textView.setText(MarkdownHelper.formatMarkdown(textParts[expressionIndex + 1]));
                        textView.setTextColor(messageText.getTextColors());
                        textView.setTextSize(16);
                        // 设置TextView可滚动
                        textView.setMaxLines(1000);
                        textView.setVerticalScrollBarEnabled(true);
                        textView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
                        messageContainer.addView(textView);
                    }

                    expressionIndex++;
                }

                // 隐藏原始消息文本视图
                messageText.setVisibility(View.GONE);
            } else {
                // 没有数学表达式，使用普通文本显示
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(MarkdownHelper.formatMarkdown(text));
                // 设置TextView可滚动
                messageText.setMovementMethod(new android.text.method.ScrollingMovementMethod());
            }

            timeText.setText(message.getTimestamp());
        }
    }
}