# ECHO - Server Owner Guide
## Easy Command Reference & LuckPerms Integration

---

## 📋 Quick Start Guide

### Installation
1. Install ProtocolLib (required)
2. Install LuckPerms (optional, but recommended)
3. Drop ECHO.jar into plugins folder
4. Start server
5. Configure `plugins/ECHO/config.yml`
6. Set up permissions

---

## 🎮 Command Reference

### Basic Commands

#### `/echo status`
**Permission**: `echo.admin`  
**Description**: View complete system status  
**Usage**: `/echo status`

Shows:
- Online player count
- Monitored players
- Packet processing stats
- All 10 layer statuses
- System health

**Example Output**:
```
═══════════════════════
ECHO - Behavioral Integrity Engine
═══════════════════════
Version: 1.0.0-ALPHA
Online Players: 45
Monitored Players: 45

Active Layers:
  • ✓ Layer 1: Input Capture - ACTIVE
  • ✓ Layer 2: Entropy Analysis - ACTIVE
  ...
```

---

#### `/echo check <player>`
**Permission**: `echo.viewscore`  
**Description**: Perform integrity check on a player  
**Usage**: `/echo check PlayerName`

Shows:
- Human probability score (0-100)
- Component scores (yaw, pitch, clicks, etc.)
- Current integrity rating
- Flagged status
- Sample counts

**Score Colors**:
- §a**Green (80-100)**: Clean player
- §e**Yellow (60-79)**: Normal behavior
- §6**Orange (40-59)**: Suspicious
- §c**Red (20-39)**: Very suspicious
- §4**Dark Red (<20)**: Extremely suspicious

**Example**:
```
/echo check Hacker123

Human Probability: §c25.3/100
Status: Extremely Suspicious Behavior

Component Scores:
  • Yaw Entropy: 15.2
  • Pitch Entropy: 18.4
  • Click Distribution: 8.5
  ...
```

---

#### `/echo profile <player>`
**Permission**: `echo.admin`  
**Description**: View detailed player profile  
**Usage**: `/echo profile PlayerName`

Shows:
- Session information
- Total playtime
- Baseline statistics (CPS, reaction time, accuracy)
- Integrity history
- Violation count

**Example**:
```
/echo profile PlayerName

Session Info:
  • Session Count: 15
  • Total Playtime: 12 hours
  
Baseline Statistics:
  • Avg CPS: 8.5
  • Avg Reaction: 210ms
  • Hit Accuracy: 65.4%
```

---

#### `/echo violations <player> [page]`
**Permission**: `echo.admin`  
**Description**: View player's violation history  
**Usage**: `/echo violations PlayerName [page]`

Features:
- Paginated (10 violations per page)
- Timestamped entries
- Violation types and details

**Example**:
```
/echo violations Hacker123 1

Violations: Hacker123 (Page 1/3)
1. [Aimbot] Snap detection - 0.95 confidence
2. [Autoclicker] CPS burst detected - 24 CPS
...
```

---

#### `/echo info <player>`
**Permission**: `echo.admin`  
**Description**: Quick player overview  
**Usage**: `/echo info PlayerName`

Shows:
- UUID, ping, client
- Current integrity score
- Violation count
- Flagged status
- Active exemptions

**Example**:
```
/echo info PlayerName

UUID: abc123...
Ping: 45ms
Client: vanilla

ECHO Status:
  • Integrity Score: 85.2/100
  • Violations: 0
  • Flagged: No
  • Exemptions: none
```

---

### Data Management Commands

#### `/echo reset <player> [type]`
**Permission**: `echo.admin`  
**Description**: Reset player data  
**Usage**: 
- `/echo reset PlayerName all` - Reset everything
- `/echo reset PlayerName violations` - Clear violations only
- `/echo reset PlayerName baseline` - Reset behavioral baseline

**When to use**:
- `all`: Complete fresh start for player
- `violations`: Clear false positives
- `baseline`: Player changed hardware/settings

**Example**:
```
/echo reset PlayerName violations
✓ Cleared violations for PlayerName
```

---

#### `/echo exempt <player> <add|remove|list> [check]`
**Permission**: `echo.admin`  
**Description**: Manage check exemptions  
**Usage**:
- `/echo exempt PlayerName list` - View exemptions
- `/echo exempt PlayerName add aimbot` - Add exemption
- `/echo exempt PlayerName remove aimbot` - Remove exemption

**Available Checks**:
- `all` - Exempt from everything
- `aimbot` - Aimbot detection
- `autoclicker` - Autoclicker detection
- `killaura` - KillAura detection
- `reach` - Reach detection
- `timer` - Timer detection
- `velocity` - Velocity/movement detection
- `scaffold` - Scaffold detection
- `fly` - Fly detection

**Example**:
```
/echo exempt AdminPlayer add all
✓ Added exemption for AdminPlayer: all

/echo exempt PlayerName list
Exemptions for PlayerName: aimbot, reach
```

---

### Alert & Debug Commands

#### `/echo alerts [on|off|toggle]`
**Permission**: `echo.alerts`  
**Description**: Toggle alert subscriptions  
**Usage**: `/echo alerts` or `/echo alerts on/off`

**Example**:
```
/echo alerts on
✓ Alerts enabled

/echo alerts off
✗ Alerts disabled
```

---

#### `/echo verbose [on|off]`
**Permission**: `echo.verbose`  
**Description**: Toggle verbose debug mode  
**Usage**: `/echo verbose` or `/echo verbose on/off`

When enabled:
- See detailed detection output
- View internal calculations
- Debug information
- Processing stats

**Use for**: Troubleshooting, understanding detections

**Example**:
```
/echo verbose on
✓ Verbose mode enabled - detailed debug output active

[VERBOSE] Processing rotation packet from PlayerName
[VERBOSE] Yaw delta: 15.2° | Pitch delta: 3.1°
...
```

---

#### `/echo test <player> <type>`
**Permission**: `echo.admin`  
**Description**: Manually trigger detection test  
**Usage**: `/echo test PlayerName aimbot`

**Available Tests**:
- aimbot
- autoclicker
- killaura
- reach
- timer
- velocity

**Use for**: Testing detection algorithms, verifying exemptions

**Example**:
```
/echo test PlayerName aimbot
Running aimbot detection test on PlayerName...
Test complete. Check /echo check PlayerName
```

---

### System Commands

#### `/echo reload`
**Permission**: `echo.admin`  
**Description**: Reload configuration  
**Usage**: `/echo reload`

Reloads:
- config.yml settings
- All layer configurations
- Threshold values
- Enforcement settings

**Note**: Does NOT reset player data or profiles

**Example**:
```
/echo reload
✓ Configuration reloaded successfully! (45ms)
```

---

#### `/echo togglelayer <layer>`
**Permission**: `echo.admin`  
**Description**: Enable/disable specific layers  
**Usage**: `/echo togglelayer <1-10|name>`

**Note**: Currently requires config edit + reload. GUI coming soon.

---

#### `/echo logs [player]`
**Permission**: `echo.admin`  
**Description**: View detection logs  
**Usage**: `/echo logs` or `/echo logs PlayerName`

**Note**: Check `plugins/ECHO/violations.log` or console for now

---

#### `/echo export <player>`
**Permission**: `echo.admin`  
**Description**: Export player data  
**Usage**: `/echo export PlayerName`

**Note**: Exports to JSON format. Database access available for advanced users.

---

## 🔐 Permission Setup

### For Bukkit/Spigot (Without LuckPerms)

Edit `permissions.yml`:
```yaml
groups:
  admin:
    permissions:
      echo.admin: true
  
  moderator:
    permissions:
      echo.staff: true
      echo.viewscore: true
      echo.alerts: true
  
  helper:
    permissions:
      echo.alerts: true
      echo.command.check: true
```

---

### For LuckPerms (Recommended)

#### Setup Admin Group
```bash
/lp group admin permission set echo.admin true
```

This gives full ECHO access including:
- All commands
- All alerts
- Exemption management
- System configuration

---

#### Setup Moderator Group
```bash
/lp group mod permission set echo.staff true
```

This gives:
- View player scores
- Receive alerts
- Check player info
- View violations

---

#### Setup Helper Group
```bash
/lp group helper permission set echo.alerts true
/lp group helper permission set echo.command.check true
/lp group helper permission set echo.command.info true
```

This gives:
- Basic alerts
- Player checking
- Info viewing

---

#### Exempt Specific Players

**Exempt from all checks**:
```bash
/lp user AdminPlayer permission set echo.bypass true
```

**Exempt from specific check**:
```bash
/lp user YouTuber permission set echo.exempt.aimbot true
/lp user BuilderPlayer permission set echo.exempt.scaffold true
```

**Alert levels** (1=basic, 2=important, 3=critical):
```bash
/lp user ModPlayer permission set echo.alerts.level.3 true
/lp user HelperPlayer permission set echo.alerts.level.1 true
```

---

## 📊 Permission Reference

### Admin Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `echo.admin` | Full administrative access | OP |
| `echo.staff` | Staff member access | false |
| `echo.bypass` | Bypass all checks | false |

### View Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `echo.viewscore` | View integrity scores | OP |
| `echo.alerts` | Receive alerts | OP |
| `echo.verbose` | Verbose debug mode | OP |

### Alert Levels
| Permission | Description | Default |
|------------|-------------|---------|
| `echo.alerts.level.1` | Basic alerts | false |
| `echo.alerts.level.2` | Important alerts | false |
| `echo.alerts.level.3` | Critical alerts | false |

### Check Exemptions
| Permission | Description | Default |
|------------|-------------|---------|
| `echo.exempt.all` | Exempt from all checks | false |
| `echo.exempt.aimbot` | Exempt from aimbot detection | false |
| `echo.exempt.autoclicker` | Exempt from autoclicker | false |
| `echo.exempt.killaura` | Exempt from killaura | false |
| `echo.exempt.reach` | Exempt from reach | false |
| `echo.exempt.timer` | Exempt from timer | false |
| `echo.exempt.velocity` | Exempt from velocity | false |
| `echo.exempt.scaffold` | Exempt from scaffold | false |
| `echo.exempt.fly` | Exempt from fly | false |

### Command Permissions
Each command has its own permission: `echo.command.<command>`
- `echo.command.status`
- `echo.command.check`
- `echo.command.profile`
- `echo.command.violations`
- `echo.command.reset`
- `echo.command.exempt`
- etc.

---

## 🎯 Common Use Cases

### Scenario 1: New Player Joins
```
# System automatically:
- Starts capturing data
- Builds behavioral baseline
- No action needed from staff

# After 30+ minutes of play:
/echo check PlayerName
# View their integrity score
```

---

### Scenario 2: Suspicious Player
```
# Staff receives alert:
[ECHO] PlayerName flagged - Score: 25/100 (Aimbot: 0.85)

# Check the player:
/echo check PlayerName
/echo profile PlayerName
/echo violations PlayerName

# View detailed history:
/echo violations PlayerName 1
/echo violations PlayerName 2

# If confirmed cheating:
/ban PlayerName Cheating

# If false positive:
/echo reset PlayerName violations
```

---

### Scenario 3: Staff Member Testing
```
# Add exemption for staff testing mods:
/echo exempt StaffTester add all

# Or use LuckPerms:
/lp user StaffTester permission set echo.bypass true

# Remove when done:
/echo exempt StaffTester remove all
```

---

### Scenario 4: YouTuber/Streamer
```
# Exempt from specific checks that might false positive:
/echo exempt YouTuberName add killaura
/echo exempt YouTuberName add reach

# They can still be detected for other cheats!
```

---

### Scenario 5: Configuration Change
```
# Edit plugins/ECHO/config.yml
# Change thresholds, enable/disable layers, etc.

# Reload without restart:
/echo reload
```

---

## 💡 Tips & Best Practices

### For Server Owners

✅ **DO**:
- Install LuckPerms for better permission management
- Give staff `echo.staff` permission (not full admin)
- Enable transparency mode (players see their scores)
- Review violations before banning
- Whitelist trusted players/staff with exemptions

❌ **DON'T**:
- Give everyone `echo.bypass`
- Disable all layers (defeats the purpose)
- Auto-ban without review (system is statistical)
- Exempt too many players
- Ignore alerts

---

### For Staff

✅ **DO**:
- Use `/echo check` before accusing
- Review full `/echo profile` for context
- Check `/echo violations` for patterns
- Enable `/echo alerts` when on duty
- Use `/echo verbose` when investigating

❌ **DON'T**:
- Ban based on single low score
- Ignore high scores (could be false negative)
- Reset data without good reason
- Add exemptions without permission

---

### Recommended Setup

**1. Initial Setup**:
```bash
# Install dependencies
- ProtocolLib ✓
- LuckPerms ✓

# Set up groups
/lp creategroup staff
/lp group staff permission set echo.staff true

# Configure ECHO
Edit config.yml thresholds to your preference
/echo reload
```

**2. Staff Permissions**:
```bash
# Admin (full access)
/lp group admin permission set echo.admin true

# Moderator (alerts + checks)
/lp group mod permission set echo.staff true

# Helper (basic alerts)
/lp group helper permission set echo.alerts true
/lp group helper permission set echo.command.check true
```

**3. Exempt Staff**:
```bash
# Exempt staff from all checks
/lp group staff permission set echo.bypass true

# Or per-player:
/echo exempt AdminName add all
```

---

## 🔧 Troubleshooting

### Issue: Commands not working
**Solution**:
```bash
# Check permissions:
/lp user YourName permission check echo.admin

# Make sure you have OP or proper permissions
```

### Issue: No alerts appearing
**Solution**:
```bash
# Enable alerts:
/echo alerts on

# Check permission:
/lp user YourName permission check echo.alerts
```

### Issue: False positives
**Solution**:
```bash
# Adjust thresholds in config.yml
# OR exempt specific checks:
/echo exempt PlayerName add <check>

# OR clear violations:
/echo reset PlayerName violations
```

### Issue: Player score stuck at low value
**Solution**:
```bash
# Reset baseline (if hardware changed):
/echo reset PlayerName baseline

# Or full reset:
/echo reset PlayerName all
```

---

## 📚 Additional Resources

- **Config Guide**: See `config.yml` for all settings
- **Technical Docs**: See `TECHNICAL_SPEC.md`
- **GrimAC Comparison**: See `GRIMAC_COMPARISON.md`
- **Support**: GitHub Issues

---

## ⚡ Quick Reference Card

```
MOST USED COMMANDS:

/echo status              # System status
/echo check <player>      # Check player  
/echo violations <player> # View violations
/echo alerts on           # Enable alerts
/echo exempt <player> add <check>  # Add exemption
/echo reset <player> violations   # Clear violations
/echo reload              # Reload config

LUCKPERMS SHORTCUTS:

/lp group admin permission set echo.admin true
/lp user <player> permission set echo.bypass true
/lp user <player> permission set echo.exempt.aimbot true
```

---

**Made for server owners. Built for ease of use.** — ECHO
