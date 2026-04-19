FROM pgvector/pgvector:pg15

# 安装编译工具
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    unzip \
    postgresql-server-dev-15 \
    && rm -rf /var/lib/apt/lists/*

# 设置 pg_config 路径
ENV PATH="/usr/lib/postgresql/15/bin:${PATH}"

# 复制本地下载的源码
COPY pg_jieba.zip /tmp/pg_jieba.zip
COPY cppjieba.zip /tmp/cppjieba.zip
COPY limonp.zip /tmp/limonp.zip

# 解压、构建目录结构、编译 pg_jieba（全部在一个 RUN 中）
RUN unzip /tmp/pg_jieba.zip -d /tmp \
    && unzip /tmp/cppjieba.zip -d /tmp \
    && unzip /tmp/limonp.zip -d /tmp \
    && rm -rf /tmp/pg_jieba-master/libjieba \
    && mv /tmp/cppjieba-master /tmp/pg_jieba-master/libjieba \
    && rm -rf /tmp/pg_jieba-master/libjieba/deps/limonp \
    && mv /tmp/limonp-master /tmp/pg_jieba-master/libjieba/deps/limonp \
    && cd /tmp/pg_jieba-master \
    && cmake \
        -DPostgreSQL_TYPE_INCLUDE_DIR=/usr/include/postgresql/15/server \
        -DPostgreSQL_INCLUDE_DIRS=/usr/include/postgresql \
        . \
    && make \
    && make install \
    && rm -rf /tmp/*.zip /tmp/pg_jieba-* /tmp/cppjieba-* /tmp/limonp-* \
    && apt-get purge -y build-essential cmake unzip \
    && apt-get autoremove -y

# 设置环境变量
ENV POSTGRES_DB=admindb
ENV POSTGRES_USER=adminuser
ENV POSTGRES_PASSWORD=adminpass123

# 初始化脚本：创建扩展
COPY init.sql /docker-entrypoint-initdb.d/

EXPOSE 5432