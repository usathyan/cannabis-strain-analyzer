.PHONY: help install sync run dev clean test lint format check

# Default target
help:
	@echo "Cannabis Strain Recommendation System"
	@echo ""
	@echo "Available targets:"
	@echo "  install        Install dependencies using UV"
	@echo "  sync           Sync dependencies with pyproject.toml"
	@echo "  run            Start the web server"
	@echo "  run-streamlined Start the streamlined strain analyzer"
	@echo "  dev            Start development server with auto-reload"
	@echo "  clean          Clean cache files and build artifacts"
	@echo "  test           Run tests"
	@echo "  lint           Run linting"
	@echo "  format         Format code"
	@echo "  check          Run all checks (lint + format)"
	@echo ""
	@echo "Examples:"
	@echo "  make install    # Install all dependencies"
	@echo "  make run        # Start production server"
	@echo "  make dev        # Start development server"

# Install dependencies using UV
install:
	@echo "📦 Installing dependencies with UV..."
	@if command -v uv >/dev/null 2>&1; then \
		uv venv .venv && \
		source .venv/bin/activate && \
		uv sync --extra dev; \
	else \
		echo "❌ UV not found. Install UV first:"; \
		echo "curl -LsSf https://astral.sh/uv/install.sh | sh"; \
		exit 1; \
	fi

# Sync dependencies
sync:
	@echo "🔄 Syncing dependencies..."
	@if command -v uv >/dev/null 2>&1; then \
		source .venv/bin/activate && \
		uv sync; \
	else \
		echo "❌ UV not found. Run 'make install' first."; \
		exit 1; \
	fi

# Start production server
run:
	@echo "🚀 Starting production server..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	.venv/bin/python web_interface.py

run-personalized:
	@echo "🚀 Starting personalized server with Google Auth..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	.venv/bin/python personalized_web_interface.py

run-demo:
	@echo "🚀 Starting demo personalized server..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	.venv/bin/python simple_personalized_interface.py

run-streamlined:
	@echo "🚀 Starting streamlined cannabis strain analyzer..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	.venv/bin/python run_streamlined.py

# Start development server with auto-reload
dev:
	@echo "🔧 Starting development server..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	.venv/bin/uvicorn web_interface:app --reload --host 127.0.0.1 --port 8000

# Clean cache files and build artifacts
clean:
	@echo "🧹 Cleaning up..."
	rm -rf .venv/
	rm -rf __pycache__/
	rm -rf .pytest_cache/
	rm -rf .mypy_cache/
	rm -rf cache/
	rm -rf custom_strains.json
	rm -rf *.pyc
	rm -rf *.pyo
	rm -rf dist/
	rm -rf build/
	rm -rf *.egg-info/
	find . -type d -name __pycache__ -exec rm -rf {} +
	find . -type f -name "*.pyc" -delete
	find . -type f -name "*.pyo" -delete

# Run tests
test:
	@echo "🧪 Running tests..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	source .venv/bin/activate && \
	pytest tests/ -v

# Run linting
lint:
	@echo "🔍 Running linter..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	source .venv/bin/activate && \
	flake8 . --count --select=E9,F63,F7,F82 --show-source --statistics && \
	flake8 . --count --exit-zero --max-complexity=10 --max-line-length=88 --statistics

# Format code
format:
	@echo "🎨 Formatting code..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	source .venv/bin/activate && \
	black . && \
	isort .

# Run all checks
check: lint format
	@echo "✅ All checks passed!"

# Setup for development
setup: clean install
	@echo "🎯 Development environment ready!"
	@echo "Run 'make run' to start the server"

# Docker targets (optional)
docker-build:
	@echo "🐳 Building Docker image..."
	docker build -t cannabis-recommendations .

docker-run:
	@echo "🐳 Running Docker container..."
	docker run -p 8000:8000 cannabis-recommendations

# Database management
reset-db:
	@echo "🔄 Resetting custom strains database..."
	rm -f custom_strains.json
	@echo "✅ Database reset complete"

# Show status
status:
	@echo "📊 Project Status:"
	@if [ -d ".venv" ]; then \
		echo "✅ Virtual environment: .venv"; \
	else \
		echo "❌ Virtual environment: Not found"; \
	fi
	@if [ -f "custom_strains.json" ]; then \
		strain_count=$$(python -c "import json; print(len(json.load(open('custom_strains.json'))))" 2>/dev/null || echo "0"); \
		echo "✅ Custom strains: $$strain_count saved"; \
	else \
		echo "ℹ️  Custom strains: None saved yet"; \
	fi
	@if command -v uv >/dev/null 2>&1; then \
		echo "✅ UV: Installed"; \
	else \
		echo "❌ UV: Not found"; \
	fi
