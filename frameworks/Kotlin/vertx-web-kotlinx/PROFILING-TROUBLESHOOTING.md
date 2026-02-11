# Profiling Troubleshooting Guide

## Issue: Empty Flame Graph (profile-jdbc.html shows only "all")

### Root Cause

The `profile-jdbc.html` file is essentially empty (only shows "all" with no stack traces) because **async-profiler cannot collect CPU profiling samples** due to restrictive kernel security settings.

Your system has `perf_event_paranoid` set to `4`, which blocks async-profiler from using perf events for CPU profiling.

### Verification

Check your current setting:
```bash
cat /proc/sys/kernel/perf_event_paranoid
```

If the value is `2` or higher, async-profiler will have limited or no access to CPU profiling events.

### Solutions

#### Option 1: Temporarily Lower perf_event_paranoid (Recommended for Development)

```bash
# Temporarily set to -1 (allow all profiling)
sudo sysctl kernel.perf_event_paranoid=-1

# Or set to 1 (less permissive but usually sufficient)
sudo sysctl kernel.perf_event_paranoid=1
```

This change is temporary and will be reset on reboot.

#### Option 2: Permanently Lower perf_event_paranoid

```bash
# Add to /etc/sysctl.conf or /etc/sysctl.d/99-perf.conf
echo "kernel.perf_event_paranoid = -1" | sudo tee -a /etc/sysctl.d/99-perf.conf
sudo sysctl -p /etc/sysctl.d/99-perf.conf
```

#### Option 3: Use Alternative Profiling Methods

If you cannot change system settings, async-profiler supports alternative methods:

1. **Use `-e itimer` instead of `-e cpu`** (uses SIGPROF instead of perf events):
   ```bash
   asprof start -e itimer -o flamegraph $JAVA_PID
   ```

   Modify the script line 93:
   ```bash
   $ASPROF_CMD start -e itimer -o flamegraph $JAVA_PID
   ```

2. **Run as root** (not recommended for security reasons):
   ```bash
   sudo ./profile-benchmark.sh jdbc
   ```

#### Option 4: Use Java Flight Recorder (JFR) Output

JFR doesn't require perf events:
```bash
# Start with JFR output
asprof start -e cpu -o jfr -f profile.jfr $JAVA_PID
# Then convert to flamegraph
asprof stop -o flamegraph -f profile.html $JAVA_PID
```

### Understanding perf_event_paranoid Levels

- **-1**: No restrictions (allow all profiling)
- **0**: Allow access to CPU events and kernel profiling for all users
- **1**: Disallow kernel profiling for unprivileged users
- **2** (default on most systems): Disallow CPU events for unprivileged users
- **3+**: Very restrictive - blocks most profiling operations

### Recommended Fix for Your Setup

Run this before profiling:
```bash
sudo sysctl kernel.perf_event_paranoid=-1
./profile-benchmark.sh jdbc /home/shreckye/Apps/async-profiler-4.3-linux-x64/bin
```

### Why the database profile worked but jdbc didn't

This likely indicates that:
1. You ran the database profiling with different permissions
2. OR the perf_event_paranoid setting was changed between runs
3. OR there was already a profiling session active that was terminated differently

### Verification After Fix

After applying the fix, you should see:
- A much larger profile HTML file (e.g., 79K vs 13K)
- Many stack frames in the flame graph
- Actual method names and call hierarchies

Compare your results:
- **Empty profile (jdbc)**: 13K, ~358 lines, only "all" in cpool
- **Good profile (database)**: 79K, ~5085 lines, hundreds of methods in cpool

### Additional Notes

1. **Security Implications**: Setting `perf_event_paranoid=-1` allows any user to profile any process. In a development environment, this is usually acceptable. On production systems, use more restrictive settings.

2. **Alternative on Production**: Consider using Java Mission Control (JMC) with Java Flight Recorder (JFR) which doesn't require perf events and has less system impact.

3. **Container Environments**: If running in Docker/containers, you may need to:
   - Run container with `--privileged` flag
   - Or mount `/sys/kernel/debug` with proper permissions
   - Or use `--cap-add=SYS_ADMIN` and `--cap-add=SYS_PTRACE`
