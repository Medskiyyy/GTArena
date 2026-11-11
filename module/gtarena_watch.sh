#!/system/bin/sh
D=/data/adb/gtarena
H=$D/heartbeat
C=/sys/devices/system/cpu/cpufreq
G=/sys/kernel/ged/hal
L=/data/local/tmp/hwfix.log
NL='
'
tick=0

while true; do
    if [ -s $D/games ] && [ -z "$(pidof com.gtarena)" ]; then
        list="$NL$(<$D/games)$NL"
        for p in $(</dev/cpuset/top-app/cgroup.procs); do
            c=
            IFS= read -r -d '' c < /proc/$p/cmdline 2>/dev/null
            [ -n "$c" ] || continue
            case "$list" in
                *"$NL$c$NL"*)
                    cmd activity start-foreground-service -n com.gtarena/.service.ArenaService --es game "$c" >/dev/null 2>&1
                    echo "gtarena wake for $c uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L
                    sleep 10
                    break
                    ;;
            esac
        done
    fi
    tick=$((tick + 1))
    if [ $tick -ge 10 ]; then
        tick=0
        if [ -f $H ]; then
            hb=$(cat $H 2>/dev/null)
            case "$hb" in '' | *[!0-9]*) hb=0 ;; esac
            if [ $(($(date +%s) - hb)) -gt 60 ]; then
                echo 500000 > $C/policy0/scaling_min_freq
                echo 437000 > $C/policy4/scaling_min_freq
                echo 659000 > $C/policy7/scaling_min_freq
                echo 2000000 > $C/policy0/scaling_max_freq
                echo 2600000 > $C/policy4/scaling_max_freq
                echo 3000000 > $C/policy7/scaling_max_freq
                echo 1450000 > $C/policy0/schedhorizon/efficient_freq
                echo 1740000 > $C/policy4/schedhorizon/efficient_freq
                echo 1998000 > $C/policy7/schedhorizon/efficient_freq
                echo 20 > $C/policy0/schedhorizon/up_delay
                echo 30 > $C/policy4/schedhorizon/up_delay
                echo 40 > $C/policy7/schedhorizon/up_delay
                echo 0 > $G/custom_upbound_gpu_freq
                echo 40 > $G/custom_boost_gpu_freq
                settings put system peak_refresh_rate 120.0
                settings put system min_refresh_rate 60.0
                echo 0 > /proc/tp_report_rate_switch
                echo 0 > /proc/tp_transient_report_rate_switch
                echo '0 0' > /proc/mtk_battery_cmd/current_cmd
                if [ -f $D/thermal ]; then
                    /vendor/bin/thermal_manager /vendor/etc/.tp/thermal.conf
                    rm -f $D/thermal
                fi
                if [ -f $D/dnd ]; then
                    cmd notification set_dnd off
                    rm -f $D/dnd
                fi
                rm -f $H $D/profile
                echo "gtarena heartbeat stale -> seimbang uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L
            fi
        fi
    fi
    sleep 2
done
