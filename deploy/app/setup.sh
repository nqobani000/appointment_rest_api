#!/bin/sh
# Interactive setup. It runs inside a throwaway container (curlimages/curl), so it behaves the same on
# Windows, macOS and Linux and needs nothing installed besides Docker.
#
#   setup.sh          first run: generates secrets, asks for email settings, sends a real test email
#   setup.sh --email  later runs: keeps secrets and data, only changes email settings
#   setup.sh --port N changes the port the system is served on
#
# Automation (no prompts): SETUP_NONINTERACTIVE=1 plus MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD,
# MAIL_FROM and ADMIN_EMAIL in the environment.

set -u

BUNDLE="${BUNDLE_DIR:-/bundle}"
ENV_FILE="$BUNDLE/.env"
MODE="${1:-full}"
NONINTERACTIVE="${SETUP_NONINTERACTIVE:-0}"
MSG_FILE="$(mktemp)"
ERR_FILE="$(mktemp)"

cleanup() {
  stty echo 2>/dev/null
  rm -f "$MSG_FILE" "$ERR_FILE" "$ENV_FILE.tmp"
}
trap cleanup EXIT
trap 'echo; echo "Setup cancelled. Nothing was changed."; exit 1' INT TERM

say() { printf '%s\n' "$*"; }
fail() { say ""; say "ERROR: $*"; exit 1; }

random_string() {
  head -c 192 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c "$1"
}

existing_value() {
  [ -f "$ENV_FILE" ] || return 0
  sed -n "s/^$1='\(.*\)'\$/\1/p" "$ENV_FILE" | head -n 1
}

# ask VAR "Question" "default"
ask() {
  var="$1"; question="$2"; default="${3:-}"
  if [ "$NONINTERACTIVE" = "1" ]; then return 0; fi
  if [ -n "$default" ]; then printf '%s [%s]: ' "$question" "$default"; else printf '%s: ' "$question"; fi
  IFS= read -r answer || fail "No input received."
  [ -z "$answer" ] && answer="$default"
  eval "$var=\$answer"
}

ask_secret() {
  var="$1"; question="$2"
  if [ "$NONINTERACTIVE" = "1" ]; then return 0; fi
  printf '%s (typing is hidden): ' "$question"
  [ -t 0 ] && stty -echo
  IFS= read -r answer || fail "No input received."
  [ -t 0 ] && stty echo
  echo
  eval "$var=\$answer"
}

valid_email() {
  case "$1" in
    *" "*|"") return 1 ;;
    ?*@?*.?*) return 0 ;;
    *) return 1 ;;
  esac
}

choose_provider() {
  say ""
  say "Which email service will send the emails?"
  say "  1) Gmail"
  say "  2) Brevo"
  say "  3) SendGrid"
  say "  4) Another provider"
  ask PROVIDER "Enter 1, 2, 3 or 4" "1"
  PROVIDER_HINT=""
  case "$PROVIDER" in
    1) MAIL_HOST="smtp.gmail.com"
       PROVIDER_HINT="Gmail needs an 'App password', not your normal password. See README.txt, section 3." ;;
    2) MAIL_HOST="smtp-relay.brevo.com"
       PROVIDER_HINT="Use the SMTP login and SMTP key from your Brevo account (SMTP & API page)." ;;
    3) MAIL_HOST="smtp.sendgrid.net"
       MAIL_USERNAME="apikey"
       PROVIDER_HINT="The username is the word: apikey. The password is your SendGrid API key." ;;
    4) MAIL_HOST="" ;;
    *) say "Please enter 1, 2, 3 or 4."; choose_provider; return ;;
  esac
}

send_test_email() {
  {
    printf 'From: Appointment Booking <%s>\r\n' "$MAIL_FROM"
    printf 'To: <%s>\r\n' "$ADMIN_EMAIL"
    printf 'Subject: Test email from your Appointment Booking system\r\n'
    printf 'Date: %s\r\n' "$(date -R)"
    printf 'Message-ID: <%s@appointment-setup.local>\r\n' "$(random_string 20)"
    printf 'Content-Type: text/plain; charset=utf-8\r\n'
    printf '\r\n'
    printf 'If you can read this, your email settings work.\r\n'
    printf 'The Appointment Booking system will use them to send verification codes and booking confirmations.\r\n'
  } > "$MSG_FILE"

  curl --silent --show-error --ssl-reqd --max-time 40 \
    --url "smtp://$MAIL_HOST:$MAIL_PORT" \
    --user "$MAIL_USERNAME:$MAIL_PASSWORD" \
    --mail-from "$MAIL_FROM" --mail-rcpt "$ADMIN_EMAIL" \
    --upload-file "$MSG_FILE" 2> "$ERR_FILE"
  code=$?
  [ "$code" -eq 0 ] && return 0

  say ""
  case "$code" in
    6)  say "Could not find the server '$MAIL_HOST'. Check the spelling and your internet connection." ;;
    7|28) say "Could not reach $MAIL_HOST on port $MAIL_PORT. Your network or firewall may be blocking email (port 587)." ;;
    35|60|67) if [ "$code" -eq 67 ]; then say "The email server rejected the username or password."; else say "A secure connection to the email server could not be made."; fi ;;
    *)  say "The email server did not accept the message." ;;
  esac
  say "Details: $(tail -n 1 "$ERR_FILE")"
  return 1
}

configure_email() {
  while :; do
    if [ "$NONINTERACTIVE" != "1" ]; then
      MAIL_HOST=""; MAIL_USERNAME=""; MAIL_PASSWORD=""; MAIL_FROM=""
      choose_provider
      [ -n "$PROVIDER_HINT" ] && say "" && say "$PROVIDER_HINT"
      say ""
      if [ -z "$MAIL_HOST" ]; then ask MAIL_HOST "Email server address (for example smtp.example.com)"; fi
      MAIL_PORT="587"
      if [ "$PROVIDER" = "4" ]; then
        ask MAIL_PORT "Email server port (the system needs a port that uses STARTTLS, normally 587)" "587"
      fi
      ask MAIL_USERNAME "Email username (for Gmail this is your full address)" "$MAIL_USERNAME"
      ask_secret MAIL_PASSWORD "Email password"
      [ "$PROVIDER" = "1" ] && MAIL_PASSWORD="$(printf '%s' "$MAIL_PASSWORD" | tr -d ' ')"
      default_from=""
      valid_email "$MAIL_USERNAME" && default_from="$MAIL_USERNAME"
      say ""
      say "The 'From' address is what customers see. Many providers only accept an address you have verified."
      ask MAIL_FROM "From address" "$default_from"
      ask ADMIN_EMAIL "Your own email address, to receive a test message" "$MAIL_FROM"
    fi

    problem=""
    [ -z "${MAIL_HOST:-}" ] && problem="The email server address is empty."
    [ -z "${MAIL_USERNAME:-}" ] && problem="The email username is empty."
    [ -z "${MAIL_PASSWORD:-}" ] && problem="The email password is empty."
    case "${MAIL_PASSWORD:-}${MAIL_USERNAME:-}" in *"'"*) problem="Quote characters (') are not supported in the username or password." ;; esac
    valid_email "${MAIL_FROM:-}" || problem="The From address does not look like an email address."
    valid_email "${ADMIN_EMAIL:-}" || problem="Your test email address does not look like an email address."
    if [ -n "$problem" ]; then
      say ""; say "$problem"
      [ "$NONINTERACTIVE" = "1" ] && exit 1
      continue
    fi

    say ""
    say "Sending a test email to $ADMIN_EMAIL ..."
    if send_test_email; then
      if [ "$NONINTERACTIVE" = "1" ]; then say "Test email accepted by $MAIL_HOST."; return 0; fi
      say "The email server accepted the message."
      ask CONFIRM "Did it arrive in your inbox? Check Spam too. [Y/n]" "Y"
      case "$CONFIRM" in
        [Nn]*) say "Let's check the details again." ;;
        *) return 0 ;;
      esac
    else
      [ "$NONINTERACTIVE" = "1" ] && exit 1
      ask RETRY "Try again with different details? [Y/n]" "Y"
      case "$RETRY" in [Nn]*) fail "Email must work before the system can start. Run Start again when you are ready." ;; esac
    fi
  done
}

write_env() {
  umask 077
  if [ -f "$ENV_FILE" ]; then
    grep -v -E '^(MAIL_HOST|MAIL_PORT|MAIL_USERNAME|MAIL_PASSWORD|MAIL_FROM|ADMIN_EMAIL)=' "$ENV_FILE" > "$ENV_FILE.tmp" || true
  else
    {
      echo "# Created by setup. Keep this file private: it holds your database and email passwords."
      echo "DB_USERNAME='appointments'"
      echo "DB_PASSWORD='$(random_string 32)'"
      echo "JWT_SECRET='$(random_string 64)'"
      echo "APP_PORT='8080'"
    } > "$ENV_FILE.tmp"
  fi
  {
    echo "MAIL_HOST='$MAIL_HOST'"
    echo "MAIL_PORT='$MAIL_PORT'"
    echo "MAIL_USERNAME='$MAIL_USERNAME'"
    echo "MAIL_PASSWORD='$MAIL_PASSWORD'"
    echo "MAIL_FROM='$MAIL_FROM'"
    echo "ADMIN_EMAIL='$ADMIN_EMAIL'"
  } >> "$ENV_FILE.tmp"
  mv "$ENV_FILE.tmp" "$ENV_FILE"
  if [ -n "${HOST_UID:-}" ]; then chown "$HOST_UID:${HOST_GID:-$HOST_UID}" "$ENV_FILE" 2>/dev/null; fi
}

[ -d "$BUNDLE" ] || fail "The settings folder was not mounted."

if [ "$MODE" = "--port" ]; then
  [ -f "$ENV_FILE" ] || fail "No settings found yet. Run Start first."
  PORT_VALUE="${2:-}"
  case "$PORT_VALUE" in ''|*[!0-9]*) fail "Invalid port number." ;; esac
  umask 077
  sed "s/^APP_PORT=.*/APP_PORT='$PORT_VALUE'/" "$ENV_FILE" > "$ENV_FILE.tmp" && mv "$ENV_FILE.tmp" "$ENV_FILE"
  if [ -n "${HOST_UID:-}" ]; then chown "$HOST_UID:${HOST_GID:-$HOST_UID}" "$ENV_FILE" 2>/dev/null; fi
  exit 0
fi

if [ "$MODE" = "--email" ]; then
  [ -f "$ENV_FILE" ] || fail "No settings found yet. Run Start first."
  say "Changing email settings. Your data and other settings stay as they are."
else
  if [ -f "$ENV_FILE" ]; then say "Settings already exist. Use Manage > Change email settings to change them."; exit 0; fi
  say "Welcome! Let's set up email, so verification codes and booking confirmations reach your customers."
  say "Secrets for the database and sign-in are generated for you; there is nothing to type for those."
fi

configure_email
write_env
say ""
say "Settings saved."
