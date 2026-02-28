// detect_variant.js
// Purpose: Expose environment variables and account configuration for Maestro flows.
// Allowed responsibilities ONLY:
// - Surface optional account-type flags and defaults (no UI selectors/logic)

// App ID handling
// If MAESTRO_APP_ID is already set (e.g. by a launcher script), we respect it.
const existingAppId = MAESTRO_APP_ID;

if (!existingAppId) {
  throw new Error("MAESTRO_APP_ID environment variable is required but was not set.");
}

output.appId = existingAppId;

// Account type (demo vs real)
const rawAccountType = MAESTRO_ACCOUNT_TYPE;
const isDemo = rawAccountType === "demo";
const isReal = rawAccountType === "real" || rawAccountType === "configured";

if (!isDemo && !isReal) {
  throw new Error(`Unknown MAESTRO_ACCOUNT_TYPE: ${rawAccountType}`);
}

output.isDemo = isDemo ? "true" : "false";
output.isReal = isReal ? "true" : "false";

// Defaults + optional overrides
const emailInput = MAESTRO_EMAIL_ADDRESS
const accountNameInput = MAESTRO_ACCOUNT_NAME
const userNameInput = MAESTRO_USER_NAME

const defaults = isDemo
  ? { email: "demo-user@example.com", accountName: "Demo User Account", userName: "Demo User" }
  : { email: "", accountName: "Demo User Account", userName: "Demo User" };

output.email = (emailInput === "undefined" || !emailInput) ? defaults.email : emailInput;
output.accountName = (accountNameInput === "undefined" || !accountNameInput) ? defaults.accountName : accountNameInput;
output.userName = (userNameInput === "undefined" || !userNameInput) ? defaults.userName : userNameInput;

console.log(`AppId: ${output.appId}`);
console.log(`Account type: ${isDemo ? "demo" : "real"}`);
console.log(`Account config:`);
console.log(`  Email: ${output.email}`)
console.log(`  AccountName: ${output.accountName}`)
console.log(`  UserName: ${output.userName}`)
