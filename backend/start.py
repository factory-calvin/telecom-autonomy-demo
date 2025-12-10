#!/usr/bin/env python3
import socket
import subprocess
import sys


def find_available_port(start_port: int = 8000, max_attempts: int = 100) -> int:
    for port in range(start_port, start_port + max_attempts):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(("127.0.0.1", port))
                return port
            except OSError:
                continue
    raise RuntimeError(f"No available port found in range {start_port}-{start_port + max_attempts}")


if __name__ == "__main__":
    reload_flag = "--reload" in sys.argv
    port = find_available_port()
    print(f"Starting backend on port {port}")
    
    cmd = ["python3", "-m", "uvicorn", "main:app", "--port", str(port)]
    if reload_flag:
        cmd.append("--reload")
    
    subprocess.run(cmd)
