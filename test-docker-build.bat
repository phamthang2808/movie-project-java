@echo off
REM Script để test Docker build local giống Render (Windows)

echo Cleaning previous builds...
docker rmi movie-project-be-test 2>nul

echo Building Docker image...
docker build -t movie-project-be-test .

if %ERRORLEVEL% EQU 0 (
    echo Build thanh cong!
    echo Image: movie-project-be-test
    echo De chay: docker run -p 8080:8080 movie-project-be-test
) else (
    echo Build that bai!
    exit /b 1
)

