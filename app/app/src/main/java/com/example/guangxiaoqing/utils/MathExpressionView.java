package com.example.guangxiaoqing.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.example.guangxiaoqing.R;

/**
 * 自定义视图，用于显示数学表达式
 */
public class MathExpressionView extends LinearLayout {
    private WebView webView;

    public MathExpressionView(Context context) {
        super(context);
        init(context);
    }

    public MathExpressionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MathExpressionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_math_expression, this, true);
        webView = findViewById(R.id.webView);

        // 设置WebView配置
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setBackgroundColor(0x00000000); // 透明背景
        webView.setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /**
     * 设置LaTeX表达式
     * @param latex LaTeX表达式
     */
    public void setLatex(String latex) {
        // 构建HTML内容，使用KaTeX渲染LaTeX表达式
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/katex@0.16.4/dist/katex.min.css\">\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/katex@0.16.4/dist/katex.min.js\"></script>\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/katex@0.16.4/dist/contrib/auto-render.min.js\"></script>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            background-color: transparent;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            font-size: 16px;\n" +
                "            color: #000000;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"math\">" + latex + "</div>\n" +
                "    <script>\n" +
                "        document.addEventListener(\"DOMContentLoaded\", function() {\n" +
                "            renderMathInElement(document.getElementById(\"math\"), {\n" +
                "                delimiters: [\n" +
                "                    {left: \"$$\", right: \"$$\", display: true},\n" +
                "                    {left: \"$\", right: \"$\", display: false}\n" +
                "                ],\n" +
                "                throwOnError: false\n" +
                "            });\n" +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        webView.loadDataWithBaseURL("https://cdn.jsdelivr.net/", html, "text/html", "UTF-8", null);
    }
}
