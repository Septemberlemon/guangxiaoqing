package com.example.guangxiaoqing.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.SubscriptSpan;
import android.text.style.LineBackgroundSpan;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类，用于处理简单的Markdown格式化
 */
public class MarkdownHelper {

    // 匹配双星号包围的文本的正则表达式
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");

    // 匹配<think></think>包围的文本的正则表达式
    private static final Pattern THINK_PATTERN = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);

    // 匹配Markdown标题的正则表达式（# 到 ######）
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    // 匹配数学表达式的正则表达式（$...$）
    private static final Pattern MATH_PATTERN = Pattern.compile("\\$(.*?)\\$");

    // 匹配分数表达式的正则表达式（\frac{分子}{分母}）
    private static final Pattern FRAC_PATTERN = Pattern.compile("\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}");

    /**
     * 将文本中的Markdown格式转换为带格式的SpannableString
     * 目前支持：
     * - 双星号包围的文本显示为加粗
     * - <think></think>包围的文本显示为灰色小字体
     * - Markdown标题（# 到 ######）显示为不同大小的粗体文本
     * - 数学表达式（$...$）显示为特殊样式
     * - 分数表达式（\frac{分子}{分母}）显示为分数形式
     *
     * @param text 原始文本
     * @return 格式化后的SpannableString
     */
    public static SpannableString formatMarkdown(String text) {
        if (text == null) {
            return new SpannableString("");
        }

        // 创建一个新的字符串来存储处理后的文本
        StringBuilder processedText = new StringBuilder();

        // 存储样式信息的列表
        class StyleInfo {
            int start; // 在处理后文本中的起始位置
            int end;   // 在处理后文本中的结束位置
            boolean isBold;
            boolean isThink;
            boolean isMath;  // 是否是数学表达式
            int headingLevel; // 0表示不是标题，1-6表示标题级别
            String mathContent; // 数学表达式的内容

            StyleInfo(int start, int end, boolean isBold, boolean isThink, int headingLevel) {
                this.start = start;
                this.end = end;
                this.isBold = isBold;
                this.isThink = isThink;
                this.headingLevel = headingLevel;
                this.isMath = false;
                this.mathContent = null;
            }

            // 用于数学表达式的构造函数
            StyleInfo(int start, int end, String mathContent) {
                this.start = start;
                this.end = end;
                this.isBold = false;
                this.isThink = false;
                this.headingLevel = 0;
                this.isMath = true;
                this.mathContent = mathContent;
            }
        }

        List<StyleInfo> styles = new ArrayList<>();

        // 首先处理标题，因为它们是基于行的
        Matcher headingMatcher = HEADING_PATTERN.matcher(text);
        StringBuilder textWithoutHeadings = new StringBuilder(text);
        List<StyleInfo> headingStyles = new ArrayList<>();

        // 从后向前处理标题，避免索引变化问题
        List<Integer> headingStarts = new ArrayList<>();
        List<Integer> headingEnds = new ArrayList<>();
        List<String> headingContents = new ArrayList<>();
        List<Integer> headingLevels = new ArrayList<>();

        while (headingMatcher.find()) {
            headingStarts.add(headingMatcher.start());
            headingEnds.add(headingMatcher.end());
            String markers = headingMatcher.group(1); // #符号
            String content = headingMatcher.group(2); // 标题内容
            headingContents.add(content);
            headingLevels.add(markers.length()); // 标题级别
        }

        // 从后向前替换标题
        for (int i = headingStarts.size() - 1; i >= 0; i--) {
            int start = headingStarts.get(i);
            int end = headingEnds.get(i);
            String content = headingContents.get(i);

            // 替换标题为纯文本内容
            textWithoutHeadings.replace(start, end, content);
        }

        // 使用状态机处理剩余的文本（加粗和思考）
        String remainingText = textWithoutHeadings.toString();
        int currentPos = 0;
        int processedPos = 0;

        // 记录标题的位置
        int[] headingPositions = new int[headingStarts.size()];
        for (int i = 0; i < headingStarts.size(); i++) {
            headingPositions[i] = headingStarts.get(i);
        }

        // 处理剩余的标记
        while (currentPos < remainingText.length()) {
            // 检查是否是数学表达式开始
            if (currentPos < remainingText.length() &&
                remainingText.charAt(currentPos) == '$') {

                // 找到数学表达式结束位置
                int mathEnd = remainingText.indexOf("$", currentPos + 1);
                if (mathEnd != -1) {
                    // 提取数学表达式内容
                    String mathContent = remainingText.substring(currentPos + 1, mathEnd);

                    // 记录样式信息
                    int mathStart = processedPos;

                    // 处理数学表达式中的上下标符号，去除^和_
                    String processedMathContent = processMathSymbols(mathContent);
                    processedText.append(processedMathContent);
                    processedPos += processedMathContent.length();

                    // 添加数学表达式样式
                    styles.add(new StyleInfo(mathStart, processedPos, mathContent));

                    // 移动当前位置
                    currentPos = mathEnd + 1;
                } else {
                    // 没有找到结束标记，直接添加当前字符
                    processedText.append(remainingText.charAt(currentPos));
                    currentPos++;
                    processedPos++;
                }
            }
            // 检查是否是加粗标记开始
            else if (currentPos + 1 < remainingText.length() &&
                remainingText.charAt(currentPos) == '*' &&
                remainingText.charAt(currentPos + 1) == '*') {

                // 找到加粗标记结束位置
                int boldEnd = remainingText.indexOf("**", currentPos + 2);
                if (boldEnd != -1) {
                    // 提取加粗文本内容
                    String boldContent = remainingText.substring(currentPos + 2, boldEnd);

                    // 记录样式信息
                    int boldStart = processedPos;
                    processedText.append(boldContent);
                    processedPos += boldContent.length();

                    styles.add(new StyleInfo(boldStart, processedPos, true, false, 0));

                    // 移动当前位置
                    currentPos = boldEnd + 2;
                } else {
                    // 没有找到结束标记，直接添加当前字符
                    processedText.append(remainingText.charAt(currentPos));
                    currentPos++;
                    processedPos++;
                }
            }
            // 检查是否是思考标记开始
            else if (currentPos + 6 < remainingText.length() &&
                     remainingText.substring(currentPos, currentPos + 7).equals("<think>")) {

                // 找到思考标记结束位置
                int thinkEnd = remainingText.indexOf("</think>", currentPos + 7);
                if (thinkEnd != -1) {
                    // 提取思考文本内容
                    String thinkContent = remainingText.substring(currentPos + 7, thinkEnd);

                    // 处理思考文本中的换行符，确保不会在开头添加额外的空行
                    if (thinkContent.startsWith("\n")) {
                        thinkContent = thinkContent.substring(1);
                    }

                    // 记录样式信息
                    int thinkStart = processedPos;
                    processedText.append(thinkContent);
                    processedPos += thinkContent.length();

                    styles.add(new StyleInfo(thinkStart, processedPos, false, true, 0));

                    // 移动当前位置
                    currentPos = thinkEnd + 8; // 8是</think>的长度
                } else {
                    // 没有找到结束标记，直接添加当前字符
                    processedText.append(remainingText.charAt(currentPos));
                    currentPos++;
                    processedPos++;
                }
            }
            // 普通字符
            else {
                processedText.append(remainingText.charAt(currentPos));
                currentPos++;
                processedPos++;
            }
        }

        // 添加标题样式
        // 从后向前处理，以避免索引变化问题
        for (int i = headingStarts.size() - 1; i >= 0; i--) {
            int level = headingLevels.get(i);
            String content = headingContents.get(i);

            // 找到标题内容在处理后文本中的位置
            int start = processedText.indexOf(content);
            if (start != -1) {
                int end = start + content.length();

                // 添加标题样式
                styles.add(new StyleInfo(start, end, true, false, level));
            }
        }

        // 创建最终的SpannableString
        SpannableString spannableString = new SpannableString(processedText.toString());

        // 应用所有样式
        for (StyleInfo style : styles) {
            if (style.isBold || style.headingLevel > 0) {
                // 应用加粗样式
                spannableString.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    style.start,
                    style.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            if (style.isThink) {
                // 应用灰色小字体样式
                // 1. 设置字体大小为正常字体的0.9倍
                spannableString.setSpan(
                    new RelativeSizeSpan(0.9f),
                    style.start,
                    style.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // 2. 设置字体颜色为灰色
                spannableString.setSpan(
                    new ForegroundColorSpan(Color.GRAY),
                    style.start,
                    style.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            // 应用标题样式
            if (style.headingLevel > 0) {
                // 根据标题级别设置字体大小
                float textSize = 0;
                switch (style.headingLevel) {
                    case 1: textSize = 1.8f; break; // h1
                    case 2: textSize = 1.6f; break; // h2
                    case 3: textSize = 1.4f; break; // h3
                    case 4: textSize = 1.3f; break; // h4
                    case 5: textSize = 1.2f; break; // h5
                    case 6: textSize = 1.1f; break; // h6
                }

                if (textSize > 0) {
                    spannableString.setSpan(
                        new RelativeSizeSpan(textSize),
                        style.start,
                        style.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                }
            }

            // 应用数学表达式样式
            if (style.isMath) {
                // 设置斜体
                spannableString.setSpan(
                    new StyleSpan(Typeface.ITALIC),
                    style.start,
                    style.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // 设置颜色为深蓝色
                spannableString.setSpan(
                    new ForegroundColorSpan(Color.rgb(0, 0, 180)),
                    style.start,
                    style.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // 设置字体大小稍大
                spannableString.setSpan(
                    new RelativeSizeSpan(1.1f),
                    style.start,
                    style.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // 处理上标和下标
                if (style.mathContent != null) {
                    processMathSuperAndSubscripts(spannableString, style.mathContent, style.start);
                    processMathFractions(spannableString, style.mathContent, style.start);
                }
            }
        }

        return spannableString;
    }

    /**
     * 处理数学表达式中的上标和下标
     * 上标使用^符号，如x^2
     * 下标使用_符号，如x_1
     *
     * @param spannableString 要应用样式的SpannableString
     * @param mathContent 原始数学表达式内容（包含^和_符号）
     * @param startOffset 处理后的数学表达式在SpannableString中的起始位置
     */
    private static void processMathSuperAndSubscripts(SpannableString spannableString, String mathContent, int startOffset) {
        // 创建一个映射，记录原始文本中的位置到处理后文本中的位置的映射
        // 这个过程比较复杂，我们需要先分析原始文本中的上下标位置
        // 然后计算出这些位置在处理后文本中的对应位置

        // 分析上标
        List<int[]> superscriptRanges = new ArrayList<>(); // [原始开始位置, 原始结束位置, 处理后开始位置, 处理后结束位置]
        int currentPos = 0;
        while (currentPos < mathContent.length()) {
            // 查找上标符号
            int superscriptPos = mathContent.indexOf('^', currentPos);
            if (superscriptPos != -1 && superscriptPos < mathContent.length() - 1) {
                // 找到上标符号，获取上标内容
                char nextChar = mathContent.charAt(superscriptPos + 1);

                if (nextChar == '{') {
                    // 处理形如 x^{abc} 的情况
                    int bracketEnd = findClosingBracket(mathContent, superscriptPos + 1);
                    if (bracketEnd != -1) {
                        // 计算处理后文本中的位置
                        String beforeSuperscript = processMathSymbols(mathContent.substring(0, superscriptPos));
                        String superscriptContent = mathContent.substring(superscriptPos + 2, bracketEnd);

                        int processedStart = beforeSuperscript.length();
                        int processedEnd = processedStart + superscriptContent.length();

                        superscriptRanges.add(new int[]{superscriptPos + 2, bracketEnd, processedStart, processedEnd});

                        currentPos = bracketEnd + 1;
                    } else {
                        // 没有找到闭合括号，跳过
                        currentPos = superscriptPos + 2;
                    }
                } else {
                    // 处理形如 x^2 的情况
                    String beforeSuperscript = processMathSymbols(mathContent.substring(0, superscriptPos));

                    int processedStart = beforeSuperscript.length();
                    int processedEnd = processedStart + 1; // 单个字符

                    superscriptRanges.add(new int[]{superscriptPos + 1, superscriptPos + 2, processedStart, processedEnd});

                    currentPos = superscriptPos + 2;
                }
            } else {
                // 没有找到更多上标，退出循环
                break;
            }
        }

        // 分析下标
        List<int[]> subscriptRanges = new ArrayList<>(); // [原始开始位置, 原始结束位置, 处理后开始位置, 处理后结束位置]
        currentPos = 0;
        while (currentPos < mathContent.length()) {
            // 查找下标符号
            int subscriptPos = mathContent.indexOf('_', currentPos);
            if (subscriptPos != -1 && subscriptPos < mathContent.length() - 1) {
                // 找到下标符号，获取下标内容
                char nextChar = mathContent.charAt(subscriptPos + 1);

                if (nextChar == '{') {
                    // 处理形如 x_{abc} 的情况
                    int bracketEnd = findClosingBracket(mathContent, subscriptPos + 1);
                    if (bracketEnd != -1) {
                        // 计算处理后文本中的位置
                        String beforeSubscript = processMathSymbols(mathContent.substring(0, subscriptPos));
                        String subscriptContent = mathContent.substring(subscriptPos + 2, bracketEnd);

                        int processedStart = beforeSubscript.length();
                        int processedEnd = processedStart + subscriptContent.length();

                        subscriptRanges.add(new int[]{subscriptPos + 2, bracketEnd, processedStart, processedEnd});

                        currentPos = bracketEnd + 1;
                    } else {
                        // 没有找到闭合括号，跳过
                        currentPos = subscriptPos + 2;
                    }
                } else {
                    // 处理形如 x_2 的情况
                    String beforeSubscript = processMathSymbols(mathContent.substring(0, subscriptPos));

                    int processedStart = beforeSubscript.length();
                    int processedEnd = processedStart + 1; // 单个字符

                    subscriptRanges.add(new int[]{subscriptPos + 1, subscriptPos + 2, processedStart, processedEnd});

                    currentPos = subscriptPos + 2;
                }
            } else {
                // 没有找到更多下标，退出循环
                break;
            }
        }

        // 应用上标样式
        for (int[] range : superscriptRanges) {
            // 应用上标样式
            spannableString.setSpan(
                new SuperscriptSpan(),
                startOffset + range[2],
                startOffset + range[3],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // 设置上标字体大小较小
            spannableString.setSpan(
                new RelativeSizeSpan(0.7f),
                startOffset + range[2],
                startOffset + range[3],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // 应用下标样式
        for (int[] range : subscriptRanges) {
            // 应用下标样式
            spannableString.setSpan(
                new SubscriptSpan(),
                startOffset + range[2],
                startOffset + range[3],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // 设置下标字体大小较小
            spannableString.setSpan(
                new RelativeSizeSpan(0.7f),
                startOffset + range[2],
                startOffset + range[3],
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    /**
     * 查找闭合括号的位置
     *
     * @param text 要搜索的文本
     * @param openBracketPos 开括号的位置
     * @return 闭合括号的位置，如果没有找到则返回-1
     */
    private static int findClosingBracket(String text, int openBracketPos) {
        if (openBracketPos >= text.length() || text.charAt(openBracketPos) != '{') {
            return -1;
        }

        int bracketCount = 1;
        for (int i = openBracketPos + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                bracketCount++;
            } else if (c == '}') {
                bracketCount--;
                if (bracketCount == 0) {
                    return i;
                }
            }
        }

        return -1; // 没有找到匹配的闭合括号
    }

    /**
     * 处理数学表达式中的符号，去除上下标标记符号，处理分数表达式
     *
     * @param mathContent 原始数学表达式内容
     * @return 处理后的数学表达式内容
     */
    private static String processMathSymbols(String mathContent) {
        // 首先处理分数表达式
        String processedContent = processFractions(mathContent);

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < processedContent.length()) {
            char c = processedContent.charAt(i);

            if (c == '^' || c == '_') {
                // 遇到上标或下标符号
                if (i + 1 < processedContent.length()) {
                    char nextChar = processedContent.charAt(i + 1);

                    if (nextChar == '{') {
                        // 处理形如 x^{abc} 或 x_{abc} 的情况
                        int bracketEnd = findClosingBracket(processedContent, i + 1);
                        if (bracketEnd != -1) {
                            // 提取括号内的内容
                            String content = processedContent.substring(i + 2, bracketEnd);
                            result.append(content);
                            i = bracketEnd + 1;
                        } else {
                            // 没有找到闭合括号，跳过这个符号
                            i++;
                        }
                    } else {
                        // 处理形如 x^2 或 x_2 的情况
                        result.append(nextChar);
                        i += 2;
                    }
                } else {
                    // 符号在末尾，跳过
                    i++;
                }
            } else {
                // 普通字符，直接添加
                result.append(c);
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 处理分数表达式，将\frac{分子}{分母}转换为分子/分母
     *
     * @param content 原始内容
     * @return 处理后的内容
     */
    private static String processFractions(String content) {
        Matcher matcher = FRAC_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String numerator = matcher.group(1);
            String denominator = matcher.group(2);
            // 将\frac{分子}{分母}替换为分子/分母
            matcher.appendReplacement(sb, numerator + "/" + denominator);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 处理数学表达式中的分数
     *
     * @param spannableString 要应用样式的SpannableString
     * @param mathContent 原始数学表达式内容
     * @param startOffset 处理后的数学表达式在SpannableString中的起始位置
     */
    private static void processMathFractions(SpannableString spannableString, String mathContent, int startOffset) {
        Matcher matcher = FRAC_PATTERN.matcher(mathContent);

        while (matcher.find()) {
            String numerator = matcher.group(1);
            String denominator = matcher.group(2);

            // 计算分数在处理后文本中的位置
            String beforeFrac = processMathSymbols(mathContent.substring(0, matcher.start()));
            String fracText = numerator + "/" + denominator;

            int processedStart = beforeFrac.length() + startOffset;
            int processedEnd = processedStart + fracText.length();

            // 应用分数样式
            // 1. 设置字体颜色为深蓝色
            spannableString.setSpan(
                new ForegroundColorSpan(Color.rgb(0, 0, 180)),
                processedStart,
                processedEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // 2. 设置斜体
            spannableString.setSpan(
                new StyleSpan(Typeface.ITALIC),
                processedStart,
                processedEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // 3. 找到分数线（/）的位置
            int slashPos = processedStart + numerator.length();

            // 4. 设置分子为上标
            if (numerator.length() > 0) {
                spannableString.setSpan(
                    new SuperscriptSpan(),
                    processedStart,
                    slashPos,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // 设置分子字体大小较小
                spannableString.setSpan(
                    new RelativeSizeSpan(0.7f),
                    processedStart,
                    slashPos,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            // 5. 设置分母为下标
            if (denominator.length() > 0) {
                spannableString.setSpan(
                    new SubscriptSpan(),
                    slashPos + 1,
                    processedEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // 设置分母字体大小较小
                spannableString.setSpan(
                    new RelativeSizeSpan(0.7f),
                    slashPos + 1,
                    processedEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }

    /**
     * 创建数学表达式视图
     *
     * @param context 上下文
     * @param latex LaTeX表达式
     * @return 数学表达式视图
     */
    public static MathExpressionView createMathExpressionView(Context context, String latex) {
        MathExpressionView mathExpressionView = new MathExpressionView(context);
        mathExpressionView.setLatex(latex);
        return mathExpressionView;
    }

    /**
     * 从文本中提取数学表达式
     *
     * @param text 原始文本
     * @return 数学表达式列表
     */
    public static List<String> extractMathExpressions(String text) {
        List<String> expressions = new ArrayList<>();
        Matcher matcher = MATH_PATTERN.matcher(text);

        while (matcher.find()) {
            String expression = matcher.group(1);
            if (expression != null && !expression.isEmpty()) {
                expressions.add(expression);
            }
        }

        return expressions;
    }
}
