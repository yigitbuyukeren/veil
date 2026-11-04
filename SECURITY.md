# Security policy (closed alpha)

No independent security audit has been completed; not distributed. Do not rely on Veil for high-stakes security until independently validated.

**Threat model:** relay operator observation, network correlation, malicious counterparty, compromised device, side-channel metadata, unauthorized group access and supply chain.

**Design invariants:** E2EE provided by mature SimpleX components (the official prebuilt core, verified by hash and signature, see [docs/veil/NATIVE_CORE.md](docs/veil/NATIVE_CORE.md)); per-contact identity; Tor-only transport; fail closed rather than route clear-text; no third-party tracking SDK; encrypt local chat history; no sensitive logs.

**Disclosure:** please use GitHub private vulnerability reporting when enabled. Do not publish exploit details affecting users until maintainers can assess and coordinate disclosure.

See the [threat model](docs/veil/THREAT_MODEL.md) and the [M1 release report](docs/veil/M1_RELEASE_REPORT.md) for the detailed threat model and release gate. Current status: [docs/veil/DEVELOPMENT_STATUS.md](docs/veil/DEVELOPMENT_STATUS.md).
