#!/usr/bin/env bash
# Sets up one Nuvex WireGuard exit node on a fresh Debian/Ubuntu VPS. Run as root.
#
#   sudo bash setup-node.sh
#
# Optional overrides: WG_IF (wg0), WG_PORT (51820), SUBNET (10.66.0).
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Run this script as root." >&2
  exit 1
fi

WG_IF="${WG_IF:-wg0}"
WG_PORT="${WG_PORT:-51820}"
SUBNET="${SUBNET:-10.66.0}"

apt-get update -y
apt-get install -y wireguard iptables

umask 077
mkdir -p /etc/wireguard
cd /etc/wireguard

if [[ ! -f server.key ]]; then
  wg genkey > server.key
fi
wg pubkey < server.key > server.pub

EXT_IF="$(ip -4 route show default | awk '{print $5; exit}')"
if [[ -z "$EXT_IF" ]]; then
  echo "Could not find the default network interface." >&2
  exit 1
fi

cat > "/etc/wireguard/${WG_IF}.conf" <<EOF
[Interface]
Address = ${SUBNET}.1/24
ListenPort = ${WG_PORT}
PrivateKey = $(cat server.key)
SaveConfig = false

# Users must not reach each other or the provider's private network.
PostUp = iptables -I FORWARD 1 -i ${WG_IF} -o ${WG_IF} -j DROP
PostUp = iptables -I FORWARD 2 -i ${WG_IF} -d 169.254.0.0/16 -j DROP
PostUp = iptables -I FORWARD 3 -i ${WG_IF} -d 10.0.0.0/8 -j DROP
PostUp = iptables -I FORWARD 4 -i ${WG_IF} -d 172.16.0.0/12 -j DROP
PostUp = iptables -I FORWARD 5 -i ${WG_IF} -d 192.168.0.0/16 -j DROP
PostUp = iptables -A FORWARD -i ${WG_IF} -j ACCEPT
PostUp = iptables -A FORWARD -o ${WG_IF} -m state --state RELATED,ESTABLISHED -j ACCEPT
PostUp = iptables -t nat -A POSTROUTING -s ${SUBNET}.0/24 -o ${EXT_IF} -j MASQUERADE
PostDown = iptables -D FORWARD -i ${WG_IF} -o ${WG_IF} -j DROP
PostDown = iptables -D FORWARD -i ${WG_IF} -d 169.254.0.0/16 -j DROP
PostDown = iptables -D FORWARD -i ${WG_IF} -d 10.0.0.0/8 -j DROP
PostDown = iptables -D FORWARD -i ${WG_IF} -d 172.16.0.0/12 -j DROP
PostDown = iptables -D FORWARD -i ${WG_IF} -d 192.168.0.0/16 -j DROP
PostDown = iptables -D FORWARD -i ${WG_IF} -j ACCEPT
PostDown = iptables -D FORWARD -o ${WG_IF} -m state --state RELATED,ESTABLISHED -j ACCEPT
PostDown = iptables -t nat -D POSTROUTING -s ${SUBNET}.0/24 -o ${EXT_IF} -j MASQUERADE
EOF
chmod 600 "/etc/wireguard/${WG_IF}.conf"

echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-nuvex.conf
sysctl --system >/dev/null

systemctl enable --now "wg-quick@${WG_IF}"

echo
echo "Node is up."
echo "Server public key (put this in NUVEX_NODE_PUBLIC_KEY): $(cat /etc/wireguard/server.pub)"
echo "Open UDP port ${WG_PORT} in the VPS firewall / cloud security group."
