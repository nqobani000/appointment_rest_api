#!/bin/sh
# Launcher logic for macOS and Linux. Start.command and Manage.command call this; Windows uses run.bat.
#
#   run.sh [start|stop|update|email|reset|logs|menu]

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR" || exit 1

WIZARD_IMAGE="curlimages/curl:8.23.0"
ACTION="${1:-start}"
LOG_FILE="$(mktemp)"
trap 'rm -f "$LOG_FILE"' EXIT

say() { printf '%s\n' "$*"; }

env_value() {
  [ -f .env ] || return 0
  sed -n "s/^$1='\(.*\)'\$/\1/p" .env | head -n 1
}

open_url() {
  case "$(uname -s)" in
    Darwin) open "$1" >/dev/null 2>&1 ;;
    *) command -v xdg-open >/dev/null 2>&1 && xdg-open "$1" >/dev/null 2>&1 ;;
  esac
  return 0
}

ask_yes() {
  printf '%s [Y/n]: ' "$1"
  IFS= read -r answer || return 1
  case "$answer" in n|N|no|NO|No) return 1 ;; *) return 0 ;; esac
}

ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    say "Docker is not installed on this computer."
    say "Install Docker Desktop (it is free) from https://www.docker.com/products/docker-desktop/ ,"
    say "open it once, then run this again."
    open_url "https://www.docker.com/products/docker-desktop/"
    return 1
  fi
  if ! docker info >/dev/null 2>&1; then
    if [ "$(uname -s)" = "Darwin" ] && open -a Docker >/dev/null 2>&1; then
      say "Starting Docker Desktop... this can take a minute or two."
      waited=0
      while ! docker info >/dev/null 2>&1 && [ "$waited" -lt 60 ]; do
        sleep 3
        waited=$((waited + 1))
      done
    fi
    if ! docker info >/dev/null 2>&1; then
      say "Docker is not running."
      say "Please start Docker Desktop, wait until it says it is running, then try again."
      return 1
    fi
  fi
  if ! docker compose version >/dev/null 2>&1; then
    say "This version of Docker is too old. Please update Docker Desktop and try again."
    return 1
  fi
}

require_env() {
  if [ ! -f .env ]; then
    say "Nothing is set up yet. Use Start first."
    return 1
  fi
}

# The wizard runs in a throwaway container so it behaves the same on every operating system.
run_wizard() {
  tty_flags=""
  if [ "${SETUP_NONINTERACTIVE:-0}" != "1" ]; then
    tty_flags="-i"
    [ -t 0 ] && tty_flags="-it"
  fi
  if [ "$(uname -s)" = "Linux" ]; then
    docker run --rm $tty_flags --user root -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" \
      -e SETUP_NONINTERACTIVE -e MAIL_HOST -e MAIL_PORT -e MAIL_USERNAME -e MAIL_PASSWORD -e MAIL_FROM -e ADMIN_EMAIL \
      -v "$APP_DIR:/bundle" --entrypoint sh "$WIZARD_IMAGE" /bundle/setup.sh "$@"
  else
    docker run --rm $tty_flags --user root \
      -e SETUP_NONINTERACTIVE -e MAIL_HOST -e MAIL_PORT -e MAIL_USERNAME -e MAIL_PASSWORD -e MAIL_FROM -e ADMIN_EMAIL \
      -v "$APP_DIR:/bundle" --entrypoint sh "$WIZARD_IMAGE" /bundle/setup.sh "$@"
  fi
}

diagnose() {
  say ""
  say "Something went wrong while starting."
  if grep -qiE "pull access denied|manifest unknown|no matching manifest|no such host|TLS handshake|i/o timeout|name resolution" "$LOG_FILE"; then
    say "The application could not be downloaded. Check your internet connection and try again."
    say "If it keeps happening, contact the person who gave you this package."
  elif grep -qi "unhealthy" "$LOG_FILE"; then
    say "The system started but did not become ready. Its latest messages were:"
    say ""
    docker compose logs --no-color --tail 25 api 2>&1
    say ""
    say "If this mentions email settings, use Manage > Change email settings."
  else
    tail -n 20 "$LOG_FILE"
  fi
  say ""
  say "To get help, use Manage > Save a support log file and send the file to your support contact."
}

start_stack() {
  tries=0
  while :; do
    if docker compose up -d --wait --wait-timeout 600 >"$LOG_FILE" 2>&1; then
      return 0
    fi
    if [ "$tries" -lt 10 ] && grep -qiE "port is already allocated|address already in use|ports are not available" "$LOG_FILE"; then
      old="$(env_value APP_PORT)"
      old="${old:-8080}"
      new=$((old + 1))
      say "Port $old is used by another program, trying $new instead..."
      run_wizard --port "$new" >/dev/null || return 1
      tries=$((tries + 1))
    else
      return 1
    fi
  done
}

# Sends a real verification email through the running system, as a customer would trigger it.
mail_check() {
  command -v curl >/dev/null 2>&1 || return 0
  admin="$(env_value ADMIN_EMAIL)"
  port="$(env_value APP_PORT)"
  say ""
  say "Final check: sending a real verification email to $admin ..."
  status="$(curl -s -o /dev/null -w '%{http_code}' -X POST "http://localhost:$port/api/auth/guests/code" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"Setup\",\"surname\":\"Check\",\"email\":\"$admin\",\"phone\":\"0000000000\"}")"
  if [ "$status" != "202" ]; then
    say "The system did not accept the test request (code $status). Use Manage > Save a support log file for help."
    return 0
  fi
  say "It can take up to a minute to arrive."
  if [ ! -t 0 ]; then
    say "Check $admin for the code."
    return 0
  fi
  if ask_yes "Did an email with a verification code arrive in $admin? (check Spam too)"; then
    say "Email is working."
  else
    say ""
    say "Email did not arrive. Things to try:"
    say "  - wait another minute and check the Spam folder"
    say "  - use Manage > Change email settings and re-enter the details"
    say "  - Gmail needs an App Password, not your normal password (see README section 3)"
  fi
}

do_start() {
  ensure_docker || return 1
  first=0
  if [ ! -f .env ]; then
    first=1
    run_wizard || { say "Setup was not completed. Run Start again to retry."; return 1; }
  fi
  [ -f .env ] || { say "Setup was not completed. Run Start again to retry."; return 1; }

  say ""
  say "Step 1 of 2: getting the application (first time only, this can take several minutes)..."
  docker compose pull --ignore-pull-failures
  say ""
  say "Step 2 of 2: starting. The first start also prepares the database, so please be patient..."
  if ! start_stack; then
    diagnose
    return 1
  fi

  url="http://localhost:$(env_value APP_PORT)"
  say ""
  say "The system is running at $url"
  open_url "$url"
  [ "$first" = "1" ] && mail_check
  say ""
  say "Keep Docker Desktop open while people use the system. To stop it, use Manage."
}

do_stop() {
  require_env || return 1
  ensure_docker || return 1
  docker compose down && say "Stopped. Your data is kept. Use Start to run it again."
}

do_update() {
  require_env || return 1
  ensure_docker || return 1
  say "Getting the latest version..."
  docker compose pull --ignore-pull-failures || return 1
  if ! start_stack; then
    diagnose
    return 1
  fi
  docker image prune -f >/dev/null 2>&1
  say "Up to date and running at http://localhost:$(env_value APP_PORT)"
}

do_email() {
  require_env || return 1
  ensure_docker || return 1
  run_wizard --email || return 1
  say "Restarting with the new email settings..."
  if ! start_stack; then
    diagnose
    return 1
  fi
  mail_check
}

do_reset() {
  require_env || return 1
  ensure_docker || return 1
  say "This permanently erases ALL appointments, customers and accounts."
  say "Your email settings are kept."
  printf 'Type RESET (in capitals) to continue, or press Enter to cancel: '
  IFS= read -r answer || return 1
  if [ "$answer" != "RESET" ]; then
    say "Cancelled. Nothing was erased."
    return 0
  fi
  docker compose down -v && say "All data erased. Use Start to begin with a fresh system."
}

do_logs() {
  require_env || return 1
  ensure_docker || return 1
  docker compose logs --no-color --tail 500 >support-logs.txt 2>&1
  say "Saved: $APP_DIR/support-logs.txt"
  say "Send that file to your support contact. It does not contain your passwords."
}

do_menu() {
  while :; do
    say ""
    say "===== Appointment System: Manage ====="
    say ""
    say "  1  Stop the system"
    say "  2  Update to the latest version"
    say "  3  Change email settings"
    say "  4  Save a support log file"
    say "  5  Erase all data and start fresh"
    say "  0  Close"
    say ""
    printf 'Choose a number: '
    IFS= read -r pick || return 0
    case "$pick" in
      1) do_stop ;;
      2) do_update ;;
      3) do_email ;;
      4) do_logs ;;
      5) do_reset ;;
      0|"") return 0 ;;
      *) say "Please type one of the numbers above." ;;
    esac
  done
}

case "$ACTION" in
  start) do_start ;;
  stop) do_stop ;;
  update) do_update ;;
  email) do_email ;;
  reset) do_reset ;;
  logs) do_logs ;;
  menu) do_menu ;;
  *) say "Unknown action: $ACTION"; exit 2 ;;
esac
