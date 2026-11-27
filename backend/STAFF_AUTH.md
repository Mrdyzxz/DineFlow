# Staff authentication setup

The staff dashboard uses a server-verified username and password. Configure
`STAFF_USERNAME` and `STAFF_PASSWORD_HASH` in the server environment before
starting `app.ApiServer`. If either value is missing, staff login is disabled.
Do not commit credentials or environment files.

Generate the password hash from the repository root in PowerShell:

```powershell
New-Item -ItemType Directory -Force backend\bin | Out-Null
$sources = Get-ChildItem backend\src\main -Filter *.java -Recurse |
    ForEach-Object { $_.FullName }
javac -d backend\bin $sources
java -cp backend\bin app.StaffPasswordHash
```

The utility prompts for a password and confirmation without echoing them.
It requires at least 12 characters and prints a PBKDF2-HMAC-SHA256 hash; it
does not store the password. Set that output as `STAFF_PASSWORD_HASH` and set
the chosen account name as `STAFF_USERNAME` in the deployment's secret
configuration. The server supports one configured staff account.

Sessions are random bearer tokens held only in server memory, expire after
eight hours, and are revoked by logout. Restarting the server invalidates all
sessions. Orders and tracking tokens are also in memory and are lost on
restart. The dashboard keeps its token only in page memory, so a page reload
requires another sign-in.

The built-in HTTP server does not provide TLS or login rate limiting. Run it
behind a TLS-terminating reverse proxy and restrict network access in
production. Do not expose the HTTP listener directly to an untrusted network.

The standalone integration check can be compiled and run with:

```powershell
New-Item -ItemType Directory -Force backend\bin | Out-Null
$sources = Get-ChildItem backend\src\main, backend\src\test `
    -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -d backend\bin $sources
java -cp backend\bin app.StaffAuthenticationIntegrationTest
```
