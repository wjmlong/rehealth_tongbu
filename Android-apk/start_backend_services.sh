#!/bin/bash
# ReHealth Backend Services Startup Script
# Generated: 2026-07-20

set -e

echo "================================"
echo "ReHealth Backend Services"
echo "================================"
echo ""

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration
BACKEND_DIR="/mnt/d/rehealthAI/rehealth-algorithms"
BACKEND_PORT=8080
MODEL_PORT=8000

echo "Configuration:"
echo "  Backend Dir: $BACKEND_DIR"
echo "  Backend Port: $BACKEND_PORT (JeecgBoot)"
echo "  Model Port: $MODEL_PORT (FastAPI)"
echo ""

#############################################
# Check Prerequisites
#############################################

echo "Checking prerequisites..."

# Check Python
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 not found${NC}"
    exit 1
fi
PYTHON_VERSION=$(python3 --version)
echo -e "${GREEN}✓${NC} $PYTHON_VERSION"

# Check if backend directory exists
if [ ! -d "$BACKEND_DIR" ]; then
    echo -e "${RED}Error: Backend directory not found: $BACKEND_DIR${NC}"
    exit 1
fi
echo -e "${GREEN}✓${NC} Backend directory found"

# Check if port is available
if lsof -Pi :$MODEL_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠${NC} Port $MODEL_PORT already in use"
    PID=$(lsof -Pi :$MODEL_PORT -sTCP:LISTEN -t)
    echo "  Process: $(ps -p $PID -o comm=)"
    read -p "Kill existing process? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kill $PID
        sleep 2
    else
        echo "Exiting..."
        exit 1
    fi
fi

#############################################
# Start Model Service (FastAPI)
#############################################

echo ""
echo "================================"
echo "Starting Model Service (FastAPI)"
echo "================================"

cd "$BACKEND_DIR"

# Check if virtual environment exists
if [ ! -d "venv" ] && [ ! -d ".venv" ]; then
    echo -e "${YELLOW}⚠${NC} Virtual environment not found, creating..."
    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
else
    if [ -d "venv" ]; then
        source venv/bin/activate
    else
        source .venv/bin/activate
    fi
fi

echo -e "${GREEN}✓${NC} Virtual environment activated"

# Check if requirements are installed
if ! python -c "import fastapi" 2>/dev/null; then
    echo -e "${YELLOW}⚠${NC} Installing requirements..."
    pip install -r requirements.txt
fi

# Start FastAPI server
echo ""
echo "Starting FastAPI server on port $MODEL_PORT..."
echo "API will be available at: http://localhost:$MODEL_PORT"
echo ""

# Check which API file to use
if [ -f "api/main.py" ]; then
    API_MODULE="api.main:app"
elif [ -f "main.py" ]; then
    API_MODULE="main:app"
else
    echo -e "${RED}Error: Could not find main.py${NC}"
    exit 1
fi

# Start in background
nohup uvicorn $API_MODULE --host 0.0.0.0 --port $MODEL_PORT --reload > /tmp/rehealth_model_service.log 2>&1 &
MODEL_PID=$!

echo -e "${GREEN}✓${NC} Model service started (PID: $MODEL_PID)"
echo "  Logs: /tmp/rehealth_model_service.log"

# Wait for service to be ready
echo ""
echo "Waiting for service to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:$MODEL_PORT/docs > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Model service is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}Error: Service failed to start${NC}"
        echo "Check logs: tail -f /tmp/rehealth_model_service.log"
        exit 1
    fi
    sleep 1
    echo -n "."
done

#############################################
# Service Information
#############################################

echo ""
echo "================================"
echo "Services Running"
echo "================================"
echo ""
echo "Model Service (FastAPI):"
echo "  URL: http://localhost:$MODEL_PORT"
echo "  Docs: http://localhost:$MODEL_PORT/docs"
echo "  PID: $MODEL_PID"
echo "  Logs: tail -f /tmp/rehealth_model_service.log"
echo ""
echo "From Android emulator:"
echo "  URL: http://10.0.2.2:$MODEL_PORT"
echo ""

#############################################
# Note about JeecgBoot Backend
#############################################

echo "================================"
echo "JeecgBoot Backend (Java)"
echo "================================"
echo ""
echo -e "${YELLOW}Note:${NC} JeecgBoot backend needs to be started separately."
echo "It requires Java and a separate setup process."
echo ""
echo "Expected URL: http://localhost:$BACKEND_PORT/jeecg-boot"
echo "From emulator: http://10.0.2.2:$BACKEND_PORT/jeecg-boot"
echo ""

#############################################
# Keep Running
#############################################

echo "================================"
echo "Services Active"
echo "================================"
echo ""
echo "Press Ctrl+C to stop services"
echo ""

# Trap Ctrl+C
trap 'echo ""; echo "Stopping services..."; kill $MODEL_PID 2>/dev/null; echo "Done."; exit 0' INT

# Keep script running
while true; do
    sleep 5
    # Check if service is still running
    if ! kill -0 $MODEL_PID 2>/dev/null; then
        echo -e "${RED}Error: Model service stopped unexpectedly${NC}"
        echo "Check logs: tail -f /tmp/rehealth_model_service.log"
        exit 1
    fi
done
