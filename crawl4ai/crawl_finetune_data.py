import os
import json
import asyncio
from crawl4ai import (
    AsyncWebCrawler,
    BrowserConfig,
    CrawlerRunConfig,
    LLMConfig,
)
from crawl4ai.deep_crawling import BFSDeepCrawlStrategy  # 用于深度爬取
from crawl4ai.extraction_strategy import LLMExtractionStrategy  # 用于网页信息提取
from pydantic import BaseModel, Field

from url_list import url_list


# 定义更通用的抽取Schema
class UsefulInfo(BaseModel):
    question: str = Field(..., description="学生可能会问的问题")
    answer: str = Field(..., description="针对该问题的准确答案")


async def main(url, depth):
    bfs_strategy = BFSDeepCrawlStrategy(
        max_depth=depth,
        max_pages=1000,
        include_external=False,
    )
    gemini_config = LLMConfig(  # llm配置
        provider="gemini/gemini-2.0-flash",
        api_token="AIzaSyA04oM-VYa65jM2H1ufDfe7J7MBB-m_zFE"
    )
    llm_strategy = LLMExtractionStrategy(
        llm_config=gemini_config,
        schema=UsefulInfo.model_json_schema(),  # Or use model_json_schema()
        extraction_type="schema",
        instruction=(
            "请用中文从网页内容中提取一些问答对。"
            "要提取在校学生和报考学生关心的信息，数量不要求多，但是质量要高。"
            "尽量不要返回带有url的内容，而是将网页正文中对应的答案内容提取出来。"
            "如果页面整体是一条新闻，返回空值即可。"
            "当你谈到学院时，需要指明是哪个学院"
            "每条输出应包含："
            "1. question（学生可能提问的自然语言问句）；"
            "2. answer（针对该问题的简洁、准确的中文答案）；"
        ),
        chunk_token_threshold=1000,
        overlap_rate=0.0,
        apply_chunking=True,
        input_format="markdown",  # or "html", "fit_markdown"
        extra_args={"temperature": 0.0, "max_tokens": 800}
    )
    browser_cfg = BrowserConfig(headless=True)  # 定义browser配置
    crawler_cfg = CrawlerRunConfig(  # 定义crawler配置
        deep_crawl_strategy=bfs_strategy,
        stream=True,
        extraction_strategy=llm_strategy,
    )

    os.makedirs("E:/Temp/outputs", exist_ok=True)
    output_file_path = "E:/Temp/outputs/extracted_useful_info.jsonl"
    num_extracted_results = 0
    async with AsyncWebCrawler(config=browser_cfg) as crawler:
        async for result in await crawler.arun(url, config=crawler_cfg):
            if result.url in visited_urls:
                continue
            visited_urls.add(result.url)
            if result.extracted_content:
                print(result.extracted_content)
                num_extracted_results += 1
                parsed_content = json.loads(result.extracted_content)
                # 移除每个对象中的error字段
                for item in parsed_content:
                    del item["error"]
                with open(output_file_path, "a", encoding="utf-8") as f:
                    for info in parsed_content:
                        f.write(json.dumps(info, ensure_ascii=False) + "\n")


if __name__ == '__main__':
    visited_urls = set()
    for name, url in url_list:
        asyncio.run(main(url, depth=0))
    visited_urls = set(url for _, url in url_list)
    for name, url in url_list:
        print(f"开始爬取{name}")
        asyncio.run(main(url, depth=1))
        print(f"{name}爬取结束")
