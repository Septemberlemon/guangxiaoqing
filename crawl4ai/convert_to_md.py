import json


def jsonl_to_md(jsonl_file, md_file):
    with open(jsonl_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # 将多个 JSON 对象分割为独立的块
    json_blocks = content.strip().split('\n}\n{')
    json_blocks[0] = json_blocks[0] + '}'
    json_blocks[-1] = '{' + json_blocks[-1]
    for i in range(1, len(json_blocks) - 1):
        json_blocks[i] = '{' + json_blocks[i] + '}'

    with open(md_file, 'w', encoding='utf-8') as f:
        for block in json_blocks:
            try:
                obj = json.loads(block)
                question = obj.get('question', '').strip()
                answer = obj.get('answer', '').strip()
                if question and answer:
                    f.write(f"{question}\n{answer}\n\n")
            except json.JSONDecodeError:
                continue


# 示例用法
jsonl_to_md('outputs/cleaned_useful_info.jsonl', 'outputs/output.md')
