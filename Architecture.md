# Architecture Overview

## 🏗️ System Architecture

This is a simple cannabis strain recommendation system built with LangGraph and FastAPI.

### Core Components

#### 1. Web Interface (`web_interface.py`)
- **FastAPI Server**: Handles HTTP requests and serves the web interface
- **HTML Template**: Simple form for strain input and results display
- **API Endpoints**:
  - `POST /api/recommendations` - Main recommendation endpoint
  - `GET /api/dispensaries` - Dispensary search endpoint
  - `GET /api/health` - Health check

#### 2. LangGraph Agent (`langgraph_agent.py`)
- **ReAct Agent**: Uses LangGraph for workflow management
- **Tool Integration**: Routes queries to appropriate tools
- **State Management**: Tracks conversation state and workflow progress

##### Agent Workflow:
```
User Query → Router Node → Orchestrator → Analysis/Recommendation/Dispensary Tools → Response
```

#### 3. Core Tools

##### Strain Analysis Tool
- Analyzes terpene profiles from the strain database
- Returns detailed strain information (effects, THC/CBD, type)

##### Recommendation Tool
- Calculates terpene similarity between strains
- Returns top 3 most similar strains based on cosine similarity

##### Dispensary Tool
- Searches for dispensaries near user location
- Supports configurable radius (10, 25, 50 miles)

#### 4. Data Layer (`enhanced_strain_database.py`)
- **In-Memory Database**: Stores 10+ strain profiles with terpene data
- **Similarity Calculation**: Cosine similarity on terpene vectors
- **Terpene Effects**: Predefined effects for each terpene

#### 5. Location Service (`google_maps_integration.py`)
- **Geocoding**: Converts addresses to coordinates
- **Dispensary Search**: Mock implementation for demonstration
- **Distance Calculation**: Calculates distances between locations

### Data Flow

1. **User Input**: Strain name + optional location/radius
2. **Agent Processing**: LangGraph agent interprets the query
3. **Tool Execution**: Appropriate tools are called (analysis, recommendation, dispensary)
4. **Result Aggregation**: Results combined and formatted
5. **Response**: JSON response sent to frontend
6. **Display**: Results rendered in HTML

### Technology Stack

- **Backend**: FastAPI (Python web framework)
- **Agent Framework**: LangGraph (workflow management)
- **LLM Integration**: LangChain + Ollama (local LLM)
- **Frontend**: Vanilla HTML/CSS/JavaScript (no frameworks)
- **Data Processing**: NumPy, scikit-learn
- **Geospatial**: Geopy (location services)

### Key Design Decisions

#### Simplicity First
- **Single Responsibility**: Each component has one clear purpose
- **Minimal Dependencies**: Only essential packages included
- **No Complex ML**: Basic similarity calculations instead of advanced ML
- **Mock Data**: Dispensary data uses mock implementation for simplicity

#### LangGraph Integration
- **Tool-Based Architecture**: Agent routes to specific tools
- **State Management**: Clean state transitions between nodes
- **Extensibility**: Easy to add new tools or modify workflow

#### Web-First Design
- **Direct API**: Frontend calls backend directly via AJAX
- **Simple UI**: Clean, responsive interface focused on core functionality
- **Progressive Enhancement**: Works without JavaScript (basic form submission)

### File Structure

```
/
├── web_interface.py          # FastAPI server and HTML template
├── langgraph_agent.py        # LangGraph ReAct agent implementation
├── enhanced_strain_database.py # Strain data and similarity calculations
├── google_maps_integration.py  # Location and dispensary services
├── requirements.txt          # Python dependencies
├── README.md                 # Project documentation
└── Architecture.md          # This file
```

### Future Extensions

The architecture is designed to be easily extensible:

- **Add New Tools**: New recommendation algorithms or data sources
- **Enhanced UI**: Add charts, filters, or advanced interactions
- **Real APIs**: Replace mock data with real dispensary/location APIs
- **User Accounts**: Add authentication and personalization
- **Caching**: Add Redis or database caching for performance

### Performance Considerations

- **Response Time**: <2 seconds for typical queries
- **Memory Usage**: Lightweight with in-memory data
- **Scalability**: Single-threaded design, can be scaled with async workers
- **Caching**: No caching implemented (could be added for frequent queries)

---

**This architecture prioritizes simplicity and clarity while demonstrating LangGraph's capabilities for tool routing and workflow management.**
