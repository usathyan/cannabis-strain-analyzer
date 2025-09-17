# 🌿 Cannabis Strain Recommendation System

**Simple web interface for cannabis strain analysis and dispensary recommendations based on terpene profiles.**

[![Python](https://img.shields.io/badge/python-3.11+-blue)](https://python.org)

## ✨ What This Does

A simple web application that helps users:

- **Enter their favorite strain** and get detailed terpene analysis
- **Find similar strains** based on terpene profile matching
- **Locate nearby dispensaries** with configurable search radius
- **Get AI-powered recommendations** using LangGraph ReAct agent

## 🔐 Personalized Features (NEW!)

The personalized version adds Google OAuth authentication and user profiles:

- **Google Authentication** - Secure login with Google accounts
- **Personal Profiles** - Store name, address, and favorite strains
- **Terpene Analysis** - Generate detailed terpene profiles for your favorites
- **Personalized Ratings** - Compare any strain against your preferences
- **Smart Recommendations** - Get recommendations tailored to your taste
- **Configuration Page** - Manage your profile and view terpene analysis

## 🚀 Quick Start

### 1. Install UV (Package Manager)

```bash
# Install UV
curl -LsSf https://astral.sh/uv/install.sh | sh

# Or using pip
pip install uv
```

### 2. Install Dependencies

```bash
# Using Makefile (recommended)
make install

# Or manually
uv venv .venv
source .venv/bin/activate
uv sync --extra dev

# Activate virtual environment
source .venv/bin/activate
```

### 3. Start the Web Server

#### Basic Version (No Authentication)
```bash
# Using Makefile
make run

# Or directly
source .venv/bin/activate
python web_interface.py
```

#### Personalized Version (With Google Auth)
```bash
# Setup authentication first
python3 setup_auth.py

# Follow the printed instructions to configure Google OAuth
# Then run the personalized version
make run-personalized
```

#### Demo Personalized Version (No Auth Required)
```bash
# Run the demo version (no Google OAuth setup needed)
make run-demo
```

### 4. Open Your Browser

- **Basic App**: http://localhost:8000
- **Personalized App**: http://localhost:8000 (after running `make run-personalized`)
- **Demo Personalized App**: http://localhost:8000 (after running `make run-demo`)
- **API Documentation**: http://localhost:8000/docs

## 🧪 Testing Your Installation

After running `make install`, test that everything works:

```bash
# Check project status
make status

# Test server startup
make run

# Open http://localhost:8000 in your browser
# Try searching for "Blueberry Thai" or "Granddaddy Purple"

## 📝 Migration Notes

- **✅ Migrated to UV**: Dependencies now managed via `pyproject.toml`
- **✅ Modern Python tooling**: Black, isort, flake8 configured
- **✅ Professional structure**: Makefile, proper virtual environment
- **📝 Legacy cleanup**: You can safely delete `requirements.txt` after confirming everything works

## 🛠️ Available Commands

### Makefile Targets
```bash
make install     # Install dependencies with UV
make sync        # Sync dependencies
make run         # Start production server
make dev         # Start development server with auto-reload
make clean       # Clean cache files and build artifacts
make test        # Run tests
make lint        # Run linting
make format      # Format code with Black and isort
make check       # Run all checks (lint + format)
make status      # Show project status
```

### Direct Commands
```bash
# Activate virtual environment
source .venv/bin/activate

# Start server
python web_interface.py

# Development server with auto-reload
uvicorn web_interface:app --reload --host 127.0.0.1 --port 8000
```

## 📱 Usage

1. **Enter a strain name** (e.g., "Granddaddy Purple")
2. **Add your location** (optional, for dispensary search)
3. **Set search radius** (10, 25, or 50 miles)
4. **Click "Get Recommendations"**

The system will:
- Analyze your strain's terpene profile
- Find similar strains based on terpene matching
- Show nearby dispensaries (if location provided)

## 🏗️ Architecture

### Core Components

1. **Web Interface** (`web_interface.py`)
   - FastAPI web server with simple HTML frontend
   - Single form for strain input and location
   - Displays results with strain analysis and recommendations

2. **Strain Database** (`enhanced_strain_database.py`)
   - Stores comprehensive terpene profiles for 10+ strains
   - Simple similarity calculation based on terpene matching
   - No complex ML algorithms - just basic cosine similarity

3. **LangGraph Agent** (`langgraph_agent.py`)
   - Simple ReAct agent with 3 tools: analysis, recommendation, dispensary
   - Routes user queries to appropriate tools
   - Minimal workflow with router → orchestrator → tools

4. **Location Service** (`google_maps_integration.py`)
   - Finds dispensaries near user location
   - Configurable search radius (10, 25, 50 miles)
   - Simple mock data for demonstration

### Data Flow

1. User enters strain name and location
2. LangGraph agent processes the query
3. Agent calls analysis tool to get strain data
4. Agent calls recommendation tool for similar strains
5. Agent calls dispensary tool for nearby locations
6. Results displayed in web interface

## 🧬 Terpene Analysis

The system analyzes strains based on these key terpenes:

- **Myrcene**: Relaxing, sedating effects
- **Caryophyllene**: Anti-inflammatory, stress relief
- **Pinene**: Alertness, memory retention
- **Limonene**: Mood elevation, stress relief
- **Linalool**: Calming, sedating
- **Humulene**: Appetite suppressant, anti-inflammatory
- **Terpinolene**: Uplifting, energetic

## 🔧 Configuration

### Environment Variables

```bash
# Required for LangGraph agent
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=gemma3:latest

# Optional for location services
GOOGLE_MAPS_API_KEY=your_key_here
```

## 📄 License

This project is licensed under the MIT License.

---

**🌿 Simple, focused cannabis strain recommendations!**