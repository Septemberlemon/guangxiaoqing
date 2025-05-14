import re


def extract_url_and_name(text):
    url_list = []
    lines = text.split('\n')
    for line in lines:
        line = line.strip()
        if line.startswith('* '):
            match = re.search(r'\[(.*?)\]\((.*?)\)', line)
            if match:
                name = match.group(1)
                url = match.group(2)
                url_list.append((name, url))
    return url_list


# 输入文本
text = """
* [学校首页](https://www.gdip.edu.cn/)

* [人工智能学院](https://ict.gdqy.edu.cn/)
* [管理学院](https://bus.gdqy.edu.cn)
* [航天北斗技术学院](https://qiche.gdqy.edu.cn)
* [材料学院](https://qhx.gdqy.edu.cn)
* [生态环境技术学院](https://hj.gdqy.edu.cn)
* [智能制造与装备学院](https://jdx.gdqy.edu.cn)
* [财贸学院](https://economy.gdqy.edu.cn)
* [继续教育学院](https://jxjyxy.gdqy.edu.cn)
* [生命健康学院](https://sp.gdqy.edu.cn)
* [艺术设计学院](https://yssj.gdqy.edu.cn)
* [应用外语学院](https://wyxy.gdqy.edu.cn)
* [马克思主义学院](https://mks.gdqy.edu.cn/)
* [创业学院](https://cyxy.gdqy.edu.cn/)
* [中职教育部](https://zzb.gdqy.edu.cn)

* [教师中心](https://cfd.gdqy.edu.cn)

* [校友会](https://xy.gdip.edu.cn)
* [学生会](https://stu.gdqy.edu.cn)
* [校团委](https://cyl.gdqy.edu.cn/)

* [招生](https://zs.gdip.edu.cn/)
* [就业](https://jy.gdip.edu.cn)
* [创新创业](https://cyxy.gdqy.edu.cn/)

* [图书馆](https://lib.gdqy.edu.cn/)

* [创新强校工程](https://cqgc.gdqy.edu.cn)
* [一带一路](https://ydyl.gdqy.edu.cn/)
"""

# 提取 URL 和名称
url_list = extract_url_and_name(text)

if __name__ == '__main__':
    # 打印结果
    for item in url_list:
        print(item)
