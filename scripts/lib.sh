#!/usr/bin/env bash
# Shared helpers for the dev scripts. Source this, do not execute directly.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT/.run"
LOG_DIR="$ROOT/.logs"
mkdir -p "$RUN_DIR" "$LOG_DIR"

port_open() {
  if ss -ltn 2>/dev/null | grep -qE ":$1 "; then
    return 0
  fi
  return 1
}

pid_of_port() {
  local pid=""
  if command -v ss >/dev/null 2>&1; then
    pid="$(ss -ltnp 2>/dev/null | grep -E ":$1 " | grep -oE 'pid=[0-9]+' | grep -oE '[0-9]+' | head -1 || true)"
  elif command -v lsof >/dev/null 2>&1; then
    pid="$(lsof -ti ":$1" 2>/dev/null | head -1 || true)"
  fi
  printf '%s' "$pid"
  return 0
}

kill_port() {
  local pid
  pid="$(pid_of_port "$1")"
  if [ -n "$pid" ]; then
    kill "$pid" 2>/dev/null || true
  fi
}

is_running() {
  local f="$RUN_DIR/$1.pid"
  [ -f "$f" ] && kill -0 "$(cat "$f")" 2>/dev/null
}

write_pid() {
  echo "$2" > "$RUN_DIR/$1.pid"
}

rm_pid() {
  rm -f "$RUN_DIR/$1.pid"
}

wait_port() {
  local tries="${2:-60}"
  local i
  for i in $(seq 1 "$tries"); do
    if port_open "$1"; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_http() {
  local url="$1"
  local tries="${2:-60}"
  local i
  for i in $(seq 1 "$tries"); do
    local code
    code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$url" 2>/dev/null || true)"
    if [ -n "$code" ] && [ "$code" != "000" ]; then
      return 0
    fi
    sleep 1
  done
  return 1
}
