#!/bin/bash

echo "🚀 Setting up Misoto Codebase Indexer"
echo "====================================="

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama is not installed."
    echo "Please install Ollama from https://ollama.ai"
    echo "Or run: curl -fsSL https://ollama.ai/install.sh | sh"
    exit 1
fi

echo "✅ Ollama is installed"

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "⚠️  Ollama is not running. Starting Ollama..."
    echo "Please start Ollama manually and run this script again."
    echo "Run: ollama serve"
    exit 1
fi

echo "✅ Ollama is running"

# Pull CodeLlama model
echo "📥 Pulling CodeLlama:7b model..."
ollama pull codellama:7b

if [ $? -eq 0 ]; then
    echo "✅ CodeLlama:7b model downloaded successfully"
else
    echo "❌ Failed to download CodeLlama:7b model"
    exit 1
fi

# Check if .env file exists
if [ -f ".env" ]; then
    echo "✅ .env file found"
else
    echo "⚠️  .env file not found. Creating from template..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "📝 Please edit .env file with your Qdrant Cloud details:"
        echo "   QDRANT_HOST=your-cluster-url.qdrant.tech"
        echo "   QDRANT_API_KEY=your-qdrant-api-key"
    else
        echo "❌ .env.example template not found"
    fi
fi

echo ""
echo "🎉 Setup complete! You can now run:"
echo "   mvn spring-boot:run"
echo ""
echo "Available models:"
ollama list | grep codellama
