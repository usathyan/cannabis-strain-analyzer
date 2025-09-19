# 🏗️ Cannabis Strain Analyzer - Technical Architecture

## Overview

The Cannabis Strain Analyzer is a sophisticated web application that combines advanced mathematical similarity algorithms, AI-powered data generation, and comprehensive chemovar analysis to provide personalized cannabis strain recommendations.

## 🎯 Core Architecture Principles

- **Fixed Schema Design**: Consistent terpene and cannabinoid profiles across all strains
- **Z-Scored Similarity**: Robust mathematical comparison focusing on chemovar shape
- **Conservative Imputation**: Scientifically-backed default values for missing data
- **AI Integration**: LLM-powered strain data generation for unknown strains
- **Modular Design**: Clean separation of concerns with focused components

## 🧩 System Components

### 1. Main Application (`app.py`)

**Purpose**: FastAPI-based web server providing RESTful API and web interface

**Key Responsibilities**:
- HTTP request handling and routing
- User session management
- API endpoint implementation
- Template rendering
- Error handling and validation

**Architecture**:
```
FastAPI App
├── Routes
│   ├── GET / (Web Interface)
│   ├── POST /api/set-user
│   ├── GET /api/available-strains
│   ├── POST /api/create-ideal-profile
│   ├── POST /api/compare-strain
│   └── GET /api/user-profile
├── Services
│   └── EnhancedStrainDatabase
└── Templates
    └── Jinja2 HTML Templates
```

### 2. Strain Database (`enhanced_strain_database.py`)

**Purpose**: Manages strain data storage, retrieval, and persistence

**Key Features**:
- In-memory strain database with file-based persistence
- Custom strain addition and management
- Strain data validation and normalization
- JSON-based data serialization

**Data Structure**:
```python
StrainData = {
    "name": str,
    "terpenes": Dict[str, float],      # Fixed schema terpenes
    "cannabinoids": Dict[str, float],  # Fixed schema cannabinoids
    "effects": List[str],
    "type": str,                       # indica/sativa/hybrid
    "thc_range": str,
    "cbd_range": str,
    "description": str,
    "flavors": List[str]
}
```

## 🧮 Similarity Algorithm Architecture

### Z-Scored Cosine Similarity Implementation

**Mathematical Foundation**:
```
cos(θ) = x·y / (||x|| ||y||)
```

Where `x` and `y` are z-scored (standardized) terpene + cannabinoid vectors.

**Implementation Flow**:
```python
def compare_against_ideal_profile(strain_data, ideal_profile):
    # 1. Normalize to fixed schema
    normalized_strain = normalize_to_fixed_schema(strain_data)
    
    # 2. Create vectors in fixed order
    strain_vector = create_chemovar_vector(normalized_strain)
    ideal_vector = create_chemovar_vector(ideal_profile)
    
    # 3. Z-score standardization
    scaler = StandardScaler()
    combined_vectors = np.vstack([strain_vector, ideal_vector])
    scaled_vectors = scaler.fit_transform(combined_vectors)
    
    # 4. Calculate multiple similarity metrics
    z_scored_cosine = cosine_similarity(scaled_vectors[0], scaled_vectors[1])
    euclidean_sim = 1 / (1 + euclidean_distance(scaled_vectors))
    correlation_sim = correlation(scaled_vectors[0], scaled_vectors[1])
    
    # 5. Weighted combination
    combined_similarity = (
        0.5 * z_scored_cosine +    # Primary metric
        0.2 * euclidean_sim +      # Distance-based
        0.2 * correlation_sim +    # Pattern correlation
        0.1 * original_cosine      # Reference
    )
    
    return combined_similarity
```

### Multi-Metric Analysis

**Similarity Metrics**:
1. **Z-Scored Cosine Similarity (50%)**: Primary chemovar shape comparison
2. **Z-Scored Euclidean Similarity (20%)**: Distance-based comparison
3. **Z-Scored Correlation (20%)**: Profile pattern correlation
4. **Original Cosine Similarity (10%)**: Reference metric

## 🤖 AI Integration Architecture

### LLM-Powered Strain Generation

**Purpose**: Generate realistic strain data for unknown strains using AI

**Implementation**:
```python
async def generate_strain_data(strain_name: str):
    # 1. Create LLM prompt with fixed schema
    prompt = create_schema_based_prompt(strain_name)
    
    # 2. Call Ollama LLM
    llm = ChatOllama(model="llama3.2:latest")
    response = await llm.ainvoke(prompt)
    
    # 3. Parse and validate JSON response
    strain_data = json.loads(response.content)
    validate_required_fields(strain_data)
    
    # 4. Fallback to conservative generation if LLM fails
    if validation_fails:
        return generate_fallback_strain_data(strain_name)
    
    return strain_data
```

## 🌐 Web Interface Architecture

### Frontend Design

**Technology Stack**:
- **HTML5**: Semantic markup
- **CSS3**: Modern styling with flexbox/grid
- **JavaScript**: Vanilla JS for interactivity
- **Jinja2**: Server-side templating

**Component Structure**:
```
Tabbed Interface
├── Configuration Tab
│   ├── Strain Selection
│   ├── Ideal Profile Display
│   └── Profile Management
└── Compare Tab
    ├── Strain Input
    ├── Analysis Results
    └── AI Recommendations
```

## 🔧 Development Architecture

### Build System

**Makefile Targets**:
```makefile
install    # Install dependencies and create virtual environment
run        # Start the web application
test       # Run tests
clean      # Clean up generated files and virtual environment
```

### Project Structure
```
cannabis-strain-analyzer/
├── app.py                          # Main FastAPI application
├── enhanced_strain_database.py     # Strain database management
├── templates/
│   └── index.html                  # Web interface template
├── pyproject.toml                  # Project configuration
├── Makefile                        # Build automation
├── README.md                       # User documentation
├── Architecture.md                 # This file
└── LICENSE                         # MIT license
```

## 🚀 Deployment Architecture

### Development Environment
- **Local Development**: `make run` starts local server
- **Hot Reloading**: FastAPI automatic reload on changes
- **Debug Mode**: Detailed error logging and stack traces

### Production Considerations
- **WSGI Server**: Gunicorn or similar for production
- **Reverse Proxy**: Nginx for static files and load balancing
- **Database**: Consider PostgreSQL for production data persistence
- **Caching**: Redis for session management and API caching
- **Monitoring**: Application performance monitoring and logging

---

This architecture document provides a comprehensive overview of the Cannabis Strain Analyzer's technical design and implementation details.