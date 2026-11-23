"""Ejecuta un comando con la conexión de Compose sin evaluar .env como código shell."""

import json
import os
from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parent.parent
START_COMMANDS = "python3 scripts/init-local-env.py\ndocker compose up -d --wait --wait-timeout 60 db"


def main() -> int:
    command = sys.argv[1:]
    integration = command[:1] == ["--integration"]
    if integration:
        command = command[1:]
    if not command:
        print("Uso desde la raíz: python3 scripts/with-local-db.py [--integration] COMANDO")
        return 2
    try:
        daemon = subprocess.run(
            ["docker", "info", "--format", "{{.ServerVersion}}"],
            cwd=ROOT, capture_output=True, text=True, timeout=15, check=False,
        )
        if daemon.returncode != 0:
            print("Docker no está disponible. Iniciar Docker Desktop o, en Linux con systemd:", file=sys.stderr)
            print("sudo systemctl start docker\nLuego:\n" + START_COMMANDS, file=sys.stderr)
            return 1
        config = subprocess.run(
            ["docker", "compose", "config", "--format", "json"],
            cwd=ROOT, capture_output=True, text=True, timeout=15, check=False,
        )
        if config.returncode != 0:
            # La salida de config puede contener valores sensibles; no mostrarla.
            print("Falta configuración local válida para Compose. Ejecutar:\n" + START_COMMANDS, file=sys.stderr)
            return 1
        database = json.loads(config.stdout)["services"]["db"]["environment"]
        ready = subprocess.run(
            ["docker", "compose", "exec", "-T", "db", "pg_isready",
             "-U", database["POSTGRES_USER"], "-d", database["POSTGRES_DB"]],
            cwd=ROOT, capture_output=True, text=True, timeout=15, check=False,
        )
        if ready.returncode != 0:
            print("PostgreSQL local no está corriendo o todavía no está listo. Desde la raíz ejecutar:", file=sys.stderr)
            print(START_COMMANDS, file=sys.stderr)
            print("Consultar estado: docker compose ps db", file=sys.stderr)
            return 1
    except FileNotFoundError:
        print("Docker no está instalado o no está en PATH; instalar Docker con Compose antes de iniciar la base.", file=sys.stderr)
        return 1
    except (subprocess.TimeoutExpired, KeyError, ValueError):
        print("No se pudo comprobar la base local. Revisar Docker/Compose y ejecutar:\n" + START_COMMANDS, file=sys.stderr)
        return 1

    environment = os.environ.copy()
    environment["SPRING_DATASOURCE_URL"] = "jdbc:postgresql://127.0.0.1:5433/" + database["POSTGRES_DB"]
    environment["SPRING_DATASOURCE_USERNAME"] = database["POSTGRES_USER"]
    environment["SPRING_DATASOURCE_PASSWORD"] = database["POSTGRES_PASSWORD"]
    environment.setdefault("APP_BASE_URL", "http://127.0.0.1:5173")
    environment.pop("ODONTOLOGIA_INTEGRATION_DATABASES", None)
    if integration:
        environment["ODONTOLOGIA_INTEGRATION_DATABASES"] = "true"
    print("Conexión local: 127.0.0.1:5433.", flush=True)
    # Reemplazar el proceso permite transmitir Ctrl+C sin una shell intermedia.
    os.chdir(ROOT)
    os.execvpe(command[0], command, environment)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
