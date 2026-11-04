#!/usr/bin/env bash
# Source-level invariants of the Veil network and branding policy (cheap guards; the Kotlin unit tests cover behavior).
set -u
cd "$(git rev-parse --show-toplevel)"
fail=0
res=apps/multiplatform/common/src/commonMain/resources/MR
pol=apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/VeilNetworkPolicy.kt
bad=$(grep -h 'name="app_name"' $res/*/strings.xml | grep -vc '>Veil<')
[ "$bad" -eq 0 ] || { echo "POLICY FAIL: $bad app_name entries are not Veil"; fail=1; }
for c in 'UPSTREAM_PROMOTIONS = false' 'LINK_PREVIEWS_ENABLED = false' 'CALLS_ENABLED = false' 'socksMode = SocksMode.Always' 'hostMode = HostMode.OnionViaSocks' 'requiredHostMode = true' 'smpProxyMode = SMPProxyMode.Never' 'smpProxyFallback = SMPProxyFallback.Prohibit'; do
  grep -q "$c" $pol || { echo "POLICY FAIL: '$c' missing in VeilNetworkPolicy.kt"; fail=1; }
done
grep -q 'VeilNetworkPolicy.enforce(' apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt || { echo "POLICY FAIL: getNetCfg is not wrapped by enforce()"; fail=1; }
[ $fail -eq 0 ] && echo "policy check: PASS"
exit $fail
