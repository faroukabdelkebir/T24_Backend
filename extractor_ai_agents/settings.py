"""
Django settings for SEO7 AI Agents.
Variables d'environnement dans .env (voir .env.example)
"""

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(override=True) 

BASE_DIR = Path(__file__).resolve().parent
SECRET_KEY = os.getenv("DJANGO_SECRET_KEY", "dev-secret-change-in-production")
DEBUG = os.getenv("DJANGO_DEBUG", "True").lower() == "true"
ALLOWED_HOSTS = ["*"]

INSTALLED_APPS = [
    "django.contrib.contenttypes",
    "django.contrib.staticfiles",
    "rest_framework",
    "corsheaders",
    "agent_anomaly_quality",
    "agent_scheduler",
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
]

CORS_ALLOW_ALL_ORIGINS = True  # Pour le dev — restreindre en prod

ROOT_URLCONF = "urls"
WSGI_APPLICATION = "wsgi.application"

DATABASES = {}  # Pas de DB Django, on utilise pyodbc directement

REST_FRAMEWORK = {
    "DEFAULT_RENDERER_CLASSES": ["rest_framework.renderers.JSONRenderer"],
    "DEFAULT_PARSER_CLASSES": ["rest_framework.parsers.JSONParser"],
    "UNAUTHENTICATED_USER": None,
}

STATIC_URL = "/static/"

# =============================================
# SQL Server (BI destination)
# =============================================
MSSQL_HOST = os.getenv("MSSQL_HOST", "127.0.0.1")
MSSQL_PORT = os.getenv("MSSQL_PORT", "1433")
MSSQL_DB = os.getenv("MSSQL_DB", "T24_TEST")
MSSQL_USER = os.getenv("MSSQL_USER", "T24")
MSSQL_PASS = os.getenv("MSSQL_PASS", "wevioo2026")

# =============================================
# Oracle T24 (source)
# =============================================
ORACLE_HOST = os.getenv("ORACLE_HOST", "127.0.0.1")
ORACLE_PORT = os.getenv("ORACLE_PORT", "1521")
ORACLE_SID = os.getenv("ORACLE_SID", "XE")
ORACLE_USER = os.getenv("ORACLE_USER", "T24_USER")
ORACLE_PASS = os.getenv("ORACLE_PASS", "20011920")

# =============================================
# Ollama
# =============================================
OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "Qwen2.5:7b")

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
