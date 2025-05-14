import json
import re


input_file = "outputs/extracted_useful_info.jsonl"
output_file = "outputs/cleaned_useful_info.jsonl"

# 读入整个文件
with open(input_file, "r", encoding="utf-8") as fin:
    raw_text = fin.read()

# 用正则分割出每个对象（以 { 开头，以 } 结尾）
json_objects = re.findall(r'\{.*?\}', raw_text, re.DOTALL)

with open(output_file, "w", encoding="utf-8") as fout:
    for obj_str in json_objects:
        try:
            obj = json.loads(obj_str)
        except json.JSONDecodeError:
            continue  # 解析失败的跳过

        # 保留正确的question/answer
        if isinstance(obj, dict) and "question" in obj and "answer" in obj:
            fout.write(json.dumps(obj, ensure_ascii=False) + "\n")

print("清洗完成！")
