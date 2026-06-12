import json
import os
import sys
import subprocess

def validate_environment():
    required_vars = ["KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD"]
    missing = [var for var in required_vars if not os.environ.get(var)]
    if missing:
        print(f"Error: Missing environment variables: {', '.join(missing)}")
        sys.exit(1)
    print("Environment variables validated for production signing.")

def sign_apk(apk_path, keystore_path):
    print(f"Executing jarsigner for: {apk_path}")
    keystore_password = os.environ.get("KEYSTORE_PASSWORD")
    key_alias = os.environ.get("KEY_ALIAS")
    key_password = os.environ.get("KEY_PASSWORD")
    
    cmd = [
        "jarsigner",
        "-verbose",
        "-sigalg", "SHA256withRSA",
        "-digestalg", "SHA-256",
        "-keystore", keystore_path,
        "-storepass", keystore_password,
        "-keypass", key_password,
        apk_path,
        key_alias
    ]
    
    try:
        subprocess.run(cmd, check=True)
        print("APK successfully signed with production keystore.")
    except Exception as e:
        print(f"Failed to sign APK using jarsigner: {e}")
        sys.exit(1)

def verify_signature(apk_path):
    cmd = ["jarsigner", "-verify", "-verbose", "-certs", apk_path]
    try:
        subprocess.run(cmd, check=True)
        print("Signature verified successfully.")
    except Exception as e:
        print(f"Failed to verify APK signature: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        action = sys.argv[1]
        
        if action == "validate":
            validate_environment()
        elif action == "sign" and len(sys.argv) > 3:
            validate_environment()
            apk_path = sys.argv[2]
            keystore_path = sys.argv[3]
            sign_apk(apk_path, keystore_path)
        elif action == "verify" and len(sys.argv) > 2:
            verify_signature(sys.argv[2])
        else:
            print("Invalid arguments. Usage: script.py [validate|sign|verify] ...")
            sys.exit(1)
    else:
        validate_environment()
