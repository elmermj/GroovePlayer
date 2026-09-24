#!/usr/bin/env bash
# GroovePlayer Tab S10 Ultra (RR2XC0049KN) viewport presets for layout QA.
# Usage:
#   ./wm_viewport_presets.sh list
#   ./wm_viewport_presets.sh apply <preset_id>
#   ./wm_viewport_presets.sh restore
# Serial override: SERIAL=RR2XC0049KN ./wm_viewport_presets.sh ...
set -euo pipefail
SERIAL="${SERIAL:-RR2XC0049KN}"
adb=(adb -s "$SERIAL")

restore() {
  "${adb[@]}" shell wm size reset
  "${adb[@]}" shell wm density reset
  # Native Tab S10 Ultra density after `wm density reset` is 280 (NOTE-001 / Tessa).
  # Do not force 240 here — reset is correct; 240 is only a QA preset override.
  echo "Restored native (expect ~1848x2960 @ 280dpi on Tab S10 Ultra)"
  "${adb[@]}" shell wm size
  "${adb[@]}" shell wm density
}

apply() {
  local size="$1" dens="$2" label="$3"
  "${adb[@]}" shell wm size "$size"
  "${adb[@]}" shell wm density "$dens"
  echo "Applied: $label -> size=$size density=$dens"
}

# id|WxH|density|label
PRESETS=$(cat <<'PRESETS'
# --- Galaxy S base / Plus / Ultra (portrait panel px) ---
s21|1080x2400|420|Galaxy S21 / S21+
s21u|1440x3200|515|Galaxy S21 Ultra
s22|1080x2340|420|Galaxy S22 / S22+ / S23 / S23+
s22u|1440x3088|500|Galaxy S22 Ultra / S23 Ultra
s24|1080x2340|420|Galaxy S24 / S25 / S26
s24p|1440x3120|510|Galaxy S24+ / S24 Ultra / S25+ / S25 Ultra / S26+ / S26 Ultra
# --- Z Fold cover = folded, main = unfolded ---
fold1_c|720x1680|400|Z Fold1 cover (folded)
fold1_m|1536x2152|362|Z Fold1 main (unfolded)
fold2_c|816x2260|386|Z Fold2 cover (folded)
fold2_m|1768x2208|373|Z Fold2 / Fold3 main (unfolded)
fold3_c|832x2268|387|Z Fold3 cover (folded)
fold4_c|904x2316|401|Z Fold4 / Fold5 cover (folded)
fold4_m|1812x2176|373|Z Fold4 / Fold5 main (unfolded)
fold6_c|968x2376|410|Z Fold6 cover (folded)
fold6_m|1856x2160|374|Z Fold6 main (unfolded)
fold7_c|1080x2520|422|Z Fold7 cover (folded)
fold7_m|1968x2184|368|Z Fold7 main (unfolded)
# Fold8: no published panel yet — use fold7_* until specs land
# --- Z Flip cover = folded, main = unfolded ---
flip1_m|1080x2636|425|Z Flip1 main (unfolded)
flip3_m|1080x2640|426|Z Flip3 / Flip4 / Flip5 / Flip6 main (unfolded)
flip5_c|720x748|300|Z Flip5 / Flip6 cover (folded) — tiny; optional
flip7_c|948x1048|350|Z Flip7 cover (folded)
flip7_m|1080x2520|397|Z Flip7 main (unfolded)
# Flip8: no published panel yet — use flip7_* until specs land
# Flip1–4 covers are ticker-sized; skip unless Jorge insists
# --- iPhone physical px @3x (aspect QA on Android) ---
ip12|1170x2532|460|iPhone 12 / 12 Pro / 13 / 13 Pro / 14
ip12pm|1284x2778|458|iPhone 12 Pro Max / 13 Pro Max / 14 Plus
ip14p|1179x2556|460|iPhone 14 Pro / 15 / 15 Pro / 16
ip14pm|1290x2796|460|iPhone 14 Pro Max / 15 Plus / 15 Pro Max / 16 Plus
ip16p|1206x2622|460|iPhone 16 Pro / 17 / 17 Pro
ip16pm|1320x2868|460|iPhone 16 Pro Max / 17 Pro Max
# --- iPad physical px @2x (264 ppi) ---
ipad_base11|1640x2360|264|iPad base 11 / Air 11
ipad_pro11|1668x2420|264|iPad Pro 11 current
ipad_air13|2048x2732|264|iPad Air 13 / older Pro 12.9
ipad_pro13|2064x2752|264|iPad Pro 13 current
# --- Galaxy Tab (portrait panel px) ---
tabs9|1600x2560|274|Galaxy Tab S9 / S11 base 11
tabs9p|1752x2800|266|Galaxy Tab S9+ / S10+
tabs9u|1848x2960|240|Galaxy Tab S9 Ultra / S10 Ultra / S11 Ultra (QA dens override; native reset=280)
# --- GroovePlayer layout buckets (fast path) ---
phone|1080x2340|420|PhoneLayout bucket
tablet|1400x2200|280|TabletLayout mid bucket
largetablet|1848x2960|240|LargeTablet QA proxy (dens 240 override; device native reset=280)
PRESETS
)

cmd="${1:-list}"
case "$cmd" in
  list)
    echo "$PRESETS" | grep -v '^#' | grep -v '^$' | while IFS='|' read -r id size dens label; do
      printf '%-12s  %-12s  dens=%-4s  %s\n' "$id" "$size" "$dens" "$label"
    done
    ;;
  restore) restore ;;
  apply)
    id="${2:?preset id required}"
    line=$(echo "$PRESETS" | grep -v '^#' | grep "^${id}|" | head -1 || true)
    if [[ -z "$line" ]]; then echo "Unknown preset: $id"; exit 1; fi
    IFS='|' read -r _ size dens label <<<"$line"
    apply "$size" "$dens" "$label"
    ;;
  *)
    echo "Usage: $0 {list|apply <id>|restore}"; exit 1
    ;;
esac
