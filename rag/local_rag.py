# local_rag.py
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List, Dict
from init_agent import init_agent
from qwen_agent.utils.output_beautify import typewriter_print
import datetime


class RAGRequest(BaseModel):
    question: str
    history: List[Dict[str, str]] = []


app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"]
)

agent = init_agent()
SYS_PROMPT = "你是广小轻，是广东轻工职业技术大学研发的聊天机器人。注意数据库中只有学生表。"


async def generate_response(req: RAGRequest):
    messages = [{"role": "system", "content": [{"text": SYS_PROMPT}]}]
    # 丢弃首条历史，从第二条开始
    for msg in req.history[1:]:
        messages.append({"role": msg.get("role"), "content": [{"text": msg.get("content")}]})
    messages.append({"role": "user", "content": [{"text": req.question}]})

    previous = ""
    for chunk in agent.run(messages):
        previous, new_text = typewriter_print(chunk, previous, False)
        yield new_text.encode()
    # 结束流
    yield b""


@app.post("/ask/stream")
async def ask_stream(req: RAGRequest):
    print(f"RAG 服务接收提问: {req.question}")
    print(datetime.datetime.now())
    return StreamingResponse(
        generate_response(req),
        media_type="text/plain"
    )


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=11435, log_level="info")
