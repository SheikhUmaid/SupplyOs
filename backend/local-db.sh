#!/usr/bin/env bash
# Runs a private MySQL for this project on port 3307, with no root or sudo access needed.
# It doesn't touch any MySQL already installed on the machine. Passwords come from backend/.env.
#
#   ./local-db.sh start | stop | status
set -euo pipefail

cd "$(dirname "$0")"
# AppArmor's MySQL profile only allows data under a few paths, and /var/tmp survives a reboot.
DATA_ROOT="/var/tmp/supplychain-mysql"
DATA_DIR="$DATA_ROOT/data"
PORT=3307

setting() { grep "^$1=" .env | cut -d= -f2-; }
is_running() { ss -ltn | grep -q ":$PORT\b"; }

start_server() {
  # The socket path is relative because MySQL limits its length, and all three streams are redirected
  # so that nothing in the caller's terminal or pipeline waits on the server.
  (
    cd "$DATA_ROOT"
    nohup /usr/sbin/mysqld --no-defaults --datadir="$DATA_DIR" --port="$PORT" \
      --bind-address=127.0.0.1 --socket=mysql.sock --mysqlx=OFF --pid-file="$DATA_ROOT/mysqld.pid" \
      --log-error="$DATA_ROOT/server.log" </dev/null >/dev/null 2>&1 &
  )
  for _ in $(seq 1 30); do
    is_running && return 0
    sleep 1
  done
  echo "MySQL did not start. See $DATA_ROOT/server.log" >&2
  exit 1
}

case "${1:-}" in
  start)
    if is_running; then
      echo "Already running on port $PORT."
      exit 0
    fi
    password="$(setting DB_PASSWORD)"
    if [ ! -d "$DATA_DIR" ]; then
      mkdir -p "$DATA_ROOT"
      /usr/sbin/mysqld --no-defaults --initialize-insecure --datadir="$DATA_DIR" \
        --log-error="$DATA_ROOT/init.log"
      start_server
      mysql --no-defaults -u root -h 127.0.0.1 -P "$PORT" <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '$password';
CREATE DATABASE supplychain CHARACTER SET utf8mb4;
CREATE DATABASE supplychain_test CHARACTER SET utf8mb4;
CREATE USER 'supplychain'@'localhost' IDENTIFIED BY '$password';
GRANT ALL PRIVILEGES ON supplychain.* TO 'supplychain'@'localhost';
GRANT ALL PRIVILEGES ON supplychain_test.* TO 'supplychain'@'localhost';
SQL
    else
      start_server
    fi
    echo "MySQL is running on port $PORT."
    ;;
  stop)
    MYSQL_PWD="$(setting DB_PASSWORD)" mysqladmin --no-defaults -u root -h 127.0.0.1 -P "$PORT" shutdown
    echo "MySQL stopped."
    ;;
  status)
    is_running && echo "Running on port $PORT." || echo "Not running."
    ;;
  *)
    echo "Usage: $0 start|stop|status" >&2
    exit 2
    ;;
esac
