# Veil
- SimpleX Chat v7.0.3 tabanlı Android istemcisi; upstream davranışını gereksiz değiştirme, `// Veil:` kapılarını ve Tor-only politikasını koru.
- Yayın kararı NO-GO; gerçek cihaz veya bağımsız denetim olmadan doğrulanmış ya da yayına hazır deme. Kanıt: `docs/veil/DEVELOPMENT_STATUS.md`.
- Gizli anahtar, imza, keystore, `local.properties`, native `.so` ve kullanıcı verisi commit etme; native kütüphaneleri yalnız doğrulanmış kaynaktan edin.
- Hedefli kontrol: `bash scripts/ci/docs-check.sh`, `strings-check.sh`, `policy-check.sh`, `egress-check.sh`; Android için `cd apps/multiplatform && ./gradlew assembleFossDebug :common:testDebugUnitTest -Pveil.abis=arm64-v8a`. Cihaz kapıları açık kalır.
- `main` push'u CI'ı çalıştırır; sürüm veya dağıtım açık istek gerektirir.
