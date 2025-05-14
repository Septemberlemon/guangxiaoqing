import json
import random


# —— 配置区域 ——
raw_file = "outputs/cleaned_useful_info.jsonl"  # 你的原始文件（可能多余大括号、重复字段、缺逗号……）
clean_file = "outputs/temp.jsonl"  # 中间产物：只保留解析成功的 JSON 对象
train_file = "outputs/train.jsonl"  # 拆分后训练集
val_file = "outputs/val.jsonl"  # 拆分后验证集
val_ratio = 0.1  # 验证集比例
seed = 42  # 随机种子，保证结果可复现

# —— 第一步：清洗，提取所有“成对大括号”内的 JSON 对象 ——
with open(raw_file, 'r', encoding='utf-8') as fin, \
        open(clean_file, 'w', encoding='utf-8') as fout:
    buffer = ""
    depth = 0
    for line in fin:
        if '{' in line:
            depth += line.count('{')
        if depth > 0:
            buffer += line
        if '}' in line:
            depth -= line.count('}')
        # 当 depth 回到 0 时，尝试解析一整个对象
        if depth == 0 and buffer.strip():
            try:
                obj = json.loads(buffer)
                # 解析成功才写入 clean_file
                fout.write(json.dumps(obj, ensure_ascii=False) + "\n")
            except json.JSONDecodeError:
                # 解析失败就跳过，不抛错误
                pass
            buffer = ""

# —— 第二步：读取 clean_file，打乱 & 按比例拆分 ——
with open(clean_file, 'r', encoding='utf-8') as f:
    lines = [l for l in f if l.strip()]

random.seed(seed)
random.shuffle(lines)

n_val = int(len(lines) * val_ratio)
val_lines = lines[:n_val]
train_lines = lines[n_val:]

with open(train_file, 'w', encoding='utf-8') as f:
    f.writelines(train_lines)
with open(val_file, 'w', encoding='utf-8') as f:
    f.writelines(val_lines)

print(f"清洗后样本: {len(lines)}，训练集: {len(train_lines)}，验证集: {len(val_lines)}")
