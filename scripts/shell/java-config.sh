# Add this to your ~/.zshrc for Java version management
# Misoto Indexer Project - Java 21 Configuration

# Java 21 for this project
export MISOTO_JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# Function to switch to Java 21 for this project
misoto_java() {
    export JAVA_HOME=$MISOTO_JAVA_HOME
    export PATH=$JAVA_HOME/bin:$PATH
    echo "Switched to Java 21 for Misoto Indexer project"
    java -version
}

# Alias for quick switching
alias mjava='misoto_java'
