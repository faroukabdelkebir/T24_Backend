from django.urls import path, include
from rest_framework.decorators import api_view
from rest_framework.response import Response
from core.ollama_client import OllamaClient
import settings

@api_view(["GET"])
def health(request):
    from django.conf import settings
    from core.ollama_client import OllamaClient
    """GET / — Santé du service + état Ollama."""
    client = OllamaClient()
    available = client.is_available()
    models = client.list_models() if available else []
    active_model = settings.OLLAMA_MODEL
    return Response({
        "service": "AI Agents",
        "status": "running",
        "ollama": {"available": available, "models": models, "active_model": active_model},
        "agents": {
            "M1": "/api/m1/ — Anomalies + Qualité",
            "M3": "/api/m3/ — Ordonnancement intelligent",
        }
    })

urlpatterns = [
    path("", health),
    path("api/m1/", include("agent_anomaly_quality.urls")),
    path("api/m3/", include("agent_scheduler.urls")),
]
