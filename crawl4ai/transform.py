import json
import re


def parse_markdown(md_text: str) -> list[dict]:
    """
    将 Markdown 文本按照 `***` 分隔为多个问答段落，
    返回一个包含 {"instruction": ..., "input": ..., "output": ...} 的列表。
    """
    # 用三个或以上星号分割段落
    sections = re.split(r"^\*{3,}\s*$", md_text, flags=re.MULTILINE)
    qa_pairs = []
    for section in sections:
        lines = [line.strip() for line in section.splitlines() if line.strip()]
        if len(lines) < 2:
            continue
        instruction = lines[0]
        output = " ".join(lines[1:])
        qa_pairs.append({
            "instruction": instruction,
            "input": "",
            "output": output
        })
    return qa_pairs


def md_to_json(md_path: str, json_path: str) -> None:
    """
    读取 Markdown 文件，解析问答对，然后写入 JSON 文件（非 JSONL）。
    """
    with open(md_path, 'r', encoding='utf-8') as f:
        md_text = f.read()

    qa_data = parse_markdown(md_text)

    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(qa_data, f, ensure_ascii=False, indent=2)

    print(f"完成：共生成 {len(qa_data)} 条问答，已保存到 {json_path}")


# 示例用法：
md_to_json('outputs/manual_data.md', 'outputs/manual_data.json')
