from mcp.server.fastmcp import FastMCP
from typing import List, Dict, Union

from langchain_chroma.vectorstores import Chroma
from langchain_ollama.embeddings import OllamaEmbeddings


mcp = FastMCP("KnowledgeBase")

# 加载向量库
embed = OllamaEmbeddings(model="smartcreation/bge-large-zh-v1.5:Q5_K_M")
vs = Chroma(persist_directory="./chroma_store", embedding_function=embed)


@mcp.tool()
def search_by_question(question: r, stkeyword: str) -> List[Dict[str, Union[str, float]]]:
    """
    根据问题查询学校网站数据数据，并根据关键词查询学生手册
    注意保持keyword和question的意义相近
    参数:
      - question: 问题
      - keyword: 关键词

    返回: 匹配内容块列表，每条包含 source, page_content, score
    """
    # 进行相似度检索
    docs_in_web = vs.similarity_search_with_score(
        question,
        k=3,
        filter={"source": "学校网站数据.md"}
    )
    docs_in_handhook = vs.similarity_search_with_score(
        keyword,
        k=3,
        filter={"source": "广东轻工职业技术大学学生手册.md"})

    def select_top(documents, keyword):
        """
        在每个检索结果集中选出 1 个最佳片段：
        - 优先选择包含 keyword 的片段；
        - 如果没有包含 keyword 的片段，则选择分数最高者。
        """
        # 1. 找出包含 keyword 的片段
        keyword_matches = [(d, score) for d, score in documents if keyword in d.page_content]

        # 2. 优先返回包含 keyword 的片段
        if keyword_matches:
            best_match = max(keyword_matches, key=lambda d: d[1])
            return {
                "source": best_match[0].metadata["source"],
                "page_content": best_match[0].page_content,
                "score": best_match[1]
            }

        # 3. 如果没有 keyword 匹配，则按分数最高者返回
        if documents:
            best_match, best_score = max(documents, key=lambda x: x[1])
            return {
                "source": best_match.metadata.get("source", ""),
                "page_content": best_match.page_content,
                "score": best_score
            }

        return None

    # 挑选每个检索集中的最佳片段
    best_in_web = select_top(docs_in_web, keyword)
    best_in_handhook = select_top(docs_in_handhook, keyword)

    # 合并结果
    results = []
    if best_in_web:
        results.append(best_in_web)
    if best_in_handhook:
        results.append(best_in_handhook)

    return results


if __name__ == "__main__":
    mcp.run()
