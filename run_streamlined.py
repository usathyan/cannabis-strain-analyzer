#!/usr/bin/env python3
"""
Startup script for the streamlined cannabis strain analyzer
"""

import os
import sys
from pathlib import Path

# Add the current directory to Python path
sys.path.insert(0, str(Path(__file__).parent))

def main():
    print("🌿 Starting Cannabis Strain Analyzer (Streamlined)")
    print("=" * 50)
    
    # Check if .env file exists
    env_file = Path(".env")
    if not env_file.exists():
        print("⚠️  No .env file found. Please run setup_auth.py first.")
        print("   This will create the necessary environment configuration.")
        return
    
    # Import and run the app
    try:
        from streamlined_app import app
        import uvicorn
        
        print("✅ Starting server on http://localhost:8000")
        print("📱 Open your browser and navigate to the URL above")
        print("🔐 Make sure you have configured Google OAuth credentials in .env")
        print("\nPress Ctrl+C to stop the server")
        print("=" * 50)
        
        uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
        
    except ImportError as e:
        print(f"❌ Import error: {e}")
        print("   Make sure all dependencies are installed:")
        print("   uv sync")
    except Exception as e:
        print(f"❌ Error starting server: {e}")

if __name__ == "__main__":
    main()
