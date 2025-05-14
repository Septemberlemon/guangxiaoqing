from mcp.server.fastmcp import FastMCP


# 1. 创建 MCP Server 实例，名称可随意
mcp = FastMCP("DemoServer")


# 2. 添加一个简单的加法工具
@mcp.tool()
def add_two_num(a: int, b: int) -> int:
    """Add two numbers"""
    return a + b


# 4. 启动服务器 (可选，供 python server.py 直接运行)
if __name__ == "__main__":
    mcp.run()
