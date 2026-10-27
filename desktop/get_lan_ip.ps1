param(
    [switch]$All
)
# ------------------------------------------------------------
# get_lan_ip.ps1 - print the real LAN ipv4 of this PC
#
# Skips virtual adapters (VirtualBox / VMware / Hyper-V / WSL /
# Tailscale / Bluetooth) because a phone can never reach them.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File get_lan_ip.ps1
#   powershell ... -File get_lan_ip.ps1 -All   (list every candidate)
# ------------------------------------------------------------

$skip = 'vEthernet|VirtualBox|VMware|Loopback|Bluetooth|WSL|Tailscale|TAP|Npcap'

$candidates = Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object {
        $_.IPAddress -notlike '127.*' -and
        $_.IPAddress -notlike '169.254.*' -and
        $_.InterfaceAlias -notmatch $skip
    } |
    Sort-Object -Property SkipAsSource, InterfaceMetric

if ($All) {
    $candidates | ForEach-Object { "{0}`t{1}" -f $_.IPAddress, $_.InterfaceAlias }
    exit 0
}

$best = $candidates | Select-Object -First 1
if ($best) {
    Write-Output $best.IPAddress
    exit 0
}

exit 1
