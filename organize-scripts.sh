#!/bin/bash

# Script to organize all script files into the scripts/ folder structure
# This will move sh, py, cmd, bat files from the root directory to proper locations

echo "🗂️ Organizing script files into scripts/ folder structure..."

# Create additional subdirectories if needed
mkdir -p scripts/python
mkdir -p scripts/shell
mkdir -p scripts/batch
mkdir -p scripts/cmd

echo "📁 Created script organization directories"

# Move shell scripts (.sh files) from root
echo "🐚 Moving shell scripts..."
for file in *.sh; do
    if [ -f "$file" ]; then
        echo "   Moving $file → scripts/shell/"
        mv "$file" scripts/shell/
    fi
done

# Move Python scripts (.py files) from root
echo "🐍 Moving Python scripts..."
for file in *.py; do
    if [ -f "$file" ]; then
        echo "   Moving $file → scripts/python/"
        mv "$file" scripts/python/
    fi
done

# Move batch scripts (.bat files) from root
echo "🪟 Moving batch scripts..."
for file in *.bat; do
    if [ -f "$file" ]; then
        echo "   Moving $file → scripts/batch/"
        mv "$file" scripts/batch/
    fi
done

# Move cmd scripts (.cmd files) from root - but skip mvnw.cmd as it's a Maven wrapper
echo "⚙️ Moving command scripts..."
for file in *.cmd; do
    if [ -f "$file" ] && [ "$file" != "mvnw.cmd" ]; then
        echo "   Moving $file → scripts/cmd/"
        mv "$file" scripts/cmd/
    fi
done

# Special handling for mvnw.cmd - keep it in root as it's a Maven wrapper
if [ -f "mvnw.cmd" ]; then
    echo "   Keeping mvnw.cmd in root (Maven wrapper)"
fi

echo ""
echo "✅ Script organization complete!"
echo ""
echo "📊 Current scripts/ structure:"
echo "scripts/"
echo "├── README.md"
echo "├── setup/"
echo "├── test/"
echo "├── utils/"
echo "├── python/     # Python scripts (.py)"
echo "├── shell/      # Shell scripts (.sh)"
echo "├── batch/      # Batch scripts (.bat)"
echo "└── cmd/        # Command scripts (.cmd)"
echo ""
echo "💡 Note: mvnw.cmd remains in root directory as it's a Maven wrapper"
