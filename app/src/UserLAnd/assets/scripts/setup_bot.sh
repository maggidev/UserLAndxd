#!/bin/sh
# setup_bot.sh - Bootstrap script for WhatsApp Bot (Baileys) on Alpine Linux
# This script runs on first boot of the PRoot Alpine session

set -e

LOCK_FILE="/var/log/bot_setup.done"
BOT_REPO_URL="${BOT_REPO_URL:-https://github.com/SEU_USUARIO/SEU_BOT_BAILEYS.git}"
BOT_DIR="/root/bot"
LOG_FILE="/var/log/bot_setup.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

error_exit() {
    log "ERROR: $*"
    exit 1
}

# Check if setup already completed
if [ -f "$LOCK_FILE" ]; then
    log "Setup already completed (lock file exists). Starting bot..."
    cd "$BOT_DIR" || error_exit "Bot directory not found: $BOT_DIR"
    exec npm start
fi

log "=== Starting WhatsApp Bot Setup ==="

# Step 1: Update package index and install dependencies
log "Step 1/5: Updating package index and installing dependencies..."
apk update >>"$LOG_FILE" 2>&1 || error_exit "Failed to update package index"

apk add --no-cache \
    nodejs \
    npm \
    git \
    python3 \
    make \
    g++ \
    ffmpeg \
    >>"$LOG_FILE" 2>&1 || error_exit "Failed to install dependencies"

log "Dependencies installed successfully"

# Step 2: Clone bot repository
log "Step 2/5: Cloning bot repository from $BOT_REPO_URL..."
rm -rf "$BOT_DIR"
git clone "$BOT_REPO_URL" "$BOT_DIR" >>"$LOG_FILE" 2>&1 || error_exit "Failed to clone repository: $BOT_REPO_URL"

log "Repository cloned successfully"

# Step 3: Install Node.js dependencies
log "Step 3/5: Installing Node.js dependencies (production)..."
cd "$BOT_DIR" || error_exit "Bot directory not found after clone"
npm install --production >>"$LOG_FILE" 2>&1 || error_exit "npm install failed"

log "Node.js dependencies installed successfully"

# Step 4: Mark setup as complete
log "Step 4/5: Creating lock file..."
mkdir -p "$(dirname "$LOCK_FILE")"
touch "$LOCK_FILE"
echo "$(date '+%Y-%m-%d %H:%M:%S')" > "$LOCK_FILE"

log "Setup completed successfully. Lock file created at $LOCK_FILE"

# Step 5: Start the bot
log "Step 5/5: Starting WhatsApp Bot..."
exec npm start