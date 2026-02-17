# ECHO Anti-Cheat Checks Location Guide

This document explains where the anti-cheat checks are located in the ECHO codebase.

## Overview

ECHO doesn't use traditional "violation checks" (like `reach > 3.01 blocks`). Instead, it uses **behavioral analysis** across 10 integrated layers to determine if a player's actions look human or bot-like.

## Main Check Files

### 1. **EntropyAnalyzer.java** - Primary Behavioral Analysis
**Location:** `src/main/java/com/void/echo/analysis/EntropyAnalyzer.java`

This is the **main file** where checks happen. It analyzes:
- **Yaw/Pitch Entropy**: Detects unnatural consistency in mouse movements
- **Click Distribution**: Identifies autoclicker patterns vs. natural variance
- **Reaction Time Variance**: Detects bot-like consistency (too predictable)
- **Crosshair Alignment**: Catches aimbots (too many perfect snaps to hitbox)

**Output:** Human Probability Score (0-100, where 100 = perfectly human-like)

### 2. **PlayerProfile.java** + **PlayerDataManager.java** - Baseline Deviation Detection
**Location:** 
- `src/main/java/com/void/echo/data/PlayerProfile.java`
- `src/main/java/com/void/echo/data/PlayerDataManager.java`

Compares each player **to themselves** over time to detect:
- Sudden behavioral shifts (e.g., reaction time drops from 210ms → 80ms)
- Toggle-based cheating patterns
- Uses z-score calculations for deviation detection

### 3. **CamouflageDetector.java** - Context-Aware Behavior Detection
**Location:** `src/main/java/com/void/echo/camouflage/CamouflageDetector.java`

Detects players who:
- Change behavior when staff are spectating
- Toggle cheats on/off based on context
- Modify patterns when they know they're being watched

### 4. **IntegrityPressureManager.java** - Enforcement Logic
**Location:** `src/main/java/com/void/echo/pressure/IntegrityPressureManager.java`

Applies restrictions based on integrity score:
- **Light Pressure** (30-45 score): Stricter raytrace validation, 5% reach reduction
- **Medium Pressure** (15-30 score): +5% knockback, packet order enforcement
- **Heavy Pressure** (0-15 score): 10% knockback, 15% reach reduction, double-check rotations

### 5. **PostFightReviewer.java** - Deep Offline Analysis
**Location:** `src/main/java/com/void/echo/review/PostFightReviewer.java`

Records intense fights for neural review:
- Queue-based async processing
- Detailed statistical analysis of fight patterns
- Cross-correlation analysis of multiple behavioral signals

## Supporting Systems

### Packet Capture (Input Layer)
**Location:** `src/main/java/com/void/echo/capture/PacketCaptureManager.java`

Captures every rotation, movement, attack, block place, and sprint toggle with microsecond-level timing precision.

### Physics Drift (Cheat Destabilization)
**Location:** `src/main/java/com/void/echo/physics/PhysicsDriftManager.java`

Introduces microscopic physics variations that destabilize cheat clients tuned for vanilla constants.

### Chaos Events (Stress Testing)
**Location:** `src/main/java/com/void/echo/chaos/ChaosEventManager.java`

Randomly triggers integrity stress tests that humans adapt to unconsciously but rigid automation fails.

### Identity Anchoring (Anti-Spoof)
**Location:** `src/main/java/com/void/echo/identity/IdentityAnchor.java`

Builds behavioral biometric fingerprints to detect:
- Client changes
- Identity spoofing
- Hardware/software switches

## Data Models

All check results are stored in structured data models:

- **MovementData.java** - Movement patterns and velocity data
- **ClickData.java** - Click timing and patterns
- **RotationData.java** - Yaw/pitch rotation history
- **BehavioralBuffer.java** - Time-series buffer for pattern analysis

## Quick Reference

**"Where are the checks?"** → **EntropyAnalyzer.java** is the main file

**"What gets checked?"** → Behavioral patterns (entropy, consistency, alignment)

**"How are violations enforced?"** → **IntegrityPressureManager.java** applies silent restrictions

**"Where is raw data captured?"** → **PacketCaptureManager.java** intercepts all packets

## Architecture Summary

```
Player Actions (Packets)
    ↓
PacketCaptureManager.java (Layer 1: Capture)
    ↓
EntropyAnalyzer.java (Layer 2: Statistical Analysis)
    ↓
PlayerProfile.java (Layer 3: Compare to Personal Baseline)
    ↓
CamouflageDetector.java (Layer 7: Context Awareness)
    ↓
IntegrityPressureManager.java (Layer 5: Apply Restrictions)
    ↓
PostFightReviewer.java (Layer 6: Deep Analysis if Needed)
```

## For Developers

When adding new checks:
1. **Data capture**: Add packet listeners in `PacketCaptureManager.java`
2. **Analysis logic**: Add statistical methods in `EntropyAnalyzer.java`
3. **Data models**: Create structures in `src/main/java/com/void/echo/data/`
4. **Enforcement**: Configure responses in `IntegrityPressureManager.java`

## For Server Admins

- Check player scores: `/echo check <player>`
- View detailed profile: `/echo profile <player>`
- Configure thresholds: Edit `plugins/ECHO/config.yml`
- View alerts: `/echo alerts on`
