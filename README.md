# HAProxyDetector Reloaded

[![](https://img.shields.io/github/downloads/mrweez/haproxy-detector-reloaded/total?style=for-the-badge)](https://github.com/mrweez/haproxy-detector-reloaded/releases) [![](https://img.shields.io/github/license/mrweez/haproxy-detector-reloaded?style=for-the-badge)](https://github.com/mrweez/haproxy-detector-reloaded/blob/master/LICENSE) [![](https://img.shields.io/bstats/servers/31890?label=Spigot%20Servers&style=for-the-badge)](https://bstats.org/plugin/bukkit/HAProxyDetector%20Reloaded/31890) [![](https://img.shields.io/bstats/servers/31892?label=BC%20Servers&style=for-the-badge)](https://bstats.org/plugin/bungeecord/HAProxyDetector%20Reloaded/31892) [![](https://img.shields.io/bstats/servers/31894?label=Velocity%20Servers&style=for-the-badge)](https://bstats.org/plugin/velocity/HAProxyDetector%20Reloaded/31894)

This [BungeeCord](https://github.com/SpigotMC/BungeeCord/) (and now [Spigot](https://www.spigotmc.org/wiki/spigot/)
and [Velocity](https://velocitypowered.com/)) plugin enables proxied and direct connections both at the same time.

## Security Warning

Allowing both direct and proxied connections has significant security implications — a malicious player can access the
server through their own HAProxy instance, thus tricking the server into believing the connection is coming from a
fake IP.

To counter this, this plugin implements IP whitelisting. **By default, only proxied connections from `localhost` will be
allowed** (direct connections aren't affected). You can add the IP/domain of your trusted HAProxy instance by
editing `whitelist.conf`, which can be found under the plugin data folder.

<details>
    <summary>Details of the whitelist format</summary>

```
# List of allowed proxy IPs
#
# An empty whitelist will disallow all proxies.
# Each entry must be an valid IP address, domain name or CIDR.
# Domain names will be resolved only once at startup.
# Each domain can have multiple A/AAAA records, all of them will be allowed.
# CIDR prefixes are not allowed in domain names.

127.0.0.0/8
::1/128
```

If you want to disable the whitelist (which you should never do), you can do so by
putting this line verbatim, before any other entries:

```
YesIReallyWantToDisableWhitelistItsExtremelyDangerousButIKnowWhatIAmDoing!!!
```

</details>

## Platform-specific Notes

#### BungeeCord

`proxy_protocol` needs to be enabled in BC `config.yml` for this plugin to work.

#### Velocity

`haproxy-protocol` needs to be enabled in Velocity config for this plugin to work.

#### Spigot and its derivatives

[ProtocolLib](https://github.com/dmulloy2/ProtocolLib) is a required dependency.
This version of the plugin supports both ProtocolLib 4.x and 5.x.

#### Paper

New versions of Paper have built-in HAProxy support (proxied connection only). It's not compatible with this plugin, so please disable the `proxy-protocol` option in `/config/paper-global.yml` (or `paper.yml` for older paper versions).

## Metrics

This plugin uses [bStats](https://bStats.org) for metrics. It collects some basic information, like how many people
use this plugin and the total player count. You can opt out at any time by editing the config file under
`plugins/bStats/`.
