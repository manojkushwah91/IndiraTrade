@echo off
REM IndiraTrade Operations Console - Setup Script (Windows)
REM Usage: setup.bat

echo === IndiraTrade Operations Console Setup ===

REM Check Java version
echo Checking Java version...
java -version 2>&1

REM Check Maven
echo Checking Maven...
mvn -version 2>&1

REM Build the project
echo Building project...
mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo === Build Successful ===
    echo.
    echo To start the application:
    echo   mvn spring-boot:run
    echo.
    echo Or run the JAR directly:
    echo   java -jar target\operations-console-1.0.0-SNAPSHOT.jar
    echo.
    echo The application will start on: http://localhost:8080
    echo.
    echo To run tests:
    echo   mvn test
    echo.
) else (
    echo Build failed. Please check the error messages above.
    exit /b 1
)
