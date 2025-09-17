#!/usr/bin/env python3
"""
Setup script for Google OAuth authentication
"""

import os
import secrets
from pathlib import Path

def generate_secret_key():
    """Generate a secure secret key"""
    return secrets.token_urlsafe(32)

def create_env_file():
    """Create .env file with default values"""
    env_file = Path(".env")
    
    if env_file.exists():
        print("⚠️  .env file already exists. Backing up to .env.backup")
        env_file.rename(".env.backup")
    
    secret_key = generate_secret_key()
    
    env_content = f"""# Google OAuth Configuration
# Get these from https://console.developers.google.com/
GOOGLE_CLIENT_ID=your-google-client-id-here
GOOGLE_CLIENT_SECRET=your-google-client-secret-here

# Secret key for JWT tokens (auto-generated)
SECRET_KEY={secret_key}

# Database URL (SQLite for development, PostgreSQL for production)
DATABASE_URL=sqlite:///./cannabis_app.db

# Ollama configuration (if using local LLM)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:latest

# Google Maps API (optional, for real dispensary data)
GOOGLE_MAPS_API_KEY=your-google-maps-api-key-here
"""
    
    with open(env_file, 'w') as f:
        f.write(env_content)
    
    print(f"✅ Created .env file with generated secret key")
    return secret_key

def print_setup_instructions():
    """Print setup instructions for Google OAuth"""
    print("\n" + "="*60)
    print("🔐 GOOGLE OAUTH SETUP INSTRUCTIONS")
    print("="*60)
    print()
    print("1. Go to https://console.developers.google.com/")
    print("2. Create a new project or select an existing one")
    print("3. Enable the Google+ API")
    print("4. Go to 'Credentials' and create 'OAuth 2.0 Client IDs'")
    print("5. Set the following redirect URIs:")
    print("   - http://localhost:8000/auth/google/callback")
    print("   - http://127.0.0.1:8000/auth/google/callback")
    print("6. Copy your Client ID and Client Secret")
    print("7. Update the .env file with your credentials")
    print()
    print("📝 Example .env file:")
    print("GOOGLE_CLIENT_ID=123456789-abcdefghijklmnop.apps.googleusercontent.com")
    print("GOOGLE_CLIENT_SECRET=GOCSPX-abcdefghijklmnopqrstuvwxyz")
    print()
    print("🚀 After setup, run: make run-personalized")
    print("="*60)

def main():
    print("🌿 Setting up Cannabis Recommendation App with Google Auth")
    print()
    
    # Create .env file
    secret_key = create_env_file()
    
    # Print setup instructions
    print_setup_instructions()
    
    print(f"\n✅ Setup complete! Secret key generated: {secret_key[:8]}...")
    print("\nNext steps:")
    print("1. Configure Google OAuth (see instructions above)")
    print("2. Run: make run-personalized")
    print("3. Visit: http://localhost:8000")

if __name__ == "__main__":
    main()

