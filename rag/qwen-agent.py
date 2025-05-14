import sys
from qwen_agent.agents import Assistant
from qwen_agent.utils.output_beautify import typewriter_print


def init_agent():
    llm_cfg = {
        "model": "qwen3:8b",
        "model_server": "http://localhost:11434/v1"
    }

    tools = [{
        "mcpServers": {
            "sqlite": {
                "command": "mcp-server-sqlite",
                "args": [
                    "--db-path",
                    "test.db"
                ]
            },
            "calculator": {
                "command": "mcp-server-calculator",
                "args": []
            },
            "knowledge_base": {
                "command": sys.executable,
                "args": ["knowledge_base_mcp_server.py"]
            },
            # "filesystem": {
            #     "command": "mcp-server-filesystem",
            #     "args": [
            #         "E:/Tools"
            #     ]
            # },
            "filesystem": {
                "command": "npx",
                "args": [
                    "-y",
                    "mcp-server-filesystem"
                    "E:/Tools"
                ]
            }
        }
    }]

    bot = Assistant(
        llm=llm_cfg,
        function_list=tools,
    )
    return bot


def run_query(query=None):
    bot = init_agent()
    messages = []  # 这里储存聊天历史。
    messages.append({'role': 'user', 'content': [{"text": query}]})
    previous_text = ""
    print("数据库管理员：", end="", flush=True)
    for response in bot.run(messages):
        previous_text, _ = typewriter_print(response, previous_text, show_detail=True)


if __name__ == '__main__':
    query = "解方程x^2+5=6x"
    run_query(query)
