#!/bin/bash

# Set Java 21 for this build
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "Using Java version:"
java -version

echo ""
echo "Building with Maven..."

# Clean and compile
mvn clean compile -DskipTests

echo ""
echo "Build completed!"
