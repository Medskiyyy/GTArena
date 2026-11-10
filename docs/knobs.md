# Infinix GT 10 Pro (X6739) performance knobs: read-only survey

Device: Infinix GT 10 Pro, MediaTek Dimensity 8050 (MT6893), Android 16 custom ROM, kernel `5.10.255-v1llhaze!-komari`, KernelSU root (`su` context `u:r:ksu:s0`). Connected via wireless adb `100.109.34.105:41799`.

Survey date: 2026-11-10 ~21:12-21:20 device time. Everything here was collected with readers only (cat, ls, grep, getprop, dumpsys, settings get/list, find, cmd package/game list-style queries). Nothing was written to sysfs/procfs or settings.

Legend: **observed** = read from the device during this survey. **assumed** = inferred from the file name or from general MediaTek knowledge, not verified on this device. Write semantics were deliberately NOT tested, so every "how to write" hint is assumed.

Device state at survey time (matters for interpreting values): screen awake, foreground app `com.anthropic.claude`, **battery 6 %, Discharging**, GPU idle/powered off, display running 60 Hz. Values like current GPU frequency and `fpsgo_status` being empty reflect this idle state.

---

## 1. CPU

### 1.1 Policies (observed)

| Policy dir | related_cpus | Governor | Available freqs (kHz) | min/max now | cpuinfo min/max |
|---|---|---|---|---|---|
| `/sys/devices/system/cpu/cpufreq/policy0` | 0 1 2 3 (LITTLE) | schedhorizon | 2000000 1800000 1725000 1625000 1525000 1450000 1350000 1250000 1075000 1000000 925000 850000 750000 675000 600000 500000 | 500000 / 2000000 | 500000 / 2000000 |
| `.../policy4` | 4 5 6 (big) | schedhorizon | 2600000 2507000 2354000 2200000 1985000 1855000 1740000 1624000 1537000 1451000 1335000 1162000 1046000 902000 700000 437000 | 437000 / 2600000 | 437000 / 2600000 |
| `.../policy7` | 7 (prime) | schedhorizon | 3000000 2892000 2713000 2600000 2463000 2284000 2141000 1998000 1820000 1632000 1482000 1370000 1258000 1108000 921000 659000 | 659000 / 3000000 | 659000 / 3000000 |

All 8 CPUs online (`/sys/devices/system/cpu/online` = `0-7`).

Per-policy files (observed, identical layout in policy0/4/7):

| File | Perms | Notes |
|---|---|---|
| `scaling_governor` | `-rw-rw---- system system` | currently `schedhorizon` on all three. Owned by system, not root-world-readable. |
| `scaling_available_governors` | `-r--r--r--` | `sugov_ext reflex conservative powersave performance schedhorizon schedutil` (same for all policies) |
| `scaling_available_frequencies` | `-r--r--r--` | list above, descending |
| `scaling_min_freq` | `-rw-r--r-- root` | policy0 mtime 21:12, policy4/7 mtime 20:41 (someone wrote them at 20:41; values now equal the cpuinfo min) |
| `scaling_max_freq` | `-rw-r--r-- root` | policy4/7 mtime 20:41 (see above); values now equal cpuinfo max |
| `cpuinfo_max_freq`, `cpuinfo_min_freq` | `-r--r--r--` | hardware limits |
| `cpuinfo_cur_freq` | `-r-------- root` | root only |
| `scaling_cur_freq` | `-r--r--r--` | sample at survey: policy0 1350000, policy4 1451000, policy7 659000 |
| `scaling_setspeed` | `-rw-r--r--` | present (only meaningful with `userspace` governor, assumed) |
| `scaling_driver`, `affected_cpus`, `cpuinfo_transition_latency` | `-r--r--r--` | read-only |

### 1.2 schedhorizon tunables (observed)

Only three tunables exist under `policyN/schedhorizon/`, all `-rw-r--r-- root root`:

| Path | policy0 | policy4 | policy7 | Meaning |
|---|---|---|---|---|
| `schedhorizon/efficient_freq` | 1450000 | 1740000 | 1998000 | kHz. Assumed: frequency up to which the governor ramps "efficiently"; above it ramping is delayed by `up_delay`. |
| `schedhorizon/up_delay` | 20 | 30 | 40 | Assumed: delay (units unknown, not documented by the node) before going above `efficient_freq`. |
| `schedhorizon/rate_limit_us` | 1000 | 1000 | 1000 | Assumed: sugov-style update rate limit in microseconds. |

### 1.3 Extras: /proc/ppm and /proc/cpufreq (observed)

`/proc/ppm/` exists (MediaTek Power Policy Manager):
- `enabled` (`-rwxrwxrwx`): `ppm is enabled`
- `policy_status` (`-rw-rw-r-- root`): lists policies, usage line says `echo <idx> <1/0> > /proc/ppm/policy_status`:
  `[0] PTPOD enabled, [1] UT enabled, [2] FORCE_LIMIT enabled, [3] PWR_THRO enabled, [4] THERMAL enabled, [5] DLPT enabled, [6] HARD_USER_LIMIT enabled, [7] USER_LIMIT enabled, [8] LCM_OFF disabled, [9] SYS_BOOST disabled`
- `dump_policy_list`: "Final limit" per cluster is `(15)(0)(4)(4)`, `(15)(0)(3)(3)`, `(15)(0)(1)(1)` (i.e. no limit active). Cluster 0 = 4 CPUs, 1 = 3 CPUs, 2 = 1 CPU.
- Other files: `cobra_budget_to_limit`, `cobra_limit_to_budget`, `cpi/`, `debug`, `dump_cluster_{0,1,2}_dvfs_table` (`-r--r----- system`), `dump_power_table`, `exclusive_core` (empty), `func_debug`, `policy/`, `profile/`.

`/proc/cpufreq/`: `cpufreq_power_mode` = `Default(Normal) mode` (`-rw-rw---- system`); `cpufreq_cci_mode` = `cci_mode as Normal mode 0`; `cpufreq_sched_disable` = `cpufreq_sched_disable = 1`; plus dirs `MT_CPU_DVFS_{L,LL,B,CCI}` and `cpufreq_debug`, `cpufreq_dvfs_time_profile`, `cpufreq_stress_test`. Meanings of the mode values are assumed (power mode likely switches Normal/Low-power/Just-make/Performance, not verified).

### 1.4 cpusets (observed, `/dev/cpuset/<group>/cpus`)

| Group | cpus |
|---|---|
| top-app | 0-7 |
| foreground | 0-7 |
| background | 0-1 |
| system-background | 0-2 |
| restricted | 0-7 |

### 1.5 cpuctl / uclamp (observed, `/dev/cpuctl/<group>/`, files `-rw-r--r--`)

| Group | cpu.uclamp.min | cpu.uclamp.max | cpu.uclamp.latency_sensitive | cpu.shares |
|---|---|---|---|---|
| top-app | 0.00 | max | 1 | 1024 |
| foreground | 0.00 | max | 0 | 1024 |
| background | 0.00 | 50.00 | 0 | 1024 |
| system-background | 0.00 | 40.00 | 0 | 1024 |

Other groups present in `/dev/cpuctl`: camera-daemon, dex2oat, foreground_window, nnapi-hal, rt, system. Values are percent strings (`0.00`, `50.00`, `max`), assumed writable in the same format.

---

## 2. GPU (Mali)

### 2.1 /proc/gpufreqv2/ (observed; all `-rw-rw---- root root`, i.e. root only)

Files: `aging_mode asensor_info fix_custom_freq_volt fix_target_opp_index gpm_mode gpu_signed_opp_table gpu_working_opp_table gpufreq_status limit_table mfgsys_power_control opp_stress_test`.

`gpu_working_opp_table` (observed, full; 41 OPPs, index 0 is the fastest; freq in kHz, volt in 10 uV units as printed, e.g. 70000 = 0.70 V):

```
[00] freq: 886000, volt: 70000, vsram: 75000, posdiv: 2, vaging: 1875, power: 2608
[01] freq: 879000, volt: 70000, vsram: 75000, posdiv: 2, vaging: 1875, power: 2582
[02] freq: 873000, volt: 69375, vsram: 75000, posdiv: 2, vaging: 1875, power: 2500
[03] freq: 867000, volt: 69375, vsram: 75000, posdiv: 2, vaging: 1875, power: 2475
[04] freq: 861000, volt: 68750, vsram: 75000, posdiv: 2, vaging: 1875, power: 2420
[05] freq: 854000, volt: 68750, vsram: 75000, posdiv: 2, vaging: 1875, power: 2395
[06] freq: 848000, volt: 68125, vsram: 75000, posdiv: 2, vaging: 1875, power: 2371
[07] freq: 842000, volt: 68125, vsram: 75000, posdiv: 2, vaging: 1875, power: 2371
[08] freq: 836000, volt: 67500, vsram: 75000, posdiv: 2, vaging: 1875, power: 2294
[09] freq: 825000, volt: 67500, vsram: 75000, posdiv: 2, vaging: 1875, power: 2270
[10] freq: 815000, volt: 67500, vsram: 75000, posdiv: 2, vaging: 1875, power: 2223
[11] freq: 805000, volt: 66875, vsram: 75000, posdiv: 2, vaging: 1875, power: 2149
[12] freq: 795000, volt: 66875, vsram: 75000, posdiv: 2, vaging: 1875, power: 2126
[13] freq: 785000, volt: 66250, vsram: 75000, posdiv: 2, vaging: 1875, power: 2054
[14] freq: 775000, volt: 65625, vsram: 75000, posdiv: 2, vaging: 1875, power: 2031
[15] freq: 765000, volt: 65625, vsram: 75000, posdiv: 2, vaging: 1875, power: 2009
[16] freq: 755000, volt: 65625, vsram: 75000, posdiv: 2, vaging: 1875, power: 1986
[17] freq: 745000, volt: 65000, vsram: 75000, posdiv: 2, vaging: 1875, power: 1918
[18] freq: 735000, volt: 65000, vsram: 75000, posdiv: 2, vaging: 1875, power: 1874
[19] freq: 725000, volt: 64375, vsram: 75000, posdiv: 2, vaging: 1875, power: 1808
[20] freq: 715000, volt: 63750, vsram: 75000, posdiv: 2, vaging: 1875, power: 1744
[21] freq: 705000, volt: 63750, vsram: 75000, posdiv: 2, vaging: 1250, power: 1723
[22] freq: 695000, volt: 63125, vsram: 75000, posdiv: 2, vaging: 1250, power: 1661
[23] freq: 685000, volt: 62500, vsram: 75000, posdiv: 2, vaging: 1250, power: 1641
[24] freq: 675000, volt: 61875, vsram: 75000, posdiv: 2, vaging: 1250, power: 1581
[25] freq: 654000, volt: 61875, vsram: 75000, posdiv: 2, vaging: 1250, power: 1521
[26] freq: 634000, volt: 61250, vsram: 75000, posdiv: 2, vaging: 1250, power: 1445
[27] freq: 614000, volt: 61250, vsram: 75000, posdiv: 2, vaging: 1250, power: 1406
[28] freq: 593000, volt: 60000, vsram: 75000, posdiv: 2, vaging: 1250, power: 1315
[29] freq: 573000, volt: 60000, vsram: 75000, posdiv: 2, vaging: 1250, power: 1277
[30] freq: 553000, volt: 59375, vsram: 75000, posdiv: 2, vaging: 1250, power: 1209
[31] freq: 532000, volt: 59375, vsram: 75000, posdiv: 2, vaging: 1250, power: 1172
[32] freq: 512000, volt: 58750, vsram: 75000, posdiv: 2, vaging: 625, power: 1089
[33] freq: 492000, volt: 58750, vsram: 75000, posdiv: 2, vaging: 625, power: 1053
[34] freq: 471000, volt: 57500, vsram: 75000, posdiv: 2, vaging: 625, power: 966
[35] freq: 451000, volt: 57500, vsram: 75000, posdiv: 2, vaging: 625, power: 915
[36] freq: 431000, volt: 56875, vsram: 75000, posdiv: 2, vaging: 625, power: 882
[37] freq: 410000, volt: 56875, vsram: 75000, posdiv: 2, vaging: 625, power: 848
[38] freq: 390000, volt: 56250, vsram: 75000, posdiv: 2, vaging: 625, power: 793
[39] freq: 370000, volt: 55625, vsram: 75000, posdiv: 3, vaging: 625, power: 725
[40] freq: 350000, volt: 55000, vsram: 75000, posdiv: 3, vaging: 625, power: 675
```

`gpufreq_status` (observed, GPU idle):

```
[GPUFREQ-DEBUG] Current Status of GPUFREQ
[GPU-OPP]       Index: 0, Freq: 886000, Volt: 70000, Vsram: 75000
[GPU-REALOPP]   FmeterFreq: 886028, Con1Freq: 886000, ReguVolt: 70000, ReguVsram: 75000
[GPU-Power]     PowerCount: 0, CG: 0, MTCMOS: 0, BUCK: 0
[GPU-Segment]   SegmentID: 2, WorkingOPPNum: 41, SignedOPPNum: 41
[GPU-LimitC]    CeilingIndex: 0, Limiter: 0, Priority: 9
[GPU-LimitF]    FloorIndex: 40, Limiter: 0, Priority: 9
[Common-Status] Dual Buck: False, GPUEB Support: Off
[Common-Status] DVFSState: 0x0006, ShaderPresent: 0x00660067
[Common-Status] Aging: Disable, AVS: Disable, StressTest: Disable, GPM1.0: Disable
[Common-Status] GPU_SB_Ver: 0x0000, GPU_PTP_Ver: 0x0000, Temp_Compensate: Disable
```

`limit_table` (observed; ceiling/floor `-1` = unset, otherwise an OPP index; lower index = higher freq):

```
[id]          [name]  [priority]  [ceiling]  [floor]  [c_enable]  [f_enable]
   0         SEGMENT           9          0       40           1           1
   1           DEBUG           8         -1       -1           1           1
   2      THERMAL_AP           7         -1       -1           1           1
   3      THERMAL_EB           7         -1       -1           1           1
   4          SRAMRC           6         -1       -1           1           1
   5         BATT_OC           5         -1       -1           1           1
   6    BATT_PERCENT           5         -1       -1           1           1
   7        LOW_BATT           5         -1       -1           1           1
   8             PBM           5         -1       -1           1           1
   9        APIBOOST           4         -1       40           1           1
  10           FPSGO           4         -1       -1           1           1
```

Other nodes:
- `fix_target_opp_index` reads `[GPUFREQ-DEBUG] fix OPP index: 0` (observed; the text does not say whether this means "disabled" or "fixed to 0"; `fix_custom_freq_volt` says "disabled" and `gpufreq_status` shows normal DVFS, so it is most likely a disabled/idle readout; unverified).
- `fix_custom_freq_volt` reads `[GPUFREQ-DEBUG] fixed freq and volt are disabled` (observed).
- Write format for both is NOT verified. Assumed (MediaTek gpufreq debug interface): `echo <opp_index> > fix_target_opp_index`, `echo <freq_khz> <volt> > fix_custom_freq_volt`, with 0/-1/0xFFFF style values to release. Test on-device before relying on it. The existence of `DEBUG` (priority 8) in the limit table suggests these fixers go through that limiter slot (assumed).
- `gpu_signed_opp_table`, `mfgsys_power_control`, `opp_stress_test`, `aging_mode`, `gpm_mode`, `asensor_info`: exist, not read (power/stress control, treated as risky).

### 2.2 /sys/kernel/ged/hal/ (observed)

Perms: `-rw-rw---- root root` for writable ones, `-r--r----- root root` for read-only ones. Root only.

| File | Perms | Current value | Meaning |
|---|---|---|---|
| `current_freqency` (sic) | r | `0 886000` | assumed: `<opp_index> <freq_kHz>`. Shows last programmed OPP; GPU was idle so it equals OPP 0. |
| `previous_freqency` | r | `0 886000` | assumed: previous OPP, same format |
| `custom_boost_gpu_freq` | rw | `40` | assumed: OPP index floor/boost (40 = lowest OPP = no boost). Write format assumed: OPP index. |
| `custom_upbound_gpu_freq` | rw | `0` | assumed: OPP index ceiling (0 = highest OPP = no cap). Write format assumed: OPP index. Primary candidate for a GPU frequency cap. |
| `gpu_boost_level` | rw | `0` | assumed: boost level selector. mtime 20:41, so something wrote it earlier today. |
| `gpu_utilization` | r | `0 0 100` | assumed: `<active%> <?%> <idle%>`; sample shows idle. Three fields; exact meaning unverified. |
| `total_gpu_freq_level_count` | r | `41` | matches 41 working OPPs |
| `dvfs_margin_value` | rw | `10` | assumed: DVFS headroom margin (percent). Higher = ramps earlier. |
| `timer_base_dvfs_margin` | rw | `30` | assumed: margin for timer-based DVFS policy |
| `dvfs_loading_mode` | rw | `0` | assumed: loading-based DVFS mode selector |
| `dvfs_workload_mode` | rw | `0` | assumed |
| `loading_base_dvfs_step` | rw | `0` | assumed |
| `loading_stride_size` | rw | `2` | assumed |
| `loading_window_size` | rw | `8` | assumed |
| `fallback_interval` | rw | `210` | assumed: ms |
| `fallback_timing` | rw | `0` | assumed |
| `fastdvfs_mode` | rw | `fdvfs is off` / `FastDVFS is disabled` | text status, write format not shown |
| `dcs_mode` | rw | `DCS Policy is disabled` + help text: "Enable DCS with user-defined mode with min Available Freq Code ... echo [the min Code you want] for enable (Echo 0 for disable)" | observed help text; DCS = dynamic core scaling (assumed) |
| `fw_idle` | rw | `0` | assumed |
| `ged_kpi` | r | `0,0,0,0,0,0,0` | assumed: GED KPI counters (fps related), all zero while idle |

### 2.3 Mali devfreq `/sys/class/devfreq/13000000.mali` (observed)

| File | Perms | Value |
|---|---|---|
| `governor` | `-rw-r--r--` | `dummy` |
| `available_governors` | `-r--r--r--` | `apupassive-pe apuconstrain apuuser apupassive dummy userspace powersave performance simple_ondemand` |
| `available_frequencies` | `-r--r--r--` | `886000000 879000000 873000000 ... 350000000` (Hz, same 41 steps as the OPP table) |
| `min_freq` | `-rw-r--r--` | `350000000` |
| `max_freq` | `-r--r--r--` | `886000000` (read-only here) |
| `cur_freq` | `-r--r--r--` | `350000000` (at idle) |
| other | | `polling_interval timer trans_stat target_freq` |

The governor is `dummy`: the devfreq node is only a reporting shim, real DVFS is driven by GED/gpufreqv2 (inferred from `dummy` governor + read-only `max_freq`). Do not expect devfreq writes to cap the GPU.

Other devfreq nodes on the device (observed names): `mtk-dvfsrc-devfreq`, `soc:APUCORE`, `soc:APUCORE:APUCONN`, `soc:APUCORE:APUCONN:APUIOMMU`, `soc:apusys_power:APUMDLA*`, `soc:apusys_power:APUVPU@0..2`.

### 2.4 Recommended GPU capping / boosting nodes (assessment, assumed)

- Cap: `/sys/kernel/ged/hal/custom_upbound_gpu_freq` (OPP index, 0 = uncapped) is the likeliest clean ceiling. Alternative: `/proc/gpufreqv2/fix_target_opp_index` pins to one OPP (blunt).
- Boost/floor: `/sys/kernel/ged/hal/custom_boost_gpu_freq` (OPP index, 40 = no floor).
- Verify the actual effect by reading `/proc/gpufreqv2/limit_table` (a new ceiling should appear in the DEBUG/APIBOOST row) and `gpufreq_status` while a game renders. Not tested.

---

## 3. FPSGO (`/sys/kernel/fpsgo/`, observed)

Directories: `common composer fbt fstb minitop xgf`. Most files `-rw-rw---- root root`; read-only ones `-r--r-----`. Exceptions world-accessible: `common/fpsgo_enable` (`-rw-rw-r--`) and `fstb/fpsgo_status` (`-r--r--r--`).

### 3.1 common

| File | Perms | Value | Notes |
|---|---|---|---|
| `fpsgo_enable` | rw-rw-r-- | `1` | master enable (assumed from name) |
| `force_onoff` | rw | `free` | assumed: force FPSGO on/off/free |
| `perfserv_ta` | rw | `0` | |
| `stop_boost` | rw | `0` | assumed: stop all boosting when 1 |
| `systrace_mask` | rw | text list: MANDATORY On, FBT/FSTB/XGF/GBE/FBT_CTRL/FRE/FRE_THM Off | trace switches |
| `render_info` | r | header only: `PID NAME TGID TYPE API BufferID FRAME_L ENQ_L ENQ_S ENQ_E DEQ_L DEQ_S DEQ_E (HWUI,UX)` | per-render-thread table; rows appear only when a tracked app renders (assumed) |
| `BQid`, `render_loading` | r | empty | |

### 3.2 composer
`composer/connect_api_info` (r): `=================================` (empty while idle).

### 3.3 fbt (frame boost, values observed)

| File | Value | Notes |
|---|---|---|
| `blc_boost` | 0 | |
| `boost_ta` | 0 | mtime 20:41 (written earlier today) |
| `enable_ceiling` | 0 | |
| `enable_switch_cap_margin` | `fbt_cap_margin_enable 1` / `set_cap_margin 0` | |
| `enable_switch_down_throttle` | `fbt_down_throttle_enable 1` / `down_throttle_ns -1` | |
| `enable_switch_sync_flag` | `fbt_sync_flag_enable 1` / `sync_flag -1` | |
| `enable_uclamp_boost` (r) | `enable uclamp boost` | |
| `light_loading_policy` | `light loading policy:0` ("0 means bypass, other value(1-100) means percent.") | |
| `limit_cfreq` | `2600000` | assumed: CPU freq limit (kHz) for big cluster, default = max |
| `limit_cfreq_m`, `limit_rfreq`, `limit_rfreq_m` | 0 | assumed: variants (rescue / "m" = for specific mode) |
| `llf_task_policy` | 0 | |
| `switch_idleprefer` | `fbt_idleprefer_enable:1` | |
| `thrm_enable` | 1 | FPSGO thermal throttling enabled (assumed) |
| `thrm_temp_th` | 65 | assumed: temperature threshold in degC |
| `thrm_iso_pcbtemp_th` | 200 | |
| `thrm_limit_cpu`, `thrm_sub_cpu` | -1 | |
| `ultra_rescue` | 0 | |
| `fbt_info` (r) | cluster table `clus max min: 3 2 0`; per-cluster c/r `-1 -1 / -1 -1 / 19 -1`; `enable 1 idleprefer 0 ... dfps ... vsync 60` | state dump |
| `table_cap`, `table_freq` (r) | per-cluster/OPP tables (`0/ 0 22` cap, `0/ 0 2000` freq in MHz) | |
| `thrm_info` (r) | cluster mapping `0:4 cpus first 0, 1:3 cpus first 4, 2:1 cpu first 7` | |

### 3.4 fstb (FPS target control; most relevant)

| File | Perms | Value | Notes |
|---|---|---|---|
| `fpsgo_status` | `r--r--r--` (world-readable) | header only while idle (see 4.1) | **the live FPS table** |
| `fstb_fps_list` | rw | empty | assumed: list of target FPS values, e.g. user-supplied fps list |
| `set_render_max_fps` | rw | empty | assumed: per-render max FPS target (write format unknown, likely `pid fps` or `name fps`) |
| `fstb_self_ctrl_fps_enable` | rw | `1` | self-controlled FPS target enabled |
| `fstb_fps_bypass_min` / `fstb_fps_bypass_max` | rw | 50 / 150 | assumed: FPS range outside which FSTB bypasses control |
| `fstb_soft_level` | rw | `1 240-10 ` | |
| `adopt_low_fps` | rw | 1 | |
| `fstb_debug` | rw | `fstb_enable 1, fstb_log 0, fstb_active 0, fstb_active_dbncd 0, fstb_idle_cnt 1` | |
| `fstb_tune_window_size` | rw | 1000000 | assumed ns |
| `fstb_tune_quantile` / `fstb_tune_error_threshold` | rw | 50 / 10 | |
| `jump_check_num` / `jump_check_q_pct` | rw | 21 / 33 | |
| `margin_mode`, `margin_mode_gpu` | rw | 2 / 2 | (+ `_dbnc_a` = 9, `_dbnc_b` = 1) |
| `set_cam_active`, `set_cam_active_fpsgo_off` | rw | 0 / 0 | |
| `set_ui_ctrl`, `set_video_pid`, `clear_video_pid`, `fstb_video_pid_list` | rw / r | empty / `tgid count_instance` header | |
| `tfps_to_powerhal_enable` | rw | 0 | |

### 3.5 minitop
`enable` (`Mini TOP is ON`), `minitop_n` (`Top 3 is tracked`), `thrs_heavy` (`Threshold of heavy is 70`), `cooldn_order` (`Debounce slot is 2^2=4`), `warmup_order` (`Slot size is 2^3=8 tick(s)`), `minitop_trace_enable` (0), `list` (r, empty).

### 3.6 xgf
`xgf_status`, `deplist`, `runtime` (r, empty while idle), `xgf_spid_list` (rw, header only), `xgf_trace_enable` / `xgf_log_trace_enable` (0).

### 3.7 Candidate switches (assumed)
- Enable switch: `common/fpsgo_enable` (currently 1) and `common/force_onoff` (`free`).
- FPS target controls: `fstb/set_render_max_fps`, `fstb/fstb_fps_list`, `fstb/fstb_self_ctrl_fps_enable`, `fstb/fstb_fps_bypass_{min,max}`.
- Boost/limit: `fbt/limit_cfreq*`, `fbt/limit_rfreq*`, `fbt/blc_boost`, `fbt/boost_ta`, `fbt/ultra_rescue`, `common/stop_boost`.
- Thermal inside FPSGO: `fbt/thrm_enable`, `fbt/thrm_temp_th` (65).
The writable semantics were not tested.

---

## 4. Reading current FPS without touching the foreground app

### 4.1 /sys/kernel/fpsgo/fstb/fpsgo_status (observed)
World-readable (`-r--r--r--`), so it can be read without root. Content while no game renders:

```
tid	bufID		name		currentFPS	targetFPS	FPS_margin	FPS_margin_GPU	FPS_margin_thrs	sbe_state	HWUI	t_gpu	VIDEO
fstb_self_ctrl_fps_enable:1
fstb_is_cam_active:0
dfps_ceiling:60
```

Format: tab-separated header, then one row per tracked render thread with `currentFPS` and `targetFPS` columns (rows not observed, because nothing was being rendered; row layout is assumed to follow the header). `dfps_ceiling` mirrors the active display refresh rate (it said 60 with the display at 60 Hz). FPSGO tracks only apps it decides are rendering through BufferQueue; plain UI apps may not appear (assumed). To be confirmed with a real game running.

### 4.2 dumpsys SurfaceFlinger --timestats -dump (observed)
Returns nothing (rc 0, empty) because timestats is not enabled, and enabling it (`--timestats -enable`) changes SurfaceFlinger state, so it was not attempted. Not usable read-only on this device in its current state.

### 4.3 dumpsys SurfaceFlinger --latency (observed)
`dumpsys SurfaceFlinger --latency` prints only the refresh period (`16666666` ns at 60 Hz). With a layer-name argument (`'com.anthropic.claude/com.anthropic.claude.mainactivity.MainActivity#2826'`) it printed only that period line, no frame timestamps (layer name probably must match the exact name in `--list`; not resolved). `dumpsys SurfaceFlinger --list` lists layers (verbose `RequestedLayerState{...}` lines on Android 16).

### 4.4 dumpsys SurfaceFlinger (full) FPS ring buffer (observed)
The full dump contains a section `FPS ring buffer:` with lines like `(0) 21:12:58.176 fps=2.12 dur=1412.54 max=1029.20 min=14.97`, plus `renderRate=60.00 Hz` and `activeMode={id=2, hwcId=2, ... vsyncRate=60.00 Hz ...}`. The ring buffer's meaning is unverified (low fps values at idle suggest it is per-layer or per-window of the latest content update; assumed). Useful, grep-able display refresh readout: `activeMode`.

### 4.5 dumpsys gfxinfo (observed)
`dumpsys gfxinfo` (all processes) and `dumpsys gfxinfo <pkg>` print per-process: `Total frames rendered`, `Janky frames`, percentiles (50th/90th/95th/99th ms), `Number Missed Vsync`, etc. (observed for `com.anthropic.claude` and `com.android.vending`). `dumpsys gfxinfo <pkg> framestats` was accepted and gave the same header (full frame-timestamp matrix not captured). This only covers HWUI-rendered UI, not OpenGL/Vulkan game engines (assumed, standard Android behaviour), so for games FPSGO's `fpsgo_status` is the better source.

---

## 5. Display

### 5.1 Modes (observed)
`dumpsys display`: panel 1080x2400, three modes. Framework ids are 1-based and SurfaceFlinger ids are 0-based:

| dumpsys display modeId | SF id | Refresh |
|---|---|---|
| 1 | 0 | 120.00001 Hz |
| 2 | 1 | 90.0 Hz |
| 3 | 2 | 60.0 Hz |

At survey: `mActiveModeId=3` (60 Hz), `mDefaultModeId=2`, `userPreferredModeId -1`, `supportedRefreshRates [120.00001, 90.0, 60.0]`, `frameRateCategoryRate {normal=60.0, high=90.0}`, `hasArrSupport false`. The panel sits at 60 Hz although peak=120, because the foreground content is idle UI (assumed).

### 5.2 Settings (observed)

| Namespace | Key | Value |
|---|---|---|
| system | `peak_refresh_rate` | `120.0` |
| system | `min_refresh_rate` | `60.0` |
| system | `user_refresh_rate` | `null` (not present) |
| system | `extreme_refresh_rate` | `0` |
| system | `refresh_rate_config_custom` | `com.garena.game.codm,120;` (per-app refresh override; format `pkg,hz;` repeated, assumed from the one sample) |
| secure | (grep refresh/rate) | only `sysui_qs_tiles` containing a `refresh_rate` tile |
| global | (grep refresh/rate/game) | only `ingress_rate_limit_bytes_per_second=-1` and `job_scheduler_quota_controller_constants` (unrelated) |

No `game`-named global setting exists. The ROM-specific per-app setting is `system refresh_rate_config_custom` (this looks like the ROM's game-space refresh-rate list; write effect not tested).

---

## 6. Charging

### 6.1 /proc/mtk_battery_cmd/ (observed; all `-rw-r--r-- root root`)

| File | Value | Meaning |
|---|---|---|
| `en_power_path` | `0` | **Candidate bypass/power-path node.** Assumed: MediaTek "enable power path" (1 = power path on i.e. normal charging path; 0 = ...). The exact polarity and whether 0 currently means "disabled" or "normal" is NOT verified. The phone is on battery with no charger, so the value is not informative about charging state. |
| `en_safety_timer` | `1` | assumed: charging safety timer enabled |
| `current_cmd` | `0 0` | assumed: `<current_limit> <?>` command pair; writing sets a charge current command (format assumed) |
| `set_cv` | `4450000` | assumed: constant-voltage setpoint in uV (4.45 V) |

Note `battery/constant_charge_voltage` shows `4350` (mV) and `charger/constant_charge_voltage` `4350`, whereas `set_cv` shows 4450000. These disagree, so `set_cv` may be a pending command value rather than the active CV (assumed; unverified).

### 6.2 power_supply list (observed, `/sys/class/power_supply/`)
`battery charger mtk-gauge mtk-master-charger mtk-mst-div-chg mtk-slave-charger mtk-slv-div-chg mtk_charger_type tran-auto-test usb_switch`. There is no `usb`, `ac`, `wireless`, `main-charger` entry.

### 6.3 battery (observed)
Writable by root (`-rw-r--r--`): `disable` (0), `init_done` (1), `log_level` (3), `temperature` (34). Write-only (`--w-------`): `coulomb_int_gap en_uisoc_ht_int en_uisoc_lt_int reset temp_th uisoc uisoc_ht_int_gap uisoc_lt_int_gap` (not read, Permission denied on cat).

| Node | Value | Notes |
|---|---|---|
| `status` | `Discharging` | |
| `capacity` | `6` | percent. Battery was low at survey. |
| `capacity_level` | `Low` | |
| `current_now` | `-560200` (second sample `-378400`) | uA, negative = discharging |
| `current_avg` | `-427800` | uA |
| `voltage_now` | `3629000` | uV |
| `temp` | `330` | tenths of degC = 33.0 |
| `temperature` | `34` | degC integer (rw) |
| `health` | `Good` | |
| `charge_full` / `charge_full_design` / `charge_counter` | 5000000 / 500000 / 300000 | `charge_full_design` is clearly scaled wrongly by the driver (observed) |
| `constant_charge_voltage` | `4350` | |
| `cycle_count` | 1 | |
| `time_to_full_now` | 44293 | |
| `disable` | `0` | assumed: disables battery gauge/charging path from the gauge side; semantics unverified, treat as risky |

### 6.4 charger (`/sys/class/power_supply/charger`, device `mt6360_chg.2.auto`, observed)
Writable (`-rw-r--r--`): `charge_term_current` 150, `constant_charge_current` 2000 (mA, assumed), `constant_charge_voltage` 4350, `input_current_limit` 500 (mA, assumed), `input_voltage_limit` 4400, `online` 0 (rw), `status` `Not charging` (rw), `energy_empty`. Read-only: `current_max` 500000, `usb_type` `[Unknown] SDP DCP CDP C PD PD_DRP BrickID`, `type` USB, `calibrate`, `manufacturer`, `voltage_max` (the last four returned "No data available").

### 6.5 mtk-master-charger (observed)
Writable: `constant_charge_current_max` 2000000 (uA), `input_current_limit` 500000 (uA), `voltage_max` 1 (odd value, as printed). Read-only: `online` 0, `present` 1, `temp` 0, `type` Unknown, `voltage_now` 0.

### 6.6 Others (observed)
`usb_switch`: `online=0`, `type=Mains` (only online/type, read-only). `mtk_charger_type`: `TYPE=Unknown`, `VOLTAGE_NOW=0`. `mtk-gauge` is a symlink to `10026000.pwrap/.../mt6359p-gauge`.

Charge-limit / bypass candidates (assumed): `/proc/mtk_battery_cmd/en_power_path` (power path), `charger/input_current_limit` and `mtk-master-charger/input_current_limit` (input limit), `charger/constant_charge_current`, `mtk-master-charger/constant_charge_current_max`, `mtk_battery_cmd/current_cmd`. Current values are all "no charger attached" values, so the true charging-state behavior of any of them must be tested while plugged in. Nothing was written.

---

## 7. Thermal

### 7.1 Zones (observed, `/sys/class/thermal/thermal_zone*/`; temp in millidegC; all `mode=enabled`)

| Zone | type | temp | Sane? |
|---|---|---|---|
| thermal_zone0 | mtk-master-charger | 0 | no (no charger) |
| thermal_zone1 | mtk-slave-charger | 0 | no |
| thermal_zone2 | mtk-mst-div-chg | 0 | no |
| thermal_zone3 | mtk-slv-div-chg | 0 | no |
| thermal_zone4 | battery | 34000 | yes |
| thermal_zone5 | mtktscpu | 42806 | yes (CPU) |
| thermal_zone6 | mtktspmic | 36303 | yes |
| thermal_zone7 | mtktsbtsnrpa | 36381 | yes (NR PA) |
| thermal_zone8 | mtktsbattery | 34000 | yes |
| thermal_zone9 | mtktsAP | 37551 | yes (AP/board) |
| thermal_zone10 | mtktsbtsmdpa | 35924 | yes (modem PA) |
| thermal_zone11 | mtktspa | 34000 | yes |
| thermal_zone12 | mtktscharger | 0 | no |
| thermal_zone13..25 | tzimgs0..tzimgs12 | -127000 | no (camera/imgsys, invalid when unused) |
| thermal_zone26 | mtktsdctm | 33842 | yes |
| thermal_zone27 | mtktswmt | 34000 | yes |

Best sources: CPU temp = `thermal_zone5` (`mtktscpu`), battery temp = `thermal_zone4` (`battery`) or `thermal_zone8` (`mtktsbattery`) (both 34000), cross-check with `power_supply/battery/temp` (330 = 33.0 degC). Zone indexes should be resolved by reading `type`, not hard-coded (assumed; index order could change across boots).

Cooling devices: 136 entries (`cooling_device0..135`), all `cur_state=0` at survey, e.g. `mtk-cl-shutdown*`, `cpu_adaptive_0..2`, `cpu00..cpu12`, `mtk-cl-backlight01`, `thermal-devfreq-0` (max_state 40, which matches GPU OPP count), `clnothing*`. Reading `cur_state` is a harmless way to see if thermal throttling is active (assumed).

### 7.2 Thermal processes and services (observed; DO NOT touch)

Processes (`ps -A | grep -i therm`):
```
root    639   2  [fre_thmwork:0]                     (kernel thread)
system 1697   1  thermal
system 1701   1  thermalloadalgod
root   2749   1  thermal_core
system 2750   1  android.hardware.thermal-service.mediatek
```
init services (`getprop | grep thermal`): `init.svc.thermal=running` (pid 1697), `init.svc.thermal_core=running` (2749), `init.svc.thermalloadalgod=running` (1701), `init.svc.vendor.thermal-hal=running` (2750), `init.svc.thermal_manager=stopped`, `init.svc.vendor.thermal.symlinks=stopped`, `vendor.thermal.link_ready=1`. An app can check liveness via these props (`stopped` for thermal_manager and thermal.symlinks is the normal state here, they are one-shot, assumed).

---

## 8. Touch

Found (observed via `find /sys /proc -maxdepth 6`):

| Path | Perms | Value | Meaning |
|---|---|---|---|
| `/proc/game_mode` | `-rw-rw-rw-` | `Gear position:0` / `Lock point distance:1` | touch driver game mode. Format of writes not tested. Assumed gear = touch sensitivity/game gear. |
| `/proc/tp_report_rate_switch` | `-rw-rw-rw-` | `0` | assumed: touch report rate switch (0 = default, 1 = high, e.g. 240 Hz). Write semantics unverified. |
| `/proc/tp_transient_report_rate_switch` | `-rw-rw-rw-` | `0` | assumed: temporary version of the above |
| `/proc/custom_mistouch` | `-rw-rw-rw-` | 5 regions all `(0x00 0x00) (0x00 0x00)` | assumed: edge/mistouch rejection regions |
| `/sys/devices/platform/touch/` | | `gesture_test` (rw), `tp_firmware_version`, `tp_final_firmware_version`, `tp_vendor`, `driver -> tran-tpd` | touch controller `cap_touch@38` on i2c@11015000; input device `mtk-tpd` (event4) |

These four `/proc` nodes are world-writable (`rw-rw-rw-`), so they need no root. fpsgo has no touch nodes. A Transsion driver (`tran-tpd`) is in use.

---

## 9. Memory (observed)

`/proc/meminfo` head: `MemTotal 7648524 kB`, `MemFree 212424 kB`, `MemAvailable 2357168 kB`, `Buffers 1432 kB`, `Cached 2008540 kB`, `SwapCached 188848 kB`, `Active 1913400 kB`, `Inactive 2113752 kB`.

zram0: `disksize` `4290379776` (`-rw-r--r--`), `comp_algorithm` `lzo lzo-rle [lz4] zstd` (active = lz4), `mm_stat` `1746612224 541555296 567173120 0 951128064 45551 122405 25998` (orig, compressed, mem_used, limit, max_used, same_pages, compacted, huge_pages). `/proc/swaps`: `/dev/block/zram0 partition 4189820 KiB, used 1706904, priority -2`. `/proc/sys/vm/swappiness` = `100`, `vfs_cache_pressure` = `100`.

---

## 10. KernelSU module `x6739_tuning`

Files in `/data/adb/modules/x6739_tuning/`: `module.prop` (182 B), `post-fs-data.sh` (882 B, `-rwxr-xr-x`), `service.sh` (2062 B, `-rwxr-xr-x`), `system.prop` (43 B). Log file used: `/data/local/tmp/hwfix.log`.

### service.sh (verbatim)

```sh
#!/system/bin/sh
L=/data/local/tmp/hwfix.log
P=/sys/devices/system/cpu/cpufreq

while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done

(
    for p in 0 4 7; do echo schedhorizon > $P/policy$p/scaling_governor; done
    while true; do
        if [ -e $P/policy7/schedhorizon/up_delay ] && [ "$(cat $P/policy7/schedhorizon/efficient_freq)" = "0" ]; then
            echo 1450000 > $P/policy0/schedhorizon/efficient_freq; echo 20 > $P/policy0/schedhorizon/up_delay
            echo 1740000 > $P/policy4/schedhorizon/efficient_freq; echo 30 > $P/policy4/schedhorizon/up_delay
            echo 1998000 > $P/policy7/schedhorizon/efficient_freq; echo 40 > $P/policy7/schedhorizon/up_delay
            echo "cpu-tune applied uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L
        fi
        sleep 30
    done
) </dev/null >/dev/null 2>&1 &

sleep 3
echo "service uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L

i=0
while [ $i -lt 15 ]; do
    CAM_COUNT=$(dumpsys media.camera 2>/dev/null | grep -m1 "Number of camera devices:" | awk '{print $NF}')
    [ -n "$CAM_COUNT" ] && [ "$CAM_COUNT" != "0" ] && break
    sleep 2
    i=$((i + 1))
done
echo "camera count=$CAM_COUNT tries=$i" >> $L
if [ "$CAM_COUNT" = "0" ] || [ -z "$CAM_COUNT" ]; then
    echo "camera-missing -> restart" >> $L
    pkill -f camerahalserver
    pkill -f cameraserver
fi

i=0
while [ $i -lt 10 ]; do
    SENSOR_PROBE=$(dumpsys sensorservice 2>/dev/null | grep -m1 "icm42631_acc")
    [ -n "$SENSOR_PROBE" ] && break
    sleep 2
    i=$((i + 1))
done
echo "sensor real-names=$([ -n "$SENSOR_PROBE" ] && echo 1 || echo 0) tries=$i early=$(getprop sys.x6739.early_sensor_fix)" >> $L
if [ -z "$SENSOR_PROBE" ]; then
    echo "sensor generic -> fallback restart HAL" >> $L
    pkill -f android.hardware.sensors-service.multihal
fi

mount -o bind /data/adb/modules/gps_optimizer/system/etc/gps.conf /system/etc/gps_debug.conf 2>/dev/null
cmd location providers send-extra-command gps force_time_injection 2>/dev/null
cmd location providers send-extra-command gps force_psds_injection 2>/dev/null
```

### post-fs-data.sh (verbatim)

```sh
#!/system/bin/sh
L=/data/local/tmp/hwfix.log
up() { cut -d' ' -f1 /proc/uptime; }
echo "boot $(date '+%F %T') post-fs-data uptime=$(up) scp=$(cat /sys/devices/virtual/misc/scp/scp_A_status 2>/dev/null)" >> $L
(
    i=0
    while [ "$(cat /sys/devices/virtual/misc/scp/scp_A_status 2>/dev/null)" != "SCP A is ready" ] && [ $i -lt 200 ]; do sleep 0.1; i=$((i + 1)); done
    echo "early scp-ready uptime=$(up)" >> $L
    i=0
    while [ "$(getprop init.svc.vendor.sensors-hal-multihal)" != "running" ] && [ $i -lt 300 ]; do sleep 0.1; i=$((i + 1)); done
    sleep 1
    if [ -z "$(pidof system_server)" ]; then
        setprop ctl.restart vendor.sensors-hal-multihal
        echo "early sensor-hal restart uptime=$(up)" >> $L
        setprop sys.x6739.early_sensor_fix 1
    else
        echo "early too-late system_server-up uptime=$(up)" >> $L
    fi
) </dev/null >/dev/null 2>&1 &
```

### system.prop (verbatim)

```
vendor.debug.camera.3rdalgo.slimbodymode=0
```

### What the watcher loop writes and how often (derived from the code above)

- Once at boot (after `sys.boot_completed=1`): writes `schedhorizon` to `policy0/4/7 scaling_governor`.
- Then every **30 s** (`sleep 30`) the loop does a *read* of `policy7/schedhorizon/efficient_freq`. It only **writes** when that value equals `0` (i.e. after something reset the tunables, e.g. the governor being re-created). In that case it writes:
  - `policy0/schedhorizon/efficient_freq` = `1450000`, `policy0/schedhorizon/up_delay` = `20`
  - `policy4/schedhorizon/efficient_freq` = `1740000`, `policy4/schedhorizon/up_delay` = `30`
  - `policy7/schedhorizon/efficient_freq` = `1998000`, `policy7/schedhorizon/up_delay` = `40`
  and appends `cpu-tune applied uptime=...` to `/data/local/tmp/hwfix.log`.
- It does NOT write `scaling_governor` again after boot, does NOT touch `scaling_min_freq`/`scaling_max_freq`, `rate_limit_us`, GPU or FPSGO nodes. At survey time all six values match the targets, so the loop is idle (the read sees non-zero). Implication for the app: a governor change by the app would NOT be reverted unless the app triggers a governor re-creation that zeroes `efficient_freq`; if it does, within 30 s the loop restores the six values above (and not the governor).
- Remainder of service.sh is one-shot: camera-count check (kills `camerahalserver`/`cameraserver` if 0 cameras after 15 tries), sensor probe (kills sensors multihal HAL if `icm42631_acc` is missing), and GPS config bind-mount + force time/PSDS injection. Last lines of `/data/local/tmp/hwfix.log`: `service uptime=38.65`, `camera count=0 tries=15`, `camera-missing -> restart`, `sensor real-names=1 tries=0 early=1`.

### All modules in /data/adb/modules (observed)

| Module dir | name (module.prop) | service.sh | post-fs-data.sh | system.prop | disable |
|---|---|---|---|---|---|
| DolbyAtmos | Dolby Atmos Moto G52 | yes | yes | yes | no |
| MotoCore | Moto Core | yes | yes | yes | no |
| RAW | RAW | no | no | no | no |
| gms_role_fix | GMS Role Holders Permission Fix | yes | no | yes | no |
| gps_optimizer | GPS and GNSS Optimizer | no | no | no | no |
| magic_mount_rs | Magic Mount-rs | no | no | no | no |
| playintegrityfix | Play Integrity Fork Modded | yes | yes | no | no |
| trancamdata | TranCam Addons | no | no | no | no |
| uffd_fix | Disable UFFD GC for Banking | yes | yes | yes | no |
| x6739_dts | X6739 DTS Audio | no | yes | yes | no |
| x6739_tuning | X6739 Tuning | yes | yes | yes | no |
| xos16trancamera | TranCam XOS16 | no | no | yes | no |
| zygisksu | Zygisk Next | yes | yes | no | no |

No module has a `disable`, `remove`, or `skip_mount` file. `/data/adb/service.d` exists and is empty; `/data/adb/post-fs-data.d` does not exist. (The earlier note in the task about `gms_role_fix` forcing adb 5555 was not inspected here; the contents of the other modules' scripts were not read.)

---

## 11. Third-party packages and game detection

`cmd package list packages -3` returned 86 packages. `dumpsys package <pkg>` does NOT expose the manifest app category (checked: only intent-filter categories and package flags such as `HAS_CODE`, no `category=`/`isGame`). The category cannot be read from there. What works instead:

1. **`cmd game list-modes <pkg>`** (observed). For a game-category package it prints `<pkg> current mode: standard, available game modes: [standard,custom]`; for a non-game it prints `Package <pkg> is not of game type, to use the game mode commands, it must specify game category in the manifest as android:appCategory="game"`. This is the Android GameManagerService check on the manifest `appCategory`/`isGame`.
2. **`dumpsys game`** (observed): lists `No intervention found for package <pkg>` for exactly the packages GameManagerService considers games (11 lines).

Results over all 86 third-party packages: exactly 11 are of game type, no false positives:

| Package | Game type per `cmd game list-modes` |
|---|---|
| com.mobile.legends | yes |
| com.tencent.ig | yes |
| com.garena.game.codm | yes |
| com.dts.freefiremax | yes |
| com.roblox.client | yes |
| com.supercell.brawlstars | yes |
| com.supercell.clashroyale | yes |
| com.studio27.MelonPlayground | yes |
| com.paradyme.solarsmash | yes |
| com.geoguessr.app | yes |
| com.multicastgames.butcher | yes |
| **com.zeekerss.lethalcompany** | **no** ("not of game type") |

So the manifest game category identifies 11 of the 12 listed known games reliably and flags nothing else (e.g. Discord, Steam community app, Telegram, Netflix, Mother, etc. are all reported not-game). It misses `com.zeekerss.lethalcompany` (the mobile port, likely built without `android:appCategory="game"`), so auto-detection needs a manual allow-list override for that one (and for any similar sideloaded/ported game). Labels were not obtained cheaply (no label field in `cmd package` output; would need `aapt` or `dumpsys package` resource parsing), so the table lists package names only. Per-package game category flags for all 86 apps are reproduced by running `cmd game list-modes` in a loop on-device (it is a read-only query).

Other third-party apps relevant to tuning: `com.infinix.perftool` (Infinix performance tool, per earlier knowledge it can switch governors; not game type), `flar2.devcheck`, `com.github.uiautomator` (+ `.test`).

---

## 12. Open questions / not found

- Write formats and effects of every writable node are untested (by design). Specifically unverified: `fix_target_opp_index`, `fix_custom_freq_volt`, `custom_upbound_gpu_freq`, `custom_boost_gpu_freq`, `set_render_max_fps`, `fstb_fps_list`, `en_power_path` polarity, `current_cmd`, `set_cv`, `input_current_limit`, `/proc/game_mode`, `/proc/tp_report_rate_switch`.
- `fpsgo_status` row format with a running game is unknown (only the header was observed). The same for `render_info` and `fbt_info` perf rows. Needs a real game session to confirm.
- `dumpsys SurfaceFlinger --timestats -dump` is empty (not enabled); `--latency <layer>` returned only the period line for the layer name tried; the SurfaceFlinger `FPS ring buffer` meaning is unverified.
- `mtk_battery_cmd/en_power_path` current value is 0 while on battery with no charger; true bypass behaviour can only be tested while charging. `battery/charge_full_design` (500000) and `set_cv` (4450000 vs 4350 elsewhere) look inconsistent and may be driver quirks.
- Battery was at 6 % during the survey; charging-state values (`charger/*`, `mtk-master-charger/*`) are all "no charger" defaults.
- `scaling_min_freq`/`scaling_max_freq` for policy4/7 and `ged/hal/gpu_boost_level`, `fpsgo/fbt/boost_ta` have mtime 20:41 today, meaning some process (possibly a perf tool, a module, or another session) wrote them earlier. Their values now look like defaults (max/min), so nothing is currently capped. The writer was not identified. `com.infinix.perftool` is a candidate (assumed).
- Not read for risk reasons: `gpufreqv2/gpu_signed_opp_table`, `mfgsys_power_control`, `opp_stress_test`, `aging_mode`, `gpm_mode`, `asensor_info`, `/proc/ppm/*` dump tables other than `policy_status`, `dump_policy_list`, `enabled`, `exclusive_core`; write-only battery nodes (Permission denied on read).
- App labels for the game packages were not obtained.
- GPU values (current frequency, utilization) were sampled while the GPU was idle, so no load-time behaviour of `gpu_utilization` or `current_freqency` was observed.
- Housekeeping note: one command during the survey briefly wrote then deleted a scratch file in `/data/local/tmp` on the device while probing `dumpsys package` (a redirect to a temp file, removed immediately). No sysfs/procfs/settings/service state was changed.
