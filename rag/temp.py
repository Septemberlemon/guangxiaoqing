import time
from functools import wraps


def measure_time(func):
    """
    装饰器：测量函数运行时间
    """

    @wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        elapsed_time = end_time - start_time
        print(f"Function '{func.__name__}' executed in {elapsed_time:.6f} seconds")
        return result

    return wrapper


# 示例用法
@measure_time
def example_function(n):
    for i in range(n):
        print(i, flush=True)
        yield i


# 测试
if __name__ == "__main__":
    for i in example_function(10):
        pass
