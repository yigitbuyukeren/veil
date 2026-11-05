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
# Calls are a user opt-in that must default to off, and the call relay policy must stay forced.
grep -q 'veilCallsOptIn = mkBoolPreference(SHARED_PREFS_VEIL_CALLS_OPT_IN, false)' apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt || { echo "POLICY FAIL: veilCallsOptIn must default to false"; fail=1; }
grep -q 'fun callRelayPolicy(userPreference: Boolean): Boolean = true' $pol || { echo "POLICY FAIL: call relay policy is not forced"; fail=1; }
grep -q 'VeilNetworkPolicy.enforce(' apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt || { echo "POLICY FAIL: getNetCfg is not wrapped by enforce()"; fail=1; }
api=apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt
# apiSetNetworkConfig is the single choke point: CC.APISetNetworkConfig( may only be constructed inside it, and it must enforce the policy.
total=$(grep -rn 'CC\.APISetNetworkConfig(' apps/multiplatform --include='*.kt' | wc -l | tr -d ' ')
inside=$(awk '/suspend fun apiSetNetworkConfig\(/{f=1} f&&/CC\.APISetNetworkConfig\(/{n++} f&&/^  }$/{f=0} END{print n+0}' $api)
[ "$total" -eq 1 ] && [ "$inside" -eq 1 ] || { echo "POLICY FAIL: CC.APISetNetworkConfig( must appear exactly once, inside apiSetNetworkConfig (found $total, $inside inside)"; fail=1; }
awk '/suspend fun apiSetNetworkConfig\(/{f=1} f&&/CC\.APISetNetworkConfig\(VeilNetworkPolicy\.enforce\(/{ok=1} f&&/^  }$/{f=0} END{exit !ok}' $api || { echo "POLICY FAIL: apiSetNetworkConfig does not send VeilNetworkPolicy.enforce(cfg)"; fail=1; }
[ $fail -eq 0 ] && echo "policy check: PASS"
exit $fail
