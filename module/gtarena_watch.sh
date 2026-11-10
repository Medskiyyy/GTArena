#!/system/bin/sh
H=/data/adb/gtarena/heartbeat
C=/sys/devices/system/cpu/cpufreq
G=/sys/kernel/ged/hal
L=/data/local/tmp/hwfix.log

while true; do
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
            rm -f $H /data/adb/gtarena/profile
            echo "gtarena heartbeat stale -> seimbang uptime=$(cut -d' ' -f1 /proc/uptime)" >> $L
        fi
    fi
    sleep 20
done
