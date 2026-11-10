# Infinix GT 10 Pro (X6739): knob write experiments

Date: 2026-11-11 ~00:00-00:20 device time. Device `192.168.1.194:34985`, root via `su` (`u:r:ksu:s0`). All commands below ran as root in a shell script pushed to the device (the `su -c 'for ...'` one-liner form breaks on this `sh`; push a script file instead, see section 9). Companion to `knobs.md` (read-only survey).

Conventions: **VERIFIED** = observed on this device in this session. **NOT VERIFIED** = could not be tested. Screen was on with `com.anthropic.claude` in the foreground and the GPU idle for the whole session (no game ran), so anything that needs real load is flagged.

Restore status: every node was written back and re-read; see section 10. Three items differ from the pre-test snapshot and none is a failed restore: dynamic `scaling_cur_freq`, `/proc/game_mode` "Lock point distance" (changed on its own, before any of my writes), and the GPU `FPSGO` row of `limit_table` (now `0 40` instead of `-1 -1`, functionally identical).

---

## 1. CPU caps (`scaling_max_freq` / `scaling_min_freq`)

### Max cap: VERIFIED, works and sticks

```sh
C=/sys/devices/system/cpu/cpufreq
echo 1000000 > $C/policy0/scaling_max_freq     # LITTLE
echo 1046000 > $C/policy4/scaling_max_freq     # big
echo 1108000 > $C/policy7/scaling_max_freq     # prime
# release
echo 2000000 > $C/policy0/scaling_max_freq
echo 2600000 > $C/policy4/scaling_max_freq
echo 3000000 > $C/policy7/scaling_max_freq
```

- Plain kHz integer. Write rc=0, readback identical.
- Held for 60 s with all three caps set at once, polled every 0.5 s: `t=0s p0=500000-1000000 p4=437000-1046000 p7=659000-1108000`, no change for the whole minute. Nothing (PPM, perf HAL, `x6739_tuning` watcher) reset the max cap. Earlier single-policy runs of 20 s each also held.
- `scaling_cur_freq` obeys the cap. policy0 capped at 1000000 showed `cur` 850000-925000; policy7 capped at 1108000 showed 659000 / 921000.
- **Use exact OPP values.** policy4 was first tried with 1000000 (not in its OPP list). Readback was `1000000` but `scaling_cur_freq` reported `1046000` (the next OPP up), i.e. a non-OPP cap is rounded up. Pick a value from `scaling_available_frequencies`.
- Good conservative Hemat caps (all are OPPs): p0 1000000, p4 1046000, p7 1108000. A caller wanting a stronger cap can pick lower OPPs; only these were run.

### Min floor: works, but gets reset by the system (not reliable)

```sh
echo 1450000 > $C/policy0/scaling_min_freq
echo 1451000 > $C/policy4/scaling_min_freq
echo 1370000 > $C/policy7/scaling_min_freq
# release
echo 500000 > $C/policy0/scaling_min_freq
echo 437000 > $C/policy4/scaling_min_freq
echo 659000 > $C/policy7/scaling_min_freq
```

- Write rc=0, readback identical, `scaling_cur_freq` never dropped below the floor while it held (policy4 at floor 1451000: every sample in a 20 s run was 1451000 or higher).
- **Something resets it.** In the first run (5 s spacing) all three policies read back the default min (`500000`, `437000`, `659000`) by t+5 s. Two later runs held for 20 s (p4) and 20 s (p0, p7). A third p0 run held 16 s then dropped to `500000`. A run where I wrote min 1450000 then max 1000000 on policy0 saw both overwritten within 2 s: `min=1000000 max=1000000` (kernel clamp, see below), then `min=1350000 max=1725000` at t=1 s, then `min=500000 max=2000000` at t=2 s. That `1350000/1725000` pair is not something I wrote, so a userspace writer (suspect the power HAL `android.hardware.power-service.pixel-libperfmgr`, pid 1817, or the Transsion perf stack; not identified for certain) rewrites the limits. It is intermittent. The max-only runs were never touched.
- Conclusion: an app must re-assert min floors periodically (every few seconds) and verify by reading back. Max caps in the same conditions held for 60 s.
- Kernel clamping behaviour (VERIFIED): lowering max below min pushes min down to max (`min=1000000 max=1000000`); writing min above max clamps min up to max (`echo 3000000 > policy0/scaling_min_freq` gave rc=0, `min=2000000 max=2000000`). Always write in an order that does not need clamping: when lowering, write max first only if the new max is above current min, otherwise lower min first; when raising, write max before min.
- `/proc/ppm/dump_policy_list` stayed at `Final limit` `(15)(0)(4)(4)/(15)(0)(3)(3)/(15)(0)(1)(1)` the whole time (read-only check): PPM is not the thing limiting or resetting; the cpufreq max/min writes act directly.

Unverified: behaviour under heavy load (CPU was mostly idle/light). Effect on thermal not examined (out of scope).

---

## 2. schedhorizon tunables: VERIFIED

```sh
C=/sys/devices/system/cpu/cpufreq
echo 0 > $C/policy0/schedhorizon/up_delay
echo 1000000 > $C/policy0/schedhorizon/efficient_freq
```

- Both accept writes, readback exact, and they stuck. I waited 35 s (longer than the `x6739_tuning` 30 s watcher cycle) with p7 `efficient_freq` non-zero: the watcher did not fire and nothing reverted the values.
- `up_delay 0` on all three: accepted (`p0 up=0 eff=1450000`, `p4 up=0 eff=1740000`, `p7 up=0 eff=1998000`). Values are plain integers.
- `efficient_freq` accepts OPP values (1000000, 1046000, 1258000 all read back as written) and also non-OPP values: `1234567` read back as `1234567` with rc=0, no snapping.
- Garbage input: `echo abc > up_delay` returned rc=0 and the node then read `0` (parsed as 0). Do not rely on rejection of bad input; write only validated integers.
- Do NOT write `0` to `policy7/schedhorizon/efficient_freq` (watcher treats `0` as reset). Never done.
- Observable effect of `up_delay 0` / lower `efficient_freq`: frequency samples were noisy (device lightly loaded and the owner interacting), so no clean quantitative ramp-speed difference was measured. `scaling_cur_freq` after the 35 s wait was high (p0 2000000, p4 2200000, p7 2713000) with up_delay 0/100 mix, consistent with fast ramping but not conclusive. NOT VERIFIED: actual latency improvement; needs a measured workload.
- `rate_limit_us` was read (1000) but not written (not in the task list).

Restored: p0 eff 1450000 / up 20, p4 eff 1740000 / up 30, p7 eff 1998000 / up 40 (read back, matches).

---

## 3. GPU cap and floor: VERIFIED format, effect on clocks NOT VERIFIED (GPU idle)

```sh
G=/sys/kernel/ged/hal
echo 20 > $G/custom_upbound_gpu_freq     # ceiling at OPP index 20 (715000 kHz)
echo 0  > $G/custom_upbound_gpu_freq     # release ceiling (0 = no cap)
echo 20 > $G/custom_boost_gpu_freq       # floor at OPP index 20
echo 40 > $G/custom_boost_gpu_freq       # release floor (40 = lowest OPP = no floor)
```

- Format is a plain OPP index (0 = 886000 kHz fastest, 40 = 350000 kHz slowest). rc=0, readback exact.
- The write shows up in `/proc/gpufreqv2/limit_table` as the **FPSGO** row (id 10, priority 4), and in `gpufreq_status`:

```
# after: echo 20 > custom_upbound_gpu_freq
  10           FPSGO           4         20       -1           1           1
[GPU-LimitC]    CeilingIndex: 20, Limiter: 10, Priority: 4
[GPU-LimitF]    FloorIndex: 40, Limiter: 0, Priority: 9

# after: echo 20 > custom_boost_gpu_freq
  10           FPSGO           4          0       20           1           1
[GPU-LimitC]    CeilingIndex: 0, Limiter: 0, Priority: 9
[GPU-LimitF]    FloorIndex: 20, Limiter: 10, Priority: 4
```

  So a ceiling and a floor each appear as a real limiter with `Limiter: 10` (FPSGO). `30` and `10` also worked the same way. The row is the one FPSGO itself uses, so a running game's FPSGO could rewrite it (NOT VERIFIED either way).
- `echo 99 > custom_upbound_gpu_freq` was accepted (rc=0) and clamped to 40 (`up=40`, ceiling 40 = **slowest OPP, a hard cap at 350 MHz**). Never write out-of-range values; the clamp goes to the harsh end. Always validate 0..40.
- `echo -1 > custom_upbound_gpu_freq` read back `0` (treated as 0 / no cap), rc=0; it does not restore the original `-1` shown in `limit_table`.
- Effect on actual clocks: **not verified**. The GPU was idle/powered off, `current_freqency` stayed `0 886000` throughout (it is the last programmed OPP, not a live clock when idle). `limit_table`/`gpufreq_status` do confirm the ceiling/floor is registered and enforced by the limiter chain, which is what the clamp uses.
- Side note: after the restore, the FPSGO row reads `0 40` (ceiling 0, floor 40) rather than the survey's `-1 -1`. Both mean no limit; the nodes cannot write `-1` back.

Restored: `custom_upbound_gpu_freq=0`, `custom_boost_gpu_freq=40`, `gpufreq_status` `CeilingIndex: 0, Limiter: 0, Priority: 9` / `FloorIndex: 40, Limiter: 0, Priority: 9`.

---

## 4. Refresh rate: VERIFIED

Original (re-read at the start): `peak_refresh_rate=120.0`, `min_refresh_rate=60.0`.

```sh
settings put system peak_refresh_rate 60.0;  settings put system min_refresh_rate 60.0   # lock 60 Hz
settings put system peak_refresh_rate 90.0;  settings put system min_refresh_rate 90.0   # lock 90 Hz
settings put system peak_refresh_rate 120.0; settings put system min_refresh_rate 120.0  # lock 120 Hz
settings put system peak_refresh_rate 120.0; settings put system min_refresh_rate 60.0   # restore (auto 60..120)
```

Observed after each (4 s settle), `dumpsys display` `mActiveModeId` (framework id: 1=120, 2=90, 3=60) and `dumpsys SurfaceFlinger` `renderRate` / `activeMode`:

| peak / min | mActiveModeId | SF renderRate | SF activeMode |
|---|---|---|---|
| 120 / 60 (original) | 1 | 120.00 Hz | `id=0 vsyncRate=120.00` (screen busy at that moment) |
| 60 / 60 | 3 | 60.00 Hz | `id=2 ... 60.00 Hz` |
| 90 / 90 | 2 | 90.00 Hz | `id=1 ... 90.00 Hz` |
| 120 / 120 | 1 | 120.00 Hz | `id=0 ... 120.00 Hz` |
| 120 / 60 | 1 | 60.00 Hz | `id=2 ... 60.00 Hz` (idle content, SF drops to the floor) |
| 90 / 60 | 1 (!) | 60.00 Hz | `id=2 ... 60.00 Hz` |
| 60 / 0.0 | 1 (!) | 60.00 Hz | `id=2 ... 60.00 Hz` |

- To **lock** a rate: write the same value to both `peak_refresh_rate` and `min_refresh_rate`. This is the only combination that gave a deterministic, content-independent mode (60, 90, 120 all verified via `renderRate=` and `activeMode` in SurfaceFlinger).
- When `min < peak`, the display floats between them depending on content; at idle the SF render rate sat at `60.00 Hz` regardless of peak. Framework `mActiveModeId` can show `1` (120 Hz) while SF's render rate is 60: **use `dumpsys SurfaceFlinger | grep -E "renderRate=|activeMode="` as the truth**, not `mActiveModeId`.
- `peak=60` with `min=0.0` also resulted in 60 Hz (peak caps). Hemat can use `peak=60 min=60`.
- Settings applied within the 4 s settle; no screen activity was needed for the switch to take effect in this run (the screen was on).
- `refresh_rate_config_custom` (`com.garena.game.codm,120;`): the string `refresh_rate_config_custom` exists inside `/system/framework/framework.jar` and `/system/framework/services.jar` (found by `grep -rl`), so the ROM's framework code reads it, i.e. it is not a leftover. Whether it overrides the rate for a foreground package was **NOT VERIFIED** because that needs launching an app of the list while a test entry is in place, and the only launch I was allowed (Solar Smash, exp. 6) was skipped. No write to this key was done.

Restored: `peak_refresh_rate=120.0`, `min_refresh_rate=60.0` (read back).

---

## 5. Touch: VERIFIED

```sh
echo 1 > /proc/tp_report_rate_switch            # also accepts 2
echo 0 > /proc/tp_report_rate_switch            # default
echo 1 > /proc/tp_transient_report_rate_switch  # accepts 0, 1 only
echo 0 > /proc/tp_transient_report_rate_switch
echo 3 > /proc/game_mode                         # sets "Gear position"; 0..5 accepted
echo 0 > /proc/game_mode
```

- All three nodes are `-rw-rw-rw- root root u:object_r:proc:s0`, world-writable, no root needed.
- `tp_report_rate_switch`: writing `1` and `2` stuck (readback `1`, `2`, still the same after 1 s); `0` back to default. No help text on `cat` (just the number).
- `tp_transient_report_rate_switch`: `1` and `0` accepted; `2` was rejected (rc=1, value stayed `1`).
- The meaning of each number in Hz (1 = e.g. 240 Hz?) is **NOT VERIFIED**; I only proved what values the driver accepts. Measuring the real report rate needs `getevent -lt /dev/input/event4` with touch, which I was told not to inject.
- `/proc/game_mode`: `cat` returns `Gear position:N` and `Lock point distance:M`. Accepted writes: single integers `0 1 2 3 5` (rc=0, `Gear position` follows: 1->1, 2->2, 3->3, 5->5). `9` rejected (rc=1, value unchanged). Two-field writes (`"1 1"`, `"0 0"`) were rejected (rc=1). So the accepted range is at least 0..5 (4 was not tried, 6..8 not tried).
- "Lock point distance": the value is **not written by this node's single-integer write**. It read `1` in the survey and in my first snapshot, `0` at my very first node read of this experiment (before I wrote anything), and `0` afterward. It changes on its own (something else, perhaps the touch HAL or screen state, drives it). Treat it as not controllable here.
- No help text exists on any of these nodes.

Restored: both report-rate nodes `0`, `game_mode` `Gear position:0`.

---

## 6. FPS source (`fpsgo_status` with a game running): SKIPPED

The precondition was a focused window of `com.gtarena` or the launcher. At the start and again before the end `dumpsys window | grep mCurrentFocus` returned `com.anthropic.claude/com.anthropic.claude.mainactivity.MainActivity` (final check: `NotificationShade`), so the owner was using another app. I did not launch Solar Smash. No row format, no pick-the-game-row rule, no `gpu_utilization` field meaning under load.

Still unknown (from knobs.md section 12): the row layout of `fstb/fpsgo_status` with a game, which `gpu_utilization` field is load, and whether the FPSGO GPU limiter row gets overwritten by a running game. Suggested follow-up when the owner is idle on the launcher: run the capture sequence from the experiment plan (start Solar Smash with `am start -n`, 3 reads of `fpsgo_status` one second apart, plus `gpu_utilization` and `current_freqency`).

---

## 7. Focus-mode commands: VERIFIED

### Do Not Disturb

```sh
cmd notification set_dnd off        # zen_mode 0, ZEN_MODE_OFF, filter 1
cmd notification set_dnd priority   # zen_mode 1, ZEN_MODE_IMPORTANT_INTERRUPTIONS, filter 2
cmd notification set_dnd none       # zen_mode 2, ZEN_MODE_NO_INTERRUPTIONS (total silence), filter 3  (also "on")
cmd notification set_dnd alarms     # zen_mode 3, ZEN_MODE_ALARMS, filter 4
cmd notification set_dnd all        # same as off
settings get global zen_mode        # read current state
dumpsys notification | grep -E "mZenMode=|mInterruptionFilter"
```

- Usage from `cmd notification help`: `set_dnd [on|none (same as on)|priority|alarms|all|off (same as all)]`. All five tested ones returned rc=0 with no output; state changed immediately each time (table above).
- Original state at the start was **zen_mode=1 (priority)**, not off. A "reset" must therefore restore the previously stored value, not blindly `off`. Read `settings get global zen_mode` before changing and write it back with the matching `set_dnd` word (0=off, 1=priority, 2=none, 3=alarms).
- There is no `get_dnd` command (`Unknown command: get_dnd`); use the settings value.

### Brightness mode

```sh
settings get system screen_brightness_mode        # 0 manual, 1 auto
settings put system screen_brightness_mode 1
settings put system screen_brightness_mode 0
settings put system screen_brightness 4           # level 0..255
```

- Original: `mode=0`, `screen_brightness=4`. After `put ... mode 1` the mode read `1` and brightness drifted to `6` (auto picked a level). After `put ... mode 0` the mode returned to `0` but the level stayed `6`, so **the level must be restored explicitly**: `settings put system screen_brightness 4` (done, read back `4`).

### Kill background processes

```sh
am kill-all
```

- `cmd activity help`: "kill-all: Kill all processes that are safe to kill (cached, etc)". Ran once: rc=0, process count 825 -> 799, `MemFree` 108744 kB -> 1572112 kB, `MemAvailable` 2458132 kB -> 4003192 kB. Foreground app untouched (focus unchanged). This is a one-shot, not restorable; it does not need restoring.

---

## 8. Heartbeat directory: inspected only

```
$ ls -laZ /data/adb/        (excerpt)
drwx------ 11 root root u:object_r:adb_data_file:s0  .
drwxr-xr-x  2 root root u:object_r:adb_data_file:s0  service.d
drwx------ 15 root root u:object_r:adb_data_file:s0  modules
$ ls -laZ /data/adb/gtarena
ls: /data/adb/gtarena: No such file or directory
```

- `/data/adb/gtarena/` does **not** exist (not created, as instructed).
- `/data/adb` is `drwx------ root root u:object_r:adb_data_file:s0`. Nothing but root can traverse it, so a non-root process (the app itself) can neither read nor write there; only a `su` shell can. A directory created by a root shell inside it would inherit type `adb_data_file` (siblings `service.d`, `magic_mount`, `hp-backup` all have it) and default mode follows umask (siblings are `drwx------` or `drwxr-xr-x`); the exact mode/context was not tested because creating it was not allowed. The root shell context is `u:r:ksu:s0`.
- Consequence for a heartbeat: the app cannot read a heartbeat file under `/data/adb/gtarena/` without `su`. If a non-root reader is needed, `/data/local/tmp` is `drwxrwx--x shell shell u:object_r:shell_data_file:s0` and is what the existing hwfix.log uses.

---

## 9. Operational notes

- A one-liner `adb shell su -c 'for v in 3 5; do ...; done'` failed with `syntax error: unexpected 'do'` (nested quoting through adb/su). All multi-line tests were pushed as script files to `/data/local/tmp/gta_*.sh` and run with `su -c 'sh /data/local/tmp/gta_x.sh'`; the scripts were deleted at the end (`ls /data/local/tmp | grep gta` empty).
- `dumpsys ... | grep -m1` prints "Failed to write while dumping service ...: Broken pipe". Harmless; use `grep` without `-m` or redirect stderr.
- Git Bash needs `MSYS_NO_PATHCONV=1` on every adb call.

---

## 10. Restore proof

Pre-test snapshot vs. post-test snapshot (same script, `diff` of the two outputs):

```
node                                     before                after
policy0 min/max                          500000 / 2000000      500000 / 2000000
policy4 min/max                          437000 / 2600000      437000 / 2600000
policy7 min/max                          659000 / 3000000      659000 / 3000000
policy0 efficient_freq / up_delay        1450000 / 20          1450000 / 20
policy4 efficient_freq / up_delay        1740000 / 30          1740000 / 30
policy7 efficient_freq / up_delay        1998000 / 40          1998000 / 40
governor (all three)                     schedhorizon          schedhorizon
ged custom_boost_gpu_freq                40                    40
ged custom_upbound_gpu_freq              0                     0
ged gpu_boost_level                      0                     0
gpufreq_status LimitC / LimitF           idx 0 Lim 0 / idx 40 Lim 0   idx 0 Lim 0 / idx 40 Lim 0
tp_report_rate_switch                    0                     0
tp_transient_report_rate_switch          0                     0
game_mode Gear position                  0                     0
settings system peak_refresh_rate        120.0                 120.0
settings system min_refresh_rate         60.0                  60.0
settings system refresh_rate_config_custom   com.garena.game.codm,120;   (unchanged, never written)
settings system screen_brightness_mode   0                     0
settings system screen_brightness        4                     4
settings global zen_mode                 1                     1  (ZEN_MODE_IMPORTANT_INTERRUPTIONS)
```

The `diff` printed only: `scaling_cur_freq` samples of policy4 and policy7 (live values), `Lock point distance` 1 -> 0 (changes on its own, see section 5), and the FPSGO limit row `0 40` instead of `-1 -1` (section 3).

---

## 11. Proposed profile recipes

All commands verified above. Write max caps with OPP values only. Run as root.

Setup lines used by every recipe:

```sh
C=/sys/devices/system/cpu/cpufreq
G=/sys/kernel/ged/hal
```

### Hemat (coolest / most frugal)

```sh
echo 1000000 > $C/policy0/scaling_max_freq
echo 1046000 > $C/policy4/scaling_max_freq
echo 1108000 > $C/policy7/scaling_max_freq
echo 20 > $G/custom_upbound_gpu_freq
settings put system peak_refresh_rate 60.0
settings put system min_refresh_rate 60.0
echo 0 > /proc/tp_report_rate_switch
echo 0 > /proc/game_mode
```

Notes: the CPU max caps held for 60 s (verified). The GPU ceiling registers as `FPSGO` limiter at index 20 (715 MHz); verified in `limit_table` but its clock effect under load is unverified (idle). If a stronger GPU cap is wanted use a larger index, never above 40 (99 clamps to 40 = 350 MHz hard cap). Do not touch min floors or `schedhorizon` here. Do not write thermal anything.

### Seimbang (current defaults, identical to the knobs.md values)

```sh
echo 2000000 > $C/policy0/scaling_max_freq
echo 2600000 > $C/policy4/scaling_max_freq
echo 3000000 > $C/policy7/scaling_max_freq
echo 500000  > $C/policy0/scaling_min_freq
echo 437000  > $C/policy4/scaling_min_freq
echo 659000  > $C/policy7/scaling_min_freq
echo 1450000 > $C/policy0/schedhorizon/efficient_freq
echo 1740000 > $C/policy4/schedhorizon/efficient_freq
echo 1998000 > $C/policy7/schedhorizon/efficient_freq
echo 20 > $C/policy0/schedhorizon/up_delay
echo 30 > $C/policy4/schedhorizon/up_delay
echo 40 > $C/policy7/schedhorizon/up_delay
echo 0  > $G/custom_upbound_gpu_freq
echo 40 > $G/custom_boost_gpu_freq
settings put system peak_refresh_rate 120.0
settings put system min_refresh_rate 60.0
echo 0 > /proc/tp_report_rate_switch
echo 0 > /proc/tp_transient_report_rate_switch
echo 0 > /proc/game_mode
```

Write min after max when raising max back, and write max before min if the system has pushed min up; the clamp rules from section 1 apply. Write `efficient_freq` only with the non-zero values listed (never 0 on policy7).

### Gaming (full power, fast ramp-up, thermal untouched)

```sh
echo 2000000 > $C/policy0/scaling_max_freq
echo 2600000 > $C/policy4/scaling_max_freq
echo 3000000 > $C/policy7/scaling_max_freq
echo 0 > $C/policy0/schedhorizon/up_delay
echo 0 > $C/policy4/schedhorizon/up_delay
echo 0 > $C/policy7/schedhorizon/up_delay
echo 1450000 > $C/policy0/schedhorizon/efficient_freq
echo 1740000 > $C/policy4/schedhorizon/efficient_freq
echo 1998000 > $C/policy7/schedhorizon/efficient_freq
echo 0  > $G/custom_upbound_gpu_freq
echo 20 > $G/custom_boost_gpu_freq          # optional GPU floor at 715 MHz; drop this line for no floor
settings put system peak_refresh_rate 120.0
settings put system min_refresh_rate 120.0   # lock 120 Hz
echo 1 > /proc/tp_report_rate_switch         # 2 also accepted
echo 3 > /proc/game_mode                      # optional touch gear (accepted 0..5; meaning unverified)
```

Notes: `up_delay 0` with non-zero `efficient_freq` was written and held (verified), but the ramp-speed benefit itself is not measured. The `tp_report_rate_switch=1` change is accepted, the real report rate it selects is unverified. Optional CPU floors (`scaling_min_freq`) are unreliable because the system resets them; if used, re-assert them every few seconds and read back. Optional `am kill-all` before starting a session to free RAM (verified, one-shot).

### Reset to Seimbang

```sh
C=/sys/devices/system/cpu/cpufreq
G=/sys/kernel/ged/hal
echo 2000000 > $C/policy0/scaling_max_freq
echo 2600000 > $C/policy4/scaling_max_freq
echo 3000000 > $C/policy7/scaling_max_freq
echo 500000  > $C/policy0/scaling_min_freq
echo 437000  > $C/policy4/scaling_min_freq
echo 659000  > $C/policy7/scaling_min_freq
echo 1450000 > $C/policy0/schedhorizon/efficient_freq
echo 1740000 > $C/policy4/schedhorizon/efficient_freq
echo 1998000 > $C/policy7/schedhorizon/efficient_freq
echo 20 > $C/policy0/schedhorizon/up_delay
echo 30 > $C/policy4/schedhorizon/up_delay
echo 40 > $C/policy7/schedhorizon/up_delay
echo 0  > $G/custom_upbound_gpu_freq
echo 40 > $G/custom_boost_gpu_freq
settings put system peak_refresh_rate 120.0
settings put system min_refresh_rate 60.0
echo 0 > /proc/tp_report_rate_switch
echo 0 > /proc/tp_transient_report_rate_switch
echo 0 > /proc/game_mode
```

If the focus-mode DND or brightness changes were applied by the app, also restore the values the app saved before the session: `cmd notification set_dnd <priority|off|none|alarms>` matching the saved `settings get global zen_mode` (0 off, 1 priority, 2 none, 3 alarms), `settings put system screen_brightness_mode <saved>` and `settings put system screen_brightness <saved>`.

---

## 12. Remaining unverified

- Everything that needs a running game: `fpsgo_status` rows, `gpu_utilization` fields under load, `current_freqency` under load, whether FPSGO rewrites the GPU `FPSGO` limiter row during a game, real GPU clock under the ceiling/floor.
- `refresh_rate_config_custom` per-app override effect (string is in framework.jar/services.jar, so it is consumed by the ROM, but effect untested).
- What actually resets `scaling_min_freq` (writer not identified; max cap is not reset).
- Hz meaning of `tp_report_rate_switch` 1/2, and the meaning of `game_mode` gear 1-5 and "Lock point distance".
- Measured benefit of `up_delay 0` / lower `efficient_freq`.
- Permissions/SELinux context of a file freshly created under `/data/adb/gtarena/` (inferred `adb_data_file`, not tested).
