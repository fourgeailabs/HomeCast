import re

file_path = "app/src/main/AndroidManifest.xml"
with open(file_path, "r") as f:
    text = f.read()

permissions = """
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
"""

if "FOREGROUND_SERVICE_MEDIA_PLAYBACK" not in text:
    text = text.replace('<uses-permission android:name="android.permission.INTERNET" />', permissions)

service = """
        <service
            android:name=".PlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>
"""

if ".PlaybackService" not in text:
    text = text.replace("</application>", service + "\n    </application>")

with open(file_path, "w") as f:
    f.write(text)
