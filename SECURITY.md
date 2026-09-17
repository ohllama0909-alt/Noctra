# Security Policy

## Supported Versions

Only the latest release is actively maintained and receives security fixes.

| Version | Supported |
|---|---|
| Latest release | Yes |
| Older releases | No |

## Scope

Noctra is a **client-side only** Fabric mod. It does not run any server-side code and does not expose network endpoints. The attack surface is limited to:

- Reading and writing the local config file (`noctra.json`)
- In-game client interactions (inventory manipulation, chat interception)

Issues outside this scope (e.g. vulnerabilities in Minecraft itself, Fabric Loader, or Fabric API) should be reported to the respective upstream projects.

## Reporting a Vulnerability

If you discover a security issue in Noctra, please **do not open a public GitHub issue**.

Report privately via one of these channels:

- **GitHub Private Vulnerability Reporting:** [Report a vulnerability](https://github.com/ohllama0909-alt/Noctra/security/advisories/new)
- **Email:** niklasoliver77@icloud.com

Please include:
- A clear description of the vulnerability
- Steps to reproduce
- Potential impact
- A suggested fix if you have one

You will receive a response within **72 hours**. If the report is confirmed, a patched release will be published as soon as possible, and you will be credited in the release notes unless you prefer to remain anonymous.
