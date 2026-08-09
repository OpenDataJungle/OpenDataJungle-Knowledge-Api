FROM postgres:18

RUN apt-get update && \
    apt-get install -y build-essential git postgresql-server-dev-18 && \
    git clone --depth 1 https://github.com/pgvector/pgvector.git /pgvector && \
    cd /pgvector && make && make install && \
    rm -rf /pgvector && \
    apt-get remove --purge -y git build-essential && \
    apt-get autoremove -y && apt-get clean
