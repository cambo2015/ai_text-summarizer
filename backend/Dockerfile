FROM ubuntu:latest
LABEL authors="cameron hansen"
ENV DEBIAN_FRONTEND=noninteractive
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
RUN apt-get update && apt-get install -y\
    curl \
    wget \
    ca-certificates \
    gnupg \
    lsb-release \
    software-properties-common \
    openjdk-17-jdk \
    python3 \
    python3-pip\
    python3-dev \
    build-essential \
    supervisor \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install --no-cache-dir --break-system-packages llm

WORKDIR /app

COPY target/website-0.0.1-SNAPSHOT.jar app.jar

ENV USE_ONLINE=true
