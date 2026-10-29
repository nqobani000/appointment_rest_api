# Appointment Booking System: Quick Setup

## 1. Install Docker Desktop

Download and install Docker Desktop: https://www.docker.com/products/docker-desktop/

Open it and wait until it shows **Running**.

## 2. Set Up Gmail

Gmail requires an App Password instead of your normal password.

1. Enable 2-Step Verification: https://myaccount.google.com/security
2. Create an App Password: https://myaccount.google.com/apppasswords
3. Copy the 16-character password.

## 3. Start the App

- **Windows:** Double-click `Start.bat`.
- **Mac:** Double-click `Start.command`.
- **Linux:** Run `sh app/run.sh start` from the project folder.

On first launch:

1. Select **1 (Gmail)**.
2. Enter your full Gmail address.
3. Enter the 16-character App Password.
4. Enter your email address for the test email.
5. Confirm the test email arrives.

The app will download the required files, start the services, and open in your browser.

## 4. Open the App

Go to:

`http://localhost:8080`

If a different port is selected, use the address displayed by the startup window.

**Keep Docker Desktop running while using the app.**

## 5. Stop or Restart

To start the app again, double-click the startup file.

To stop it, open `Manage.bat` on Windows or `Manage.command` on Mac and select **1 (Stop the system)**.

Your saved data will be preserved.