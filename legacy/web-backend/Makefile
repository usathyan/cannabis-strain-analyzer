# Cannabis Strain Analysis & Matching System
# Makefile for simplified project management

.PHONY: clean install test run help

# Default target
help:
	@echo "🌿 Cannabis Strain Analysis & Matching System"
	@echo ""
	@echo "Available targets:"
	@echo "  install  - Install dependencies and create virtual environment"
	@echo "  run      - Start the web application"
	@echo "  test     - Run tests"
	@echo "  clean    - Clean up generated files and virtual environment"
	@echo ""

# Install dependencies and create virtual environment
install:
	@echo "🚀 Installing dependencies..."
	@if [ ! -d ".venv" ]; then \
		echo "Creating virtual environment..."; \
		uv venv --python python3; \
	fi
	@echo "Installing packages..."
	uv sync --dev
	@echo "✅ Installation complete!"
	@echo "Run 'make run' to start the application"

# Start the web application
run:
	@echo "🌿 Starting Cannabis Strain Analysis System..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	.venv/bin/python app.py

# Run tests and linting
test:
	@echo "🧪 Running tests and linting..."
	@if [ ! -d ".venv" ]; then \
		echo "❌ Virtual environment not found. Run 'make install' first."; \
		exit 1; \
	fi
	@echo "🔍 Running ruff linter..."
	.venv/bin/ruff check .
	@echo "🧪 Running pytest tests..."
	.venv/bin/python -m pytest tests/ -v

# Clean up generated files and virtual environment
clean:
	@echo "🧹 Cleaning up..."
	@rm -rf .venv
	@rm -f user_profiles.json
	@rm -f custom_strains.json
	@rm -f *.db
	@rm -f *.sqlite
	@rm -f *.sqlite3
	@rm -rf __pycache__
	@rm -rf .pytest_cache
	@find . -name "*.pyc" -delete
	@find . -name "*.pyo" -delete
	@find . -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true
	@echo "✅ Cleanup complete!"