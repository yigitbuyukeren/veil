# Security policy (planning stage)

No deployment or security audit has been completed. Do not rely on Veil for high-stakes security until independently validated.

**Threat model:** relay operator observation, network correlation, malicious counterparty, compromised device, side-channel metadata, unauthorized group access and supply chain.

**Design invariants:** E2EE provided by mature SimpleX components; per-contact identity; Tor-only transport; fail closed rather than route clear-text; no third-party tracking SDK; encrypt local chat history; no sensitive logs.

**Disclosure:** please use GitHub private vulnerability reporting when enabled. Do not publish exploit details affecting users until maintainers can assess and coordinate disclosure.

See VEIL-001 and VEIL-017 for the detailed threat model and release gate.
