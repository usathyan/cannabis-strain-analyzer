"""Tests for the strain database."""

import pytest

from enhanced_strain_database import EnhancedStrainDatabase


@pytest.fixture
def db():
    """Create a test database instance."""
    return EnhancedStrainDatabase()


def test_database_initialization(db):
    """Test that the database initializes correctly."""
    assert db is not None
    assert hasattr(db, 'strains')
    assert isinstance(db.strains, dict)


def test_get_strain_data(db):
    """Test getting strain data."""
    # Test with a known strain (stored in lowercase)
    strain_data = db.get_strain("blue dream")
    assert strain_data is not None
    assert "effects" in strain_data
    assert "terpenes" in strain_data


def test_get_strain_data_nonexistent(db):
    """Test getting data for a non-existent strain."""
    strain_data = db.get_strain("NonExistentStrain")
    assert strain_data is None


def test_add_custom_strain(db):
    """Test adding a custom strain."""
    custom_strain = {
        "name": "Test Strain",
        "terpenes": {"myrcene": 0.5, "limonene": 0.3},
        "cannabinoids": {"thc": 0.2, "cbd": 0.1},
        "effects": ["relaxed", "happy"],
        "type": "hybrid",
        "description": "A test strain"
    }

    db.add_custom_strain("Test Strain", custom_strain)
    strain_data = db.get_strain("test strain")
    assert strain_data is not None
    assert "effects" in strain_data


def test_get_available_strains(db):
    """Test getting list of available strains."""
    strains = list(db.strains.keys())
    assert isinstance(strains, list)
    assert len(strains) > 0
    assert "blue dream" in strains
