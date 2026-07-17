import os
import pyodbc
from dotenv import load_dotenv

load_dotenv(override=True)

MSSQL_HOST = os.getenv("MSSQL_HOST", "127.0.0.1")
MSSQL_PORT = os.getenv("MSSQL_PORT", "1433")
MSSQL_DB = os.getenv("MSSQL_DB", "T24_TEST")
MSSQL_USER = os.getenv("MSSQL_USER", "")
MSSQL_PASS = os.getenv("MSSQL_PASS", "")

# Test 1 : authentification SQL (UID/PWD)
try:
    conn = pyodbc.connect(
        "DRIVER={ODBC Driver 17 for SQL Server};"
        f"SERVER={MSSQL_HOST},{MSSQL_PORT};"
        f"DATABASE={MSSQL_DB};"
        f"UID={MSSQL_USER};"
        f"PWD={MSSQL_PASS};"
        "Connection Timeout=5;"
    )
    print("✅ Test 1 OK - Auth SQL")
    conn.close()
except Exception as e:
    print(f"❌ Test 1 : {e}")

# Test 2 : authentification Windows (Trusted)
try:
    conn = pyodbc.connect(
        "DRIVER={ODBC Driver 17 for SQL Server};"
        f"SERVER={MSSQL_HOST},{MSSQL_PORT};"
        f"DATABASE={MSSQL_DB};"
        "Trusted_Connection=yes;"
        "Connection Timeout=5;"
    )
    print("✅ Test 2 OK - Auth Windows")
    conn.close()
except Exception as e:
    print(f"❌ Test 2 : {e}")

# Test 3 : sans spécifier la DB
try:
    conn = pyodbc.connect(
        "DRIVER={ODBC Driver 17 for SQL Server};"
        f"SERVER={MSSQL_HOST},{MSSQL_PORT};"
        "Trusted_Connection=yes;"
        "Connection Timeout=5;"
    )
    print("✅ Test 3 OK - Sans DB")
    # Lister les bases disponibles
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sys.databases")
    dbs = [row[0] for row in cursor.fetchall()]
    print(f"   Bases disponibles : {dbs}")
    conn.close()
except Exception as e:
    print(f"❌ Test 3 : {e}")