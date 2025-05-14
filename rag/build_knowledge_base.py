from pathlib import Path
from langchain_community.document_loaders import TextLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_ollama.embeddings import OllamaEmbeddings
from langchain_chroma.vectorstores import Chroma


def main():
    embed = OllamaEmbeddings(model="smartcreation/bge-large-zh-v1.5:Q5_K_M")
    splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)

    all_chunks = []

    for file in Path("./docs").glob("*.md"):
        loader = TextLoader(str(file), encoding="utf-8")
        docs = loader.load()
        chunks = splitter.split_documents(docs)

        for i, chunk in enumerate(chunks):
            chunk.metadata["source"] = str(file.name)  # 添加来源信息
            print(f"Chunk {i} length: {len(chunk.page_content)}")

        all_chunks.extend(chunks)

    Chroma.from_documents(
        all_chunks,
        embedding=embed,
        persist_directory="./chroma_store"
    )

    print("向量库已构建")


if __name__ == "__main__":
    main()
