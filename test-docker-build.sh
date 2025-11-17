#!/bin/bash
# Script để test Docker build local giống Render

echo "🧹 Cleaning previous builds..."
docker rmi movie-project-be-test 2>/dev/null || true

echo "🔨 Building Docker image..."
docker build -t movie-project-be-test .

if [ $? -eq 0 ]; then
    echo "✅ Build thành công!"
    echo "📦 Image: movie-project-be-test"
    echo "🚀 Để chạy: docker run -p 8080:8080 movie-project-be-test"
else
    echo "❌ Build thất bại!"
    exit 1
fi

