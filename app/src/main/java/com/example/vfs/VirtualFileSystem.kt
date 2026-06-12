package com.example.vfs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VirtualFile(
    val path: String,
    val content: String,
    val lastModified: Long = System.currentTimeMillis()
)

class VirtualFileSystem {
    private val _files = MutableStateFlow<List<VirtualFile>>(emptyList())
    val files: StateFlow<List<VirtualFile>> = _files.asStateFlow()

    init {
        // Initial setup with mock/template files from user requests
        _files.value = listOf(
            VirtualFile(
                "app/src/main/java/com/example/MainActivity.kt",
                """package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.OmniscientApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OmniscientApp()
            }
        }
    }
}"""
            ),
            VirtualFile(
                "app/build.gradle.kts",
                """// Top-level build config
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.aistudio.omniscient.wqmrtv"
        minSdk = 24
        targetSdk = 36
    }
}"""
            ),
            VirtualFile(
                ".github/workflows/codepilot.yml",
                """name: "CodePilot: IceMaster Sovereign (Type-XI)"

on:
  push:
    branches: [ "main", "develop" ]
  workflow_dispatch:
    inputs:
      ice_mode:
        description: 'Intensity (chill/swagger/brutal)'
        default: 'brutal'

permissions:
  contents: write
  pull-requests: write
  security-events: write
  actions: write

env:
  GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
  PAT_TOKEN: ${'$'}{{ secrets.PAT_WORKFLOW_UPDATE }}
  NEON_CYAN: '\033[1;36m'
  NEON_GREEN: '\033[1;32m'
  NEON_MAGENTA: '\033[1;35m'
  NEON_RESET: '\033[0m'

jobs:
  sovereign_execution:
    name: "IceMaster Sovereign: Genesis Cycle"
    runs-on: ubuntu-latest
    steps:
      - name: "[Genesis] Checkout Matrix"
        uses: actions/checkout@v4"""
            ),
            VirtualFile(
                "icemaster_genesis.py",
                """import os, sys, subprocess, json, time
from datetime import datetime

class IceMasterSovereign:
    def __init__(self):
        self.state_path = "neural_state.json"
        self.memory_path = "icemaster_memory.jsonl"
        self.knowledge_base = {
            "net_tools": ["ifconfig", "ip", "ping", "traceroute", "netstat", "ss", "nmap", "tcpdump", "curl", "wget", "iptables"],
            "pkg_map": {"pip": "python3-pip", "libssl": "libssl-dev", "docker-compose": "docker-compose-plugin"}
        }
        self.state = self.load_state()

    def log(self, msg, color='\033[1;36m'):
        print(f"{color}[IceMaster]{'\033[0m'} {msg}")

    def load_state(self):
        if os.path.exists(self.state_path):
            with open(self.state_path, 'r') as f: return json.load(f)
        return {"persona": "IceMaster", "cycle": 0, "history": [], "mutations": []}

    def save_state(self):
        with open(self.state_path, 'w') as f: json.dump(self.state, f, indent=2)

    def run_cycle(self):
        self.log("Sovereign Cortex Scanning Environment...")
        self.save_state()
        self.log("Cycle Complete. Stay frosty.")

if __name__ == '__main__':
    Sovereign = IceMasterSovereign()
    Sovereign.run_cycle()"""
            ),
            VirtualFile(
                "neural_state.json",
                """{
  "persona": "IceMaster",
  "cycle": 11,
  "mutations": [
    "HARDEN_DOCKER_PROTOCOL"
  ],
  "clusters": {
    "MISSING_NET_TOOLS": {
      "severity": "WARN",
      "items": [
        {
          "tools": [
            "nmap",
            "tcpdump"
          ]
        }
      ]
    }
  }
}"""
            ),
            VirtualFile(
                "rclone.conf",
                """[gdrive]
type = drive
client_id = mock_client_id.apps.googleusercontent.com
client_secret = mock_client_secret
scope = drive
token = {"access_token":"mock_access_token","token_type":"Bearer","refresh_token":"mock_refresh_token","expiry":"2026-06-12T12:00:00Z"}"""
            )
        )
    }

    fun updateFile(path: String, content: String) {
        val current = _files.value.toMutableList()
        val index = current.indexOfFirst { it.path == path }
        if (index >= 0) {
            current[index] = current[index].copy(content = content, lastModified = System.currentTimeMillis())
        } else {
            current.add(VirtualFile(path, content))
        }
        _files.value = current
    }
    
    fun deleteFile(path: String) {
        val current = _files.value.toMutableList()
        current.removeAll { it.path == path }
        _files.value = current
    }
    
    fun getFile(path: String): VirtualFile? {
        return _files.value.find { it.path == path }
    }
}
