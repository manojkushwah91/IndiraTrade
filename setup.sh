#!/bin/bash
# IndiraTrade Operations Console - Setup Script
# Usage: ./setup.sh

echo "=== IndiraTrade Operations Console Setup ==="

# Check Java version
echo "Checking Java version..."
java -version 2>&1 | head -1

# Check Maven
echo "Checking Maven..."
mvn -version 2>&1 | head -1

# Build the project
echo "Building project..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Build Successful ==="
    echo ""
    echo "To start the application:"
    echo "  mvn spring-boot:run"
    echo ""
    echo "Or run the JAR directly:"
    echo "  java -jar target/operations-console-1.0.0-SNAPSHOT.jar"
    echo ""
    echo "The application will start on: http://localhost:8080"
    echo ""
    echo "To run tests:"
    echo "  mvn test"
    echo ""
    echo "To run parser tests only:"
    echo "  mvn test -Dtest=ParserUnitTests"
    echo ""
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi
