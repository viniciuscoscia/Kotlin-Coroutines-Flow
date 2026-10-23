# Mac backend controls

The four IDE shell run configurations use PowerShell7 (`pwsh`) and the trusted SSH destination `viniciuscoscia@192.168.101.9`. Override `-MacHost` with an existing SSH alias when the address changes. Authentication stays in the user's SSH configuration; strict host-key verification and batch authentication remain enabled.

- **Backend Development** runs the deployed conventional JAR with stable Homebrew JDK25.0.4.1 and the `dev` profile on Mac loopback8080.
- **Backend Optimized** runs the measured Mac ARM64 native executable with the `optimized` profile on Mac loopback8080. Its proper Gradle build includes dependency reachability metadata.
- **Backend Private Tunnel** forwards Windows127.0.0.1:8080 to Mac127.0.0.1:8080.
- **DSH Private Reverse Tunnel** separately forwards Mac127.0.0.1:39241 to Windows127.0.0.1:39241. DSH must already be running on Windows. No token or authentication configuration is transferred.

Start a backend and its backend tunnel separately. DSH is independent from the KMP app/backend. Neither tunnel launches DSH or a model provider. All controls are on demand; no login service or system sleep setting is installed.

`pwsh -NoProfile -File tools/mac-backend.ps1 -Action Run -Profile dev` stays in the foreground, reports readiness and streams the current run's logs. IDE Stop/SSH session EOF cleans the owned backend. Logs and process identity records stay in the dedicated Mac project's `build/mac-runtime`. `-Action Start`, `Status`, and `Stop` support explicit lifecycle control. Stop validates the recorded PID's start time and full command before signaling it. It never searches for or kills other Java/server processes.

The default remote project is `/Users/viniciuscoscia/Workspace/Android/KotlinCoroutinesFlow`. Override `-RemoteProject` and `-Port` explicitly as needed. Backend profiles share the same default port and should be run one at a time. Preparation/deployment is separate: these controls execute accepted artifacts in `build/deployment/jvm/server.jar` and `build/deployment/native/server` and do not rebuild user sources automatically. Deployment metadata records the measured source preparation and artifact SHA256, independently of later benchmark builds.

For tunnel port overrides use `tools/private-tunnel.ps1 -Service Backend -LocalPort 18083 -RemotePort 18082`; for DSH use `-Service Dsh`, where LocalPort denotes Windows and RemotePort denotes Mac. Every listener is loopback only. IDE Stop ends only its foreground SSH tunnel.

Verified on the actual Mac: dev Start/Status/Stop; native Start/Stop and foreground Run/Stop; readiness via the backend tunnel; DSH reverse HTTP401 authentication gate; listener cleanup. Full backend acceptance and ten starts per variant are recorded in `build/benchmark-mac/mac-final-gradle` on Windows. No inference request was made.
