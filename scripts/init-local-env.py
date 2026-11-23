"""Genera una credencial local para Compose sin reemplazar configuración existente."""

import os
from pathlib import Path
import secrets


def main() -> None:
    destination = Path(__file__).resolve().parent.parent / ".env"
    try:
        descriptor = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        print(".env ya existe; se conserva. Debe definir POSTGRES_PASSWORD para Compose.")
        return
    with os.fdopen(descriptor, "w") as stream:
        stream.write("# Solo desarrollo local; no versionar ni compartir.\n")
        stream.write("POSTGRES_PASSWORD=" + secrets.token_urlsafe(32) + "\n")
    print(".env creado con una contraseña aleatoria local, sin mostrarla.")


if __name__ == "__main__":
    main()
