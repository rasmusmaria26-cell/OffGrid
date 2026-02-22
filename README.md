# OffGrid — P2P Mesh Communication

![OffGrid Interface Mocks](https://placehold.co/800x400/0f0f0f/00B4FF?text=OFFGRID+MESH+INTERFACE)

OffGrid is a resilient, decentralized messaging platform built for festivals, concerts, and emergency scenarios where cellular infrastructure is overloaded or non-existent. It uses **Google Nearby Connections** to form a dynamic hopping mesh network.

## 🚀 Features

### 📡 Mesh Networking
- **Zero Infrastructure**: No Wi-Fi router or Cell Tower required.
- **Auto-Relay**: Every device acts as a hop. If A can't reach C, but A can reach B, B will automatically relay the message.
- **De-duplication**: Smart routing prevents message loops and flooding.

### 🛡️ Privacy First
- **E2E Encryption**: All Direct Messages (DMs) are encrypted using **RSA-2048** and **AES-256**.
- **Self-Sovereign Identity**: User identities are generated locally and persisted to the device.
- **Metadata Protection**: Relay nodes only see routing headers, not message content.

### 🎨 Modern Experience
- **Sonar Radar**: Real-time visual feedback of nearby nodes with name labels floating on the canvas.
- **Dual Modes**: 
  - **Festival**: Electric blue UI, group chats, location sharing quick-actions.
  - **Emergency**: High-contrast orange UI, one-tap SOS broadcast with haptic wave feedback.
- **P2P Topology**: Visualizer to see exactly how your mesh is connected.

## 🛠️ Tech Stack
- **Language**: Kotlin
- **Framework**: Jetpack Compose
- **Mesh Engine**: Google Nearby Connections (Bluetooth Low Energy + Wi-Fi Direct)
- **Encryption**: Java Crypto (RSA, AES/CBC/PKCS5Padding)
- **UI Architecture**: MVVM with StateFlow

## 📱 Getting Started

1. **Clone & Build**: Import into Android Studio and run on a physical device.
2. **Onboarding**: Set your name and choose a mode.
3. **Connect**: Head to the **Radar** screen. Wait for nodes to pulse into view.
4. **Chat**: Switch to the **Chat** tab to send broadcasts or pick a peer for encrypted DMs.
5. **SOS**: In an emergency, hit the SOS button to alert every node in range (and their neighbors).

## 🗺️ Roadmap

- [x] Phase 1: Mesh Foundation & Relay Routing
- [x] Phase 2: UI/UX Overhaul (Radar, Modern Chat)
- [x] Phase 3: Privacy (RSA + AES Hybrid Encryption)
- [x] Phase 4: Polish (Haptics, Node Labels, GPS)
- [ ] Phase 5: Persistence (Room DB) & Large Media Transfer

---
*Developed as a resilient P2P mesh solution.*
