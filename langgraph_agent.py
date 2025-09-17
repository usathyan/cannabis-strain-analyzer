"""
Simple LangGraph ReAct Agent for Cannabis Strain Recommendations
Routes user queries to appropriate tools for analysis and recommendations
"""

from typing import Dict, List, Any, Optional
from langchain_core.messages import HumanMessage, SystemMessage, BaseMessage
from langchain_ollama import ChatOllama
from langgraph.graph import StateGraph, END
from langgraph.graph.message import add_messages
from typing_extensions import Annotated, TypedDict
import os
from dotenv import load_dotenv

from enhanced_strain_database import EnhancedStrainDatabase
from google_maps_integration import LocationService

load_dotenv()

class StrainAgentState(TypedDict):
    """State for the strain recommendation agent"""
    messages: Annotated[list, add_messages]
    user_query: str
    strain_name: Optional[str]
    location: Optional[str]
    radius: Optional[int]
    analysis_result: Optional[Dict[str, Any]]
    recommendations: List[Dict[str, Any]]
    dispensaries: List[Dict[str, Any]]
    next_action: str

class StrainAnalysisTool:
    """Tool for analyzing cannabis strains"""

    def __init__(self):
        self.db = EnhancedStrainDatabase()
        self.llm = ChatOllama(
            model=os.getenv("OLLAMA_MODEL", "gemma3:latest"),
            base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        )

    def analyze_strain(self, strain_name: str) -> Dict[str, Any]:
        """Analyze a strain and return its profile"""
        strain_data = self.db.get_strain(strain_name)
        if not strain_data:
            return {"error": "Strain not found in database"}

        return {
            "name": strain_name,
            "terpene_profile": strain_data["terpenes"],
            "effects": strain_data["effects"],
            "type": strain_data["type"],
            "thc_range": strain_data["thc_range"],
            "cbd_range": strain_data["cbd_range"],
            "description": strain_data["description"]
        }

class RecommendationTool:
    """Tool for generating strain recommendations"""

    def __init__(self):
        self.db = EnhancedStrainDatabase()

    def get_recommendations(self, strain_name: str, limit: int = 3) -> List[Dict[str, Any]]:
        """Get similar strains based on terpene profile"""
        similar_strains = self.db.get_similar_strains(strain_name, limit)

        recommendations = []
        for strain, similarity in similar_strains:
            strain_data = self.db.get_strain(strain)
            if strain_data:
                recommendations.append({
                    "strain_name": strain,
                    "similarity": similarity,
                    "strain_data": strain_data
                })

        return recommendations

class DispensaryTool:
    """Tool for finding dispensaries"""

    def __init__(self):
        self.location_service = LocationService()

    def find_dispensaries(self, location: str, radius: int = 25) -> List[Dict[str, Any]]:
        """Find dispensaries near a location"""
        try:
            return self.location_service.find_dispensaries_near_location(location, radius)
        except Exception as e:
            return [{"error": f"Could not find dispensaries: {str(e)}"}]

class StrainRecommendationAgent:
    """Simple LangGraph agent for strain recommendations"""

    def __init__(self):
        self.llm = ChatOllama(
            model=os.getenv("OLLAMA_MODEL", "gemma3:latest"),
            base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        )

        # Initialize tools
        self.analysis_tool = StrainAnalysisTool()
        self.recommendation_tool = RecommendationTool()
        self.dispensary_tool = DispensaryTool()

        # Build the graph
        self.graph = self._build_graph()

    def _build_graph(self) -> StateGraph:
        """Build the LangGraph workflow"""

        def router_node(state: StrainAgentState) -> Dict[str, Any]:
            """Route the user query to appropriate tools"""
            query = state["user_query"].lower()

            # Extract key information from query using LLM
            routing_prompt = f"""
            Analyze this user query and extract:
            1. Strain name (if mentioned)
            2. Location (if mentioned)
            3. Radius (if mentioned, default 25)
            4. What action they want (analyze, recommend, find dispensaries)

            Query: {state['user_query']}

            Return as JSON with keys: strain_name, location, radius, action
            """

            messages = [
                SystemMessage(content="You are a cannabis expert. Extract information from user queries in JSON format."),
                HumanMessage(content=routing_prompt)
            ]

            response = self.llm.invoke(messages)

            # Parse response (simplified - in production use proper JSON parsing)
            try:
                # Simple parsing - look for strain mentions
                strain_name = None
                location = None
                radius = 25

                # Basic keyword extraction
                words = query.split()
                common_strains = ["granddaddy purple", "blue dream", "og kush", "sour diesel", "girl scout cookies"]

                for strain in common_strains:
                    if strain in query:
                        strain_name = strain
                        break

                # Look for location keywords
                if "san francisco" in query or "sf" in query:
                    location = "San Francisco, CA"
                elif "oakland" in query:
                    location = "Oakland, CA"
                elif "berkeley" in query:
                    location = "Berkeley, CA"

                # Look for radius
                if "10 miles" in query or "10 mile" in query:
                    radius = 10
                elif "50 miles" in query or "50 mile" in query:
                    radius = 50

                return {
                    "strain_name": strain_name,
                    "location": location,
                    "radius": radius,
                    "next_action": "analyze" if "analyze" in query else "recommend"
                }
            except:
                return {
                    "strain_name": None,
                    "location": None,
                    "radius": 25,
                    "next_action": "recommend"
                }

        def analysis_node(state: StrainAgentState) -> Dict[str, Any]:
            """Analyze a strain"""
            if not state.get("strain_name"):
                return {"analysis_result": {"error": "No strain specified"}}

            result = self.analysis_tool.analyze_strain(state["strain_name"])
            return {"analysis_result": result}

        def recommendation_node(state: StrainAgentState) -> Dict[str, Any]:
            """Generate recommendations"""
            if not state.get("strain_name"):
                return {"recommendations": []}

            recommendations = self.recommendation_tool.get_recommendations(state["strain_name"])
            return {"recommendations": recommendations}

        def dispensary_node(state: StrainAgentState) -> Dict[str, Any]:
            """Find dispensaries"""
            if not state.get("location"):
                return {"dispensaries": []}

            dispensaries = self.dispensary_tool.find_dispensaries(
                state["location"],
                state.get("radius", 25)
            )
            return {"dispensaries": dispensaries}

        def orchestrator_node(state: StrainAgentState) -> Dict[str, Any]:
            """Orchestrate the workflow based on user intent"""
            action = state.get("next_action", "recommend")

            if action == "analyze":
                return {"next_action": "analysis"}
            elif action == "recommend":
                return {"next_action": "recommend"}
            else:
                return {"next_action": "end"}

        # Build the graph
        workflow = StateGraph(StrainAgentState)

        # Add nodes
        workflow.add_node("router", router_node)
        workflow.add_node("analysis", analysis_node)
        workflow.add_node("recommendation", recommendation_node)
        workflow.add_node("dispensary", dispensary_node)
        workflow.add_node("orchestrator", orchestrator_node)

        # Add edges
        workflow.set_entry_point("router")
        workflow.add_edge("router", "orchestrator")

        # Conditional routing from orchestrator
        workflow.add_conditional_edges(
            "orchestrator",
            lambda x: x["next_action"],
            {
                "analysis": "analysis",
                "recommend": "recommendation",
                "end": END
            }
        )

        workflow.add_edge("analysis", END)
        workflow.add_edge("recommendation", "dispensary")
        workflow.add_edge("dispensary", END)

        return workflow.compile()

    def process_query(self, query: str) -> Dict[str, Any]:
        """Process a user query through the agent"""
        initial_state = {
            "messages": [HumanMessage(content=query)],
            "user_query": query,
            "strain_name": None,
            "location": None,
            "radius": 25,
            "analysis_result": None,
            "recommendations": [],
            "dispensaries": [],
            "next_action": "recommend"
        }

        result = self.graph.invoke(initial_state)

        return {
            "query": query,
            "analysis": result.get("analysis_result"),
            "recommendations": result.get("recommendations", []),
            "dispensaries": result.get("dispensaries", [])
        }

# Simple API function for web interface
def get_strain_recommendations(strain_name: str, location: Optional[str] = None, radius: int = 25) -> Dict[str, Any]:
    """Simple API function for strain recommendations"""
    agent = StrainRecommendationAgent()

    # Create a query string for the agent
    query = f"I like {strain_name}"
    if location:
        query += f" and I'm in {location}"
    if radius != 25:
        query += f" within {radius} miles"

    result = agent.process_query(query)

    # Format for web interface
    response = {
        "strain_analysis": result.get("analysis", {}),
        "recommendations": result.get("recommendations", []),
        "dispensaries": result.get("dispensaries", [])
    }

    return response

if __name__ == "__main__":
    # Test the agent
    agent = StrainRecommendationAgent()
    result = agent.process_query("I like Granddaddy Purple and I'm in San Francisco")
    print(result)
