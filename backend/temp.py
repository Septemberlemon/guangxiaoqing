@app.post("/chat/stream", response_model=None, status_code=200)
async def chat_stream(
        chat_req: ChatRequest,
        current_user: User = Depends(get_current_user)
):
    """
    把前端的 ChatRequest 转发到本地 RAG 服务的 /ask/stream，
    并以流式形式透传给前端。
    """
    # 记录请求开始时间和详细信息
    start_time = datetime.now()
    request_id = str(uuid.uuid4())[:8]  # 生成短ID用于日志跟踪
    print(
        f"[{request_id}] 收到聊天请求: time={start_time}, user={current_user.phone}, content={chat_req.content[:20]}...")

    # 1. 构造负载
    payload = {
        "question": chat_req.content,
        "history": [
            {"role": msg.role, "content": msg.content}
            for msg in (chat_req.history or [])
        ]
    }
    print(f"[{request_id}] 准备转发请求到RAG服务")

    # 2. 使用子进程调用curl来请求RAG服务
    async def iterate_response():
        nonlocal start_time, request_id
        print(f"[{request_id}] ===== 开始处理聊天响应 =====")

        try:
            # 使用requests库直接发送请求
            import requests

            # 设置更长的超时时间，以便处理长回答
            timeout = 60  # 60秒

            # 使用指定的RAG服务端点
            url = "http://localhost:6000/ask/stream"
            print(f"[{request_id}] 发送请求到RAG服务: {url}")

            # 发送请求
            response = requests.post(url, json=payload, timeout=timeout)

            # 检查响应状态
            if response.status_code == 200:
                print(f"[{request_id}] RAG服务返回成功")
                yield response.content
            else:
                print(f"[{request_id}] RAG服务返回错误: {response.status_code}")
                yield f"RAG服务返回错误: {response.status_code}".encode()

        except requests.exceptions.Timeout:
            print(f"[{request_id}] 连接RAG服务超时")
            yield "连接RAG服务超时".encode()

        except requests.exceptions.ConnectionError:
            print(f"[{request_id}] 无法连接到RAG服务")
            yield "无法连接到RAG服务".encode()

        except Exception as e:
            import traceback
            print(f"[{request_id}] 发生异常: {str(e)}")
            print(traceback.format_exc())
            yield f"发生异常: {str(e)}".encode()

    # 3. 返回 StreamingResponse
    print(f"[{request_id}] 创建StreamingResponse并返回")
    return StreamingResponse(
        iterate_response(),
        media_type="text/plain"
    )
