import pyodbc

# Test 1 : authentification SQL (UID/PWD)
try:
    conn = pyodbc.connect(
        "DRIVER={ODBC Driver 17 for SQL Server};"
        "SERVER=127.0.0.1,1433;"
        "DATABASE=T24_TEST;"
        "UID=T24;"
        "PWD=wevioo2026;"
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
        "SERVER=127.0.0.1,1433;"
        "DATABASE=T24_TEST;"
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
        "SERVER=127.0.0.1,1433;"
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