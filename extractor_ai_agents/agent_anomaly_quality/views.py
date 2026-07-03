from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from agent_anomaly_quality.agents.anomaly_quality_agent import AnomalyQualityAgent


class AuditTableView(APIView):
    """POST /api/agent/audit/ — Audit complet (qualité + anomalies)."""
    def post(self, request):
        table = request.data.get("table_name", "CUSTOMER")
        model = request.data.get("model", "qwen2.5:7b")
        try:
            agent = AnomalyQualityAgent(model=model)
            return Response({"status": "completed", "report": agent.audit_table(table)})
        except ConnectionError as e:
            return Response({"error": str(e)}, status=status.HTTP_503_SERVICE_UNAVAILABLE)


class AuditAllView(APIView):
    """POST /api/agent/audit-all/ — Audit comparatif des 3 tables."""
    def post(self, request):
        model = request.data.get("model", "qwen2.5:7b")
        try:
            agent = AnomalyQualityAgent(model=model)
            return Response({"status": "completed", "report": agent.audit_all_tables()})
        except ConnectionError as e:
            return Response({"error": str(e)}, status=status.HTTP_503_SERVICE_UNAVAILABLE)

class QuickScoreView(APIView):
    """POST /api/agent/quick-score/ — Score rapide (Phase 1 seulement)."""
    def post(self, request):
        table = request.data.get("table_name", "CUSTOMER")
        model = request.data.get("model", "qwen2.5:7b")
        try:
            agent = AnomalyQualityAgent(model=model)
            return Response(agent.quick_score(table))
        except ConnectionError as e:
            return Response({"error": str(e)}, status=status.HTTP_503_SERVICE_UNAVAILABLE)
