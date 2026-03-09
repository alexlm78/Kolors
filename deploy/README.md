# Kolors Deployment Guide

## Requirements

- Ubuntu 24.04 Server
- Java 25 (OpenJDK 25)
- Nginx (with SSL certificates from Let's Encrypt)
- PM2 (Node.js process manager) or systemd
- Database: SQLite (included)

## Quick Start

### 1. Prepare the Server

```bash
# Install Java 25 (Ubuntu 24.04)
sudo apt update
sudo apt install openjdk-25-jdk

# Install Nginx
sudo apt install nginx

# Verify Java installation
java -version
```

### 2. Copy Files to Server

```bash
# Create deployment directory on server
sudo mkdir -p /var/www/kolors.kreaker.net
sudo mkdir -p /var/log/kolors

# Copy the deploy contents
rsync -avz --progess .env kolors.db deploy/kolors.service deploy/kolors.kreaker.net.nginx user@your-server:/var/www/kolors.kreaker.net
```

### 3. Build the Application

```bash
# On your local machine (with Gradle installed)
./gradlew clean build -x test

# Copy the JAR to the server
scp build/libs/kolors-0.6.12.jar user@your-server:/opt/kolors/build/libs/
```

### 4. Configure Environment

```bash
# SSH into your server
ssh user@your-server

# Edit the environment file
sudo nano /var/www/kolors.kreaker.net/.env
```

Edit these critical values:

- `JWT_SECRET_KEY` - Generate a secure key (use `openssl rand -base64 32`)
- `SPRING_MAIL_USERNAME` - Your email for sending password resets
- `SPRING_MAIL_PASSWORD` - Your email app password

### 5. Configure Nginx

```bash
# Copy nginx configuration
sudo cp /opt/kolors/kolors.kreaker.net.nginx /etc/nginx/sites-available/kolors.kreaker.net

# Enable the site
sudo ln -s /etc/nginx/sites-available/kolors.kreaker.net /etc/nginx/sites-enabled/

# Remove default site
sudo rm /etc/nginx/sites-enabled/default

# Test nginx config
sudo nginx -t

# Reload nginx
sudo systemctl reload nginx
```

### 6. Start the Application

####  Using Systemd

```bash
sudo cp /opt/kolors/kolors.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable kolors
sudo systemctl start kolors
```

### 7. Verify

```bash
# Check application status
sudo systemctl status kolors

# Check if running
curl http://localhost:9085

# Check logs
sudo journalctl -u kolors -f
```

## Management Commands

```bash
# Systemd commands
sudo systemctl start kolors
sudo systemctl stop kolors
sudo systemctl restart kolors
sudo journalctl -u kolors -f
```

## Application Details

- **Port**: 9085
- **Database**: SQLite at `/var/www/kolors.kreaker.net/kolors.db`
- **Logs**: `/var/log/kolors/`
- **Profile**: production (`SPRING_PROFILES_ACTIVE=prod`)

## Nginx Configuration

The nginx config proxies all requests to the Spring Boot application:

- Static assets are cached for 365 days
- SSL is handled by nginx
- All requests are forwarded to port 9085

## SSL Certificate

The SSL certificate is managed by Certbot. To renew:

```bash
sudo certbot renew
```

## Troubleshooting

### Application won't start

```bash
# Check logs
journal -u kolors -f

# Check Java
java -version

# Check port availability
sudo netstat -tlnp | grep 9085
```

### Database issues

```bash
# Check database file exists
ls -la /opt/kolors/*.db

# Recreate database if needed
sudo -u www-data sqlite3 /opt/kolors/kolors-prod.db
```

### Nginx errors

```bash
# Test configuration
sudo nginx -t

# Check nginx logs
sudo tail -f /var/log/nginx/error.log
```

## Auto-Start on Boot

### Systemd

```bash
sudo systemctl enable kolors
```

## File Structure

```dirs
/opt/kolors/
├── build/
│   └── libs/
│       └── kolors-0.6.12.jar
├── logs/
├── ecosystem.config.js
├── kolors.kreaker.net.nginx
├── kolors.service
├── manage-kolors.sh
└── .env (environment variables)
```

## Security Notes

1. Change the default JWT secret key in `.env`
2. Configure email settings for password reset functionality
3. Keep the database file backed up regularly
4. Review nginx security headers
5. Enable firewall (ufw): `sudo ufw enable`
