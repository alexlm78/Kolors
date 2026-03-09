#!/bin/bash

###############################################################################
# Kolors Remote Deployment Script
# Copies only necessary files to remote server
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="kolors"
APP_VERSION="0.6.25"
REMOTE_USER="root"
REMOTE_HOST="kreaker.dev"
REMOTE_APP_DIR="/var/www/kolors.kreaker.net"
JAR_FILE="kolors-${APP_VERSION}.jar"
DB_FILE="kolors.db"

# Print functions
print_header() {
   echo -e "${BLUE}========================================${NC}"
   echo -e "${BLUE}$1${NC}"
   echo -e "${BLUE}========================================${NC}"
}

print_success() {
   echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
   echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
   echo -e "${RED}✗ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
   print_header "Checking Prerequisites"

   # Check rsync
   if command -v rsync &> /dev/null; then
      print_success "rsync found"
   else
      print_error "rsync not found. Install with: brew install rsync (macOS) or apt install rsync (Linux)"
      exit 1
   fi

   # Check SSH connection
   if ssh -o ConnectTimeout=5 -o BatchMode=yes "${REMOTE_USER}@${REMOTE_HOST}" "echo 'SSH OK'" &> /dev/null; then
      print_success "SSH connection to ${REMOTE_HOST} successful"
   else
      print_error "Cannot connect to ${REMOTE_HOST} via SSH"
      exit 1
   fi
}

# Check local files
check_local_files() {
   print_header "Checking Local Files"

   # Check JAR file
   if [[ -f "build/libs/${JAR_FILE}" ]]; then
      print_success "JAR file found: build/libs/${JAR_FILE}"
   else
      print_error "JAR file not found: build/libs/${JAR_FILE}"
      print_warning "Build the application first with: ./gradlew clean build -x test"
      exit 1
   fi

   # Check .env file (optional)
   if [[ -f ".env" ]]; then
      print_success ".env file found (will be copied)"
   else
      print_warning ".env file not found (will be skipped)"
   fi

   # Check database file
   if [[ -f "${DB_FILE}" ]]; then
      print_success "Database file found: ${DB_FILE}"
   else
      print_error "Database file not found: ${DB_FILE}"
      exit 1
   fi
}

# Create remote directory if not exists
create_remote_directory() {
   print_header "Creating Remote Directory"
   
   ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_APP_DIR}"
   print_success "Remote directory created/verified: ${REMOTE_APP_DIR}"
}

# Deploy files using rsync
deploy_files() {
   print_header "Deploying Files to Remote Server"

   # Deploy JAR file
   print_header "Copying JAR file"
   rsync -avz --progress "build/libs/${JAR_FILE}" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_APP_DIR}/"
   print_success "JAR file deployed"

   # Deploy .env file (if exists)
   if [[ -f ".env" ]]; then
      print_header "Copying .env file"
      rsync -avz --progress ".env" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_APP_DIR}/"
      print_success ".env file deployed"
   else
      print_warning "Skipping .env file (not found locally)"
   fi

   # Deploy database file
   print_header "Copying database file"
   rsync -avz --progress "${DB_FILE}" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_APP_DIR}/"
   print_success "Database file deployed"
}

# Show status
show_status() {
   print_header "Remote Server Status"
   
   ssh "${REMOTE_USER}@${REMOTE_HOST}" "ls -la ${REMOTE_APP_DIR}/"
}

# Main menu
show_help() {
   echo "Usage: $0 [command]"
   echo ""
   echo "Commands:"
   echo "  deploy     - Deploy files to remote server (default)"
   echo "  status     - Show remote server files"
   echo ""
}

# Main execution
case "${1:-deploy}" in
   deploy)
      check_prerequisites
      check_local_files
      create_remote_directory
      deploy_files
      show_status
      print_success "Deployment complete!"
      ;;
   status)
      show_status
      ;;
   *)
      show_help
      ;;
esac
