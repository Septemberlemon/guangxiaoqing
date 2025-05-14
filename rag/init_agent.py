import sys
from qwen_agent.agents import Assistant


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
            "knowledge": {
                "command": sys.executable,
                "args": ["knowledge_base_mcp_server.py"]
            }
        }
    }]
    return Assistant(llm=llm_cfg, function_list=tools)
