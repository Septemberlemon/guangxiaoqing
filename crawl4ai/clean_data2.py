import json


valid = []
with open("outputs/train.jsonl", encoding="utf-8") as fin:
    for line in fin:
        try:
            obj = json.loads(line)
        except json.JSONDecodeError:
            continue  # 跳过解析失败的行
        # 只保留 question/answer 都存在且为 str 的记录
        if (
                isinstance(obj.get("question"), str)
                and isinstance(obj.get("answer"), str)
        ):
            valid.append(obj)

with open("outputs/train_clean.jsonl", "w", encoding="utf-8") as fout:
    for obj in valid:
        fout.write(json.dumps(obj, ensure_ascii=False) + "\n")
