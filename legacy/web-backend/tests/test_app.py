"""Tests for the main application."""

import pytest
from fastapi.testclient import TestClient

from app import app


@pytest.fixture
def client():
    """Create a test client for the FastAPI app."""
    return TestClient(app)


def test_root_endpoint(client):
    """Test that the root endpoint returns HTML."""
    response = client.get("/")
    assert response.status_code == 200
    assert "text/html" in response.headers["content-type"]


def test_available_strains_endpoint(client):
    """Test that the available strains endpoint returns data."""
    response = client.get("/api/available-strains")
    assert response.status_code == 200
    data = response.json()
    assert "strains" in data
    assert isinstance(data["strains"], list)


def test_set_user_endpoint(client):
    """Test setting a user."""
    response = client.post("/api/set-user", json={"user_id": "test_user"})
    assert response.status_code == 200
    data = response.json()
    assert data["message"] == "User ID set to test_user"


def test_user_profile_endpoint(client):
    """Test getting user profile."""
    # First set a user
    client.post("/api/set-user", json={"user_id": "test_user"})

    response = client.get("/api/user-profile")
    assert response.status_code == 200
    data = response.json()
    assert "id" in data
    assert data["id"] == "test_user"
