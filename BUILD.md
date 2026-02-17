# Build and Installation Guide

## Prerequisites

- **Java 17 or higher**
- **Maven 3.6+**
- **Spigot/Paper server** (1.20.4 or higher)
- **ProtocolLib** plugin installed

## Building from Source

1. Clone the repository:
```bash
git clone https://github.com/felushort/Bloodlock.git
cd Bloodlock
```

2. Build with Maven:
```bash
mvn clean package
```

3. The compiled JAR will be in `target/echo-anticheat-1.0.0-ALPHA.jar`

## Installation

1. Ensure **ProtocolLib** is installed on your server
2. Copy `echo-anticheat-1.0.0-ALPHA.jar` to your `plugins/` folder
3. Restart your server
4. Configure `plugins/ECHO/config.yml` to your preferences
5. Reload with `/echo reload` or restart again

## Quick Start

After installation, ECHO begins working immediately:

1. **Monitor Status**: `/echo status`
2. **Check Player**: `/echo check <player>`
3. **View Profile**: `/echo profile <player>`
4. **Configure**: Edit `plugins/ECHO/config.yml`
5. **Reload Config**: `/echo reload`

## Configuration Tips

### For Vanilla Survival
```yaml
entropy:
  thresholds:
    rotation-consistency: 0.85
    cps-variance-min: 0.15
    
integrity-pressure:
  activation-threshold: 45
```

### For Competitive PvP
```yaml
entropy:
  thresholds:
    rotation-consistency: 0.90  # Stricter
    cps-variance-min: 0.12      # Stricter
    
integrity-pressure:
  activation-threshold: 50      # More lenient activation
```

### For Large Servers (100+ players)
```yaml
performance:
  async-processing: true
  thread-pool-size: 8           # Increase threads
  batch-interval: 10            # Process less frequently

capture:
  buffer-size: 45               # Reduce buffer to save memory
```

## Troubleshooting

### "ProtocolLib not found!"
**Solution**: Download ProtocolLib from SpigotMC and install it before ECHO.

### High CPU usage
**Solution**: 
- Reduce `thread-pool-size` in config
- Increase `batch-interval`
- Disable some layers temporarily

### Players complaining about false positives
**Solution**:
- Increase deviation thresholds in `profiling` section
- Increase `min-playtime` to establish better baselines
- Check if `integrity-pressure.activation-threshold` is too low

### Scores not updating
**Solution**:
- Check that players have enough activity (need 50+ rotation and 25+ click samples)
- Verify `transparency.show-scores` is enabled
- Check console for errors

## Performance Benchmarks

Tested on:
- **Server**: Paper 1.20.4
- **Hardware**: 4 cores @ 3.5GHz, 8GB RAM
- **Players**: 100 concurrent

**Results**:
- CPU overhead: ~4.2%
- RAM usage: ~120MB
- TPS impact: < 0.5

## Development

### Project Structure
```
src/main/java/com/void/echo/
├── EchoPlugin.java           # Main plugin
├── capture/                  # Layer 1: Packet capture
├── analysis/                 # Layer 2: Entropy analysis
├── profile/                  # Layer 3: Profiling
├── physics/                  # Layer 4: Physics drift
├── pressure/                 # Layer 5: Integrity pressure
├── review/                   # Layer 6: Post-fight review
├── camouflage/               # Layer 7: Camouflage detection
├── identity/                 # Layer 8: Identity anchoring
├── transparency/             # Layer 9: Transparency
├── chaos/                    # Layer 10: Chaos events
├── data/                     # Data models
├── util/                     # Utilities
└── command/                  # Commands
```

### Adding New Checks

To add a new behavioral check:

1. Add the check logic to `EntropyAnalyzer`
2. Update `EntropyAnalysisResult` with new scores
3. Adjust weights in `config.yml`
4. Update `EchoCommandExecutor` to display new data

### Extending Storage

Current implementation uses in-memory caching. To add persistent storage:

1. Implement `loadProfile()` in `PlayerDataManager`
2. Implement `saveProfile()` in `PlayerDataManager`
3. Add database dependency to `pom.xml`
4. Add connection config to `config.yml`

## Support

For issues, questions, or contributions:
- **GitHub**: https://github.com/felushort/Bloodlock
- **Discord**: [Coming Soon]
- **Documentation**: See README.md

## License

Proprietary - All Rights Reserved
