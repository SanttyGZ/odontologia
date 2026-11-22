# Skill Registry

**Uso del delegador.** Resolver solo las skills aplicables a la tarea y pasar sus reglas compactas en `## Project Standards (auto-resolved)` cuando haya delegación autorizada. Este resumen no sustituye la lectura del `SKILL.md` original cuando las instrucciones del entorno o del usuario la exijan.

Fecha de escaneo: **2026-11-19**. Proyecto: `odontologia`.

## Alcance y precedencia

- Inventario de archivos existentes: **111 skills únicas: 3 del proyecto, 72 globales y 36 de plugins**. No se instalaron ni activaron nuevas skills, dependencias o conexiones al generar este registro.
- Las tres seleccionadas están en `.agents/skills/` del proyecto. `springboot-patterns` fue rechazada y no está instalada ni catalogada como disponible.
- Los 20 registros `artifact-template-*` provienen de caché de `openai-templates`, no fueron anunciados como skills activas en esta sesión. Son fuentes en caché: comprobar capacidad/herramientas antes de resolverlos; no activarlos automáticamente por un trigger parecido.
- La existencia de un `SKILL.md` o de un plugin en disco no acredita conexión, herramientas utilizables ni autorización. Aplicar solo capacidades disponibles que correspondan al pedido; no desviar documentación local a Pages/Sites o servicios externos por coincidencia de palabras.
- Prevalecen las instrucciones vigentes del entorno y del usuario. El registro no habilita delegación por sí solo: crear subagentes únicamente cuando el pedido o las instrucciones aplicables la requieran/permitan explícitamente.
- Gobernanza recibida en la sesión: CRITICAL en acceso/seguridad/auditoría permite análisis y exige aprobación explícita para código; HIGH en configuración de datos/respaldo requiere propuesta revisada; MEDIUM aplica checkpoints; LOW permite autonomía con verificación pertinente. Un bloque de seguridad no autoriza ejecutar los procedimientos por mera inclusión en el catálogo.
- Primera entrega: un consultorio, un odontólogo y una agenda; ejecución local con Spring Boot/Gradle, React/TypeScript/Vite/CSS, PostgreSQL y SMTP. No convertir el inventario en decisiones de arquitectura: JPA/JDBC, seguridad, concurrencia y semántica de avisos conservan sus PT/PB/AT pendientes.
- Política visual vigente: `off`; no exigir sistema de diseño ni mockups rutinarios. La preferencia de instalación es **solo dentro del proyecto**. Aplicar límites del proyecto también a defaults de skills globales/de plugins.
- Las fuentes de estudio de seguridad que declaren validación pendiente deben contrastarse con su estado de validación y revisarse antes de ejecutar scripts. No confundir su instalación con verificación técnica, cobertura o cumplimiento.

## Scan Coverage

Se revisaron todas las raíces siguientes; las inexistentes no se crearon. Se deduplicó por `name` del frontmatter, con prioridad al proyecto y, entre fuentes globales, al primer hallazgo. Se excluyeron `sdd-*`, `_shared` y `skill-registry`.

| Scope | Root | Result |
|---|---|---|
| global | `/home/fernando/.claude/skills` | No existe |
| global | `/home/fernando/.config/opencode/skills` | Escaneada |
| global | `/home/fernando/.gemini/skills` | No existe |
| global | `/home/fernando/.cursor/skills` | No existe |
| global | `/home/fernando/.copilot/skills` | No existe |
| global | `/home/fernando/.agents/skills` | Escaneada |
| global | `/home/fernando/.codex/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/google-drive/0.1.27/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/pages/0.1.26/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/plugin-management/0.1.0/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/sites/1.1.7/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/visualize/1.0.89/skills` | Escaneada |
| plugin | `/home/fernando/.codex/plugins/cache/openai-curated-remote/work-pets/0.1.6/skills` | Escaneada |
| project | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.claude/skills` | No existe |
| project | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.gemini/skills` | No existe |
| project | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.agent/skills` | No existe |
| project | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/skills` | No existe |
| project | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.agents/skills` | Escaneada |
| project | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.codex/skills` | No existe |

Se encontraron 123 entradas tras las exclusiones; se descartaron 12 duplicados y 3 entradas excluidas. Las rutas con symlink se comprobaron con su destino real antes de catalogar.

## User Skills

| Trigger | Skill | Path | Scope / disponibilidad |
|---|---|---|---|
| /active-orchestrator:init, /active-orchestrator:kb, /active-orchestrator:design, /active-orchestrator:rules, /active-orchestrator:discovery, /active-orches… | `active-orchestrator` | `/home/fernando/.config/opencode/skills/active-orchestrator/SKILL.md` | global / instalada |
| cuando el usuario pide crear/generar/actualizar AGENTS.md o CLAUDE.md, "armar las reglas del proyecto", "instrucciones para los agentes", "generar claude.m… | `agents-md-generator` | `/home/fernando/.config/opencode/skills/agent-instruction/SKILL.md` | global / instalada |
| Detect dangerous ACL misconfigurations in Active Directory using ldap3 to identify GenericAll, WriteDACL, and WriteOwner abuse paths | `analyzing-active-directory-acl-abuse` | `/home/fernando/.agents/skills/analyzing-active-directory-acl-abuse/SKILL.md` | global / instalada |
| Uses the Linux Audit framework (auditd) with ausearch and aureport utilities to detect intrusion attempts, unauthorized access, privilege escalation, and s… | `analyzing-linux-audit-logs-for-intrusion` | `/home/fernando/.agents/skills/analyzing-linux-audit-logs-for-intrusion/SKILL.md` | global / instalada |
| analyzing network packets with scapy | `analyzing-network-packets-with-scapy` | `/home/fernando/.agents/skills/analyzing-network-packets-with-scapy/SKILL.md` | global / instalada |
| Captures and analyzes network packet data using Wireshark and tshark to identify malicious traffic patterns, diagnose protocol issues, extract artifacts, a… | `analyzing-network-traffic-with-wireshark` | `/home/fernando/.agents/skills/analyzing-network-traffic-with-wireshark/SKILL.md` | global / instalada |
| Parses Software Bill of Materials (SBOM) in CycloneDX and SPDX JSON formats to identify supply chain vulnerabilities by correlating components against the… | `analyzing-sbom-for-supply-chain-vulnerabilities` | `/home/fernando/.agents/skills/analyzing-sbom-for-supply-chain-vulnerabilities/SKILL.md` | global / instalada |
| Create a spreadsheet using the Analytics Dashboard template and its retained reference file. Use when the user selects or names Analytics Dashboard. Monito… | `artifact-template-analytics-dashboard` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-analytics-dashboard/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Business Review template and its retained reference file. Use when the user selects or names Business Review. Review busine… | `artifact-template-business-review` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-business-review/SKILL.md` | plugin / caché no anunciada |
| Create a document using the Design Report template and its retained reference file. Use when the user selects or names Design Report. Produce design report… | `artifact-template-design-report` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-design-report/SKILL.md` | plugin / caché no anunciada |
| Create a document using the Experiment Analysis template and its retained reference file. Use when the user selects or names Experiment Analysis. Analyze e… | `artifact-template-experiment-analysis` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-experiment-analysis/SKILL.md` | plugin / caché no anunciada |
| Create a spreadsheet using the Financial Budget template and its retained reference file. Use when the user selects or names Financial Budget. Model actual… | `artifact-template-financial-budget` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-financial-budget/SKILL.md` | plugin / caché no anunciada |
| Create a document using the Investment Committee Memo template and its retained reference file. Use when the user selects or names Investment Committee Mem… | `artifact-template-investment-committee-memo` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-investment-committee-memo/SKILL.md` | plugin / caché no anunciada |
| Create a document using the Legal Memorandum template and its retained reference file. Use when the user selects or names Legal Memorandum. Draft legal mem… | `artifact-template-legal-memorandum` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-legal-memorandum/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Market Trends Report template and its retained reference file. Use when the user selects or names Market Trends Report. Com… | `artifact-template-market-trends-report` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-market-trends-report/SKILL.md` | plugin / caché no anunciada |
| Create a document using the Minimal Letterhead template and its retained reference file. Use when the user selects or names Minimal Letterhead. Write profe… | `artifact-template-minimal-letterhead` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-minimal-letterhead/SKILL.md` | plugin / caché no anunciada |
| Create a spreadsheet using the Operating Calendar template and its retained reference file. Use when the user selects or names Operating Calendar. Plan ann… | `artifact-template-operating-calendar` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-operating-calendar/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Operating Review template and its retained reference file. Use when the user selects or names Operating Review. Run weekly… | `artifact-template-operating-review` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-operating-review/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Project Kickoff template and its retained reference file. Use when the user selects or names Project Kickoff. Align teams o… | `artifact-template-project-kickoff` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-project-kickoff/SKILL.md` | plugin / caché no anunciada |
| Create a spreadsheet using the Project Tracker template and its retained reference file. Use when the user selects or names Project Tracker. Manage workstr… | `artifact-template-project-tracker` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-project-tracker/SKILL.md` | plugin / caché no anunciada |
| Create a spreadsheet using the Sales Pipeline template and its retained reference file. Use when the user selects or names Sales Pipeline. Track opportunit… | `artifact-template-sales-pipeline` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-sales-pipeline/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Simple Dark Mode template and its retained reference file. Use when the user selects or names Simple Dark Mode. Create clea… | `artifact-template-simple-dark-mode` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-simple-dark-mode/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Simple Light Mode template and its retained reference file. Use when the user selects or names Simple Light Mode. Create cl… | `artifact-template-simple-light-mode` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-simple-light-mode/SKILL.md` | plugin / caché no anunciada |
| Create a document using the Strategy Memorandum template and its retained reference file. Use when the user selects or names Strategy Memorandum. Present s… | `artifact-template-strategy-memorandum` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-strategy-memorandum/SKILL.md` | plugin / caché no anunciada |
| Create a document using the System Design template and its retained reference file. Use when the user selects or names System Design. Document system archi… | `artifact-template-system-design` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-system-design/SKILL.md` | plugin / caché no anunciada |
| Create a presentation using the Team Alignment template and its retained reference file. Use when the user selects or names Team Alignment. Facilitate team… | `artifact-template-team-alignment` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-team-alignment/SKILL.md` | plugin / caché no anunciada |
| Create a spreadsheet using the Three-Statement Forecast template and its retained reference file. Use when the user selects or names Three-Statement Foreca… | `artifact-template-three-statement-forecast` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/openai-templates/0.1.1/skills/artifact-template-three-statement-forecast/SKILL.md` | plugin / caché no anunciada |
| Builds vendor-agnostic detection rules using the Sigma rule format for threat detection across SIEM platforms including Splunk, Elastic, and Microsoft Sent… | `building-detection-rules-with-sigma` | `/home/fernando/.agents/skills/building-detection-rules-with-sigma/SKILL.md` | global / instalada |
| Build a two-tier PKI Certificate Authority hierarchy (offline Root CA plus issuing Intermediate CA) using OpenSSL and the Python cryptography library, cove… | `configuring-certificate-authority-with-openssl` | `/home/fernando/.agents/skills/configuring-certificate-authority-with-openssl/SKILL.md` | global / instalada |
| Designs and implements VLAN-based (802.1Q) network segmentation on managed switches to isolate zones such as corporate, servers, DMZ, guest, and IoT, and t… | `configuring-network-segmentation-with-vlans` | `/home/fernando/.agents/skills/configuring-network-segmentation-with-vlans/SKILL.md` | global / instalada |
| Configures secure OAuth 2.0 authorization flows, including Authorization Code with PKCE, Client Credentials, and Device Authorization Grant, covering flow… | `configuring-oauth2-authorization-flow` | `/home/fernando/.agents/skills/configuring-oauth2-authorization-flow/SKILL.md` | global / instalada |
| Deploys and configures Suricata IDS/IPS with Emerging Threats rulesets, EVE JSON logging, and custom rules for high-throughput, protocol-aware traffic insp… | `configuring-suricata-for-network-monitoring` | `/home/fernando/.agents/skills/configuring-suricata-for-network-monitoring/SKILL.md` | global / instalada |
| Configures TLS 1.3 (RFC 8446) on servers, covering cipher suite and key-exchange group selection, and validates the resulting configuration with openssl s_… | `configuring-tls-1-3-for-secure-communications` | `/home/fernando/.agents/skills/configuring-tls-1-3-for-secure-communications/SKILL.md` | global / instalada |
| Configures Windows Event Logging with advanced audit policies to generate high-fidelity security events for threat detection and forensic investigation. Us… | `configuring-windows-event-logging-for-detection` | `/home/fernando/.agents/skills/configuring-windows-event-logging-for-detection/SKILL.md` | global / instalada |
| Create, repair, validate, preview, upload, or activate an animated v2 pet in ChatGPT Work mode from a character idea, brand cue, or reference image. Use fo… | `create-pet` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/work-pets/0.1.6/skills/create-pet/SKILL.md` | plugin / comprobar herramienta |
| Define or audit a project's visual identity during Active Orchestrator foundation, or when an older project first needs UI work. Ask once whether the proje… | `design-system` | `/home/fernando/.config/opencode/skills/design-system/SKILL.md` | global / instalada |
| The original v1 taste-skill, preserved for projects depending on its exact behavior. The current default is design-taste-frontend (v2 experimental), which… | `design-taste-frontend-v1` | `/home/fernando/.config/opencode/skills/design-taste-frontend-v1/SKILL.md` | global / instalada |
| Writes and tunes Falco rule syntax for container escape detection - conditions, macros, lists, priorities, and output fields - covering host filesystem mou… | `detecting-container-escape-with-falco-rules` | `/home/fernando/.agents/skills/detecting-container-escape-with-falco-rules/SKILL.md` | global / instalada |
| Corre la fase de Discovery/Investigacion antes de armar la Knowledge Base de un proyecto: investiga competidores via web-scraper si hay URLs, y guia una Q&… | `discovery-research` | `/home/fernando/.config/opencode/skills/discovery-research/SKILL.md` | global / instalada |
| This skill encodes Emil Kowalski's philosophy on UI polish, component design, animation decisions, and the invisible details that make software feel great. | `emil-design-eng` | `/home/fernando/.config/opencode/skills/emil-design-eng/SKILL.md` | global / instalada |
| Identifying and exploiting SSRF vulnerabilities to access internal services, cloud metadata, and restricted network resources during authorized penetration… | `exploiting-server-side-request-forgery` | `/home/fernando/.agents/skills/exploiting-server-side-request-forgery/SKILL.md` | global / instalada |
| Identifies and exploits SQL injection vulnerabilities in web applications during authorized penetration tests using manual techniques and automated tools l… | `exploiting-sql-injection-vulnerabilities` | `/home/fernando/.agents/skills/exploiting-sql-injection-vulnerabilities/SKILL.md` | global / instalada |
| Testing WebSocket implementations for authentication bypass, cross-site hijacking, injection attacks, and insecure message handling during authorized secur… | `exploiting-websocket-vulnerabilities` | `/home/fernando/.agents/skills/exploiting-websocket-vulnerabilities/SKILL.md` | global / instalada |
| Helps users discover and install agent skills when they ask questions like "how do I do X", "find a skill for X", "is there a skill that can...", or expres… | `find-skills` | `/home/fernando/.config/opencode/skills/find-skill/SKILL.md` | global / instalada |
| Create and edit native Google Docs through connected tools. | `google-docs` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/google-drive/0.1.27/skills/google-docs/SKILL.md` | plugin / comprobar herramienta |
| Use this skill to find and manage files in Google Drive, including organizing files, managing sharing, exporting files, and checking revision history. When… | `google-drive` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/google-drive/0.1.27/skills/google-drive/SKILL.md` | plugin / comprobar herramienta |
| Write, reply to, and resolve Google Drive comments on Docs, Sheets, Slides, and Drive files with evidence-backed location context. Use when the user asks t… | `google-drive-comments` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/google-drive/0.1.27/skills/google-drive-comments/SKILL.md` | plugin / comprobar herramienta |
| Analyze and edit connected Google Sheets with range precision. Use when the user wants to create Google Sheets, find a spreadsheet, inspect tabs or ranges,… | `google-sheets` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/google-drive/0.1.27/skills/google-sheets/SKILL.md` | plugin / comprobar herramienta |
| Route Google Slides authoring requests and derive a design system from a native template or reference deck. Use this skill when the user provides an existi… | `google-slides` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/google-drive/0.1.27/skills/google-slides/SKILL.md` | plugin / comprobar herramienta |
| Hardens Dockerfiles, images, and per-container runtime settings against the CIS Docker Benchmark v1.8.0: non-root users, dropped capabilities, read-only ro… | `hardening-docker-containers-for-production` | `/home/fernando/.agents/skills/hardening-docker-containers-for-production/SKILL.md` | global / instalada |
| Hardens the Docker daemon (dockerd) through /etc/docker/daemon.json with user namespace remapping, TLS client authentication, seccomp profiles, and CIS Doc… | `hardening-docker-daemon-configuration` | `/home/fernando/.agents/skills/hardening-docker-daemon-configuration/SKILL.md` | global / instalada |
| Hardens Linux endpoints using CIS Benchmark recommendations for Ubuntu, RHEL, and CentOS to reduce attack surface, enforce security baselines, and meet com… | `hardening-linux-endpoint-with-cis-benchmark` | `/home/fernando/.agents/skills/hardening-linux-endpoint-with-cis-benchmark/SKILL.md` | global / instalada |
| Generate or edit raster images when the task benefits from AI-created bitmap visuals such as photos, illustrations, textures, sprites, mockups, or transpar… | `imagegen` | `/home/fernando/.codex/skills/.system/imagegen/SKILL.md` | global / instalada |
| Guides implementing AES-256 encryption in GCM mode (FIPS 197) for files and data stores at rest, covering key derivation, IV/nonce management, and authenti… | `implementing-aes-encryption-for-data-at-rest` | `/home/fernando/.agents/skills/implementing-aes-encryption-for-data-at-rest/SKILL.md` | global / instalada |
| Implements API rate limiting and throttling with token bucket, sliding window, and fixed window algorithms, configuring per-user, per-IP, and per-endpoint… | `implementing-api-rate-limiting-and-throttling` | `/home/fernando/.agents/skills/implementing-api-rate-limiting-and-throttling/SKILL.md` | global / instalada |
| Implements API schema validation using OpenAPI Specification and JSON Schema documents, enforced both at the API gateway (runtime) and during development (… | `implementing-api-schema-validation-security` | `/home/fernando/.agents/skills/implementing-api-schema-validation-security/SKILL.md` | global / instalada |
| Implements digital signatures using the Ed25519 algorithm (Curve25519), covering key-pair generation, signing, signature verification, and security tradeof… | `implementing-digital-signatures-with-ed25519` | `/home/fernando/.agents/skills/implementing-digital-signatures-with-ed25519/SKILL.md` | global / instalada |
| Implements secure JWT (RFC 7519) signing and verification using HMAC-SHA256, RSA-PSS, ES256, and EdDSA, including token expiration, claims validation, and… | `implementing-jwt-signing-and-verification` | `/home/fernando/.agents/skills/implementing-jwt-signing-and-verification/SKILL.md` | global / instalada |
| Implements memory protection mechanisms including DEP (Data Execution Prevention), ASLR (Address Space Layout Randomization), CFG (Control Flow Guard), and… | `implementing-memory-protection-with-dep-aslr` | `/home/fernando/.agents/skills/implementing-memory-protection-with-dep-aslr/SKILL.md` | global / instalada |
| This skill covers implementing Gitleaks for detecting and preventing hardcoded secrets in git repositories. It addresses configuring pre-commit hooks, CI/C… | `implementing-secret-scanning-with-gitleaks` | `/home/fernando/.agents/skills/implementing-secret-scanning-with-gitleaks/SKILL.md` | global / instalada |
| Write custom Semgrep SAST rules in YAML to detect application-specific vulnerabilities, enforce coding standards, and integrate into CI/CD pipelines. | `implementing-semgrep-for-custom-sast-rules` | `/home/fernando/.agents/skills/implementing-semgrep-for-custom-sast-rules/SKILL.md` | global / instalada |
| Integrates OWASP ZAP (Zed Attack Proxy) into GitHub Actions and GitLab CI pipelines, covering baseline, full, and API scan configuration against running ap… | `integrating-dast-with-owasp-zap-in-pipeline` | `/home/fernando/.agents/skills/integrating-dast-with-owasp-zap-in-pipeline/SKILL.md` | global / instalada |
| Integrates CodeQL and Semgrep SAST scanning into GitHub Actions, covering scans on pull requests/pushes, rule tuning to cut false positives, SARIF upload t… | `integrating-sast-into-github-actions-pipeline` | `/home/fernando/.agents/skills/integrating-sast-into-github-actions-pipeline/SKILL.md` | global / instalada |
| When user asks to create, build, or generate a knowledge base for a project, document a project from source documents (.txt, .docx, .pdf), or asks to "arma… | `kb-creator` | `/home/fernando/.config/opencode/skills/kb-creator/SKILL.md` | global / instalada |
| Reconcile new evidence into existing ChatGPT Pages or a Space when the user requests upkeep. For a direct text correction, use write-page. Do not use for o… | `maintain-space` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/pages/0.1.26/skills/maintain-space/SKILL.md` | plugin / comprobar herramienta |
| Review a ChatGPT Space page and its schedules, recommend useful recurring work, and create, update, or remove scheduled automations. | `manage-schedules` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/pages/0.1.26/skills/manage-schedules/SKILL.md` | plugin / comprobar herramienta |
| Collect Active Directory data with SharpHound and Entra ID data with AzureHound, ingest into BloodHound Community Edition, and analyze on-prem, cloud, and… | `mapping-attack-paths-with-bloodhound-ce` | `/home/fernando/.agents/skills/mapping-attack-paths-with-bloodhound-ce/SKILL.md` | global / instalada |
| API first-timers, Codex/Work Q&A. If the user already has an OpenAI API integration, skip this skill for API questions and use the developer docs. Read thi… | `openai-docs` | `/home/fernando/.codex/skills/.system/openai-docs/SKILL.md` | global / instalada |
| Organize existing ChatGPT Pages and Space structure when the user requests changes to that structure. Do not use for ordinary chat drafts or advice; a sele… | `organize-space` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/pages/0.1.26/skills/organize-space/SKILL.md` | plugin / comprobar herramienta |
| Uses Postman to build structured API security test collections covering the OWASP API Security Top 10—authentication bypass, authorization flaws, injection… | `performing-api-security-testing-with-postman` | `/home/fernando/.agents/skills/performing-api-security-testing-with-postman/SKILL.md` | global / instalada |
| Analyze ELF binaries for memory-corruption vulnerabilities and build proof-of-concept exploits using pwntools, checksec, and ROPgadget for buffer overflows… | `performing-binary-exploitation-analysis` | `/home/fernando/.agents/skills/performing-binary-exploitation-analysis/SKILL.md` | global / instalada |
| Investigate AWS account compromise by querying CloudTrail with boto3's LookupEvents or AWS Athena SQL over S3-delivered logs, filtering on suspicious user… | `performing-cloud-forensics-with-aws-cloudtrail` | `/home/fernando/.agents/skills/performing-cloud-forensics-with-aws-cloudtrail/SKILL.md` | global / instalada |
| Testing web applications for Cross-Site Request Forgery vulnerabilities by crafting forged requests that exploit authenticated user sessions during authori… | `performing-csrf-attack-simulation` | `/home/fernando/.agents/skills/performing-csrf-attack-simulation/SKILL.md` | global / instalada |
| Test web applications for path traversal and Local/Remote File Inclusion vulnerabilities by manipulating file path parameters, applying encoding and filter… | `performing-directory-traversal-testing` | `/home/fernando/.agents/skills/performing-directory-traversal-testing/SKILL.md` | global / instalada |
| performing dns enumeration and zone transfer | `performing-dns-enumeration-and-zone-transfer` | `/home/fernando/.agents/skills/performing-dns-enumeration-and-zone-transfer/SKILL.md` | global / instalada |
| Performs coverage-guided fuzzing of compiled binaries with AFL++, instrumenting targets via afl-cc/afl-clang-fast, minimizing corpora with afl-cmin and afl… | `performing-fuzzing-with-aflplusplus` | `/home/fernando/.agents/skills/performing-fuzzing-with-aflplusplus/SKILL.md` | global / instalada |
| performing linux log forensics investigation | `performing-linux-log-forensics-investigation` | `/home/fernando/.agents/skills/performing-linux-log-forensics-investigation/SKILL.md` | global / instalada |
| Analyze volatile memory (RAM) dumps using the Volatility 3 framework to extract running processes, network connections, loaded modules, credentials, and en… | `performing-memory-forensics-with-volatility3` | `/home/fernando/.agents/skills/performing-memory-forensics-with-volatility3/SKILL.md` | global / instalada |
| Deploy Zeek (formerly Bro) as a passive network security monitor to generate structured logs of protocol metadata (HTTP, DNS, TLS, SSH, SMTP, FTP, and more… | `performing-network-traffic-analysis-with-zeek` | `/home/fernando/.agents/skills/performing-network-traffic-analysis-with-zeek/SKILL.md` | global / instalada |
| Guides manual enumeration and automated tooling to escalate from a low-privilege Linux user to root by exploiting misconfigurations, vulnerable services, k… | `performing-privilege-escalation-on-linux` | `/home/fernando/.agents/skills/performing-privilege-escalation-on-linux/SKILL.md` | global / instalada |
| performing security headers audit | `performing-security-headers-audit` | `/home/fernando/.agents/skills/performing-security-headers-audit/SKILL.md` | global / instalada |
| Performs forensic analysis of SQLite databases by examining B-tree page structures, recovering deleted records from freelist pages and Write-Ahead Log (WAL… | `performing-sqlite-database-forensics` | `/home/fernando/.agents/skills/performing-sqlite-database-forensics/SKILL.md` | global / instalada |
| Performs static analysis of Windows PE malware samples using PEStudio to examine file headers, imports, strings, and resources without executing the binary… | `performing-static-malware-analysis-with-pe-studio` | `/home/fernando/.agents/skills/performing-static-malware-analysis-with-pe-studio/SKILL.md` | global / instalada |
| Uses OWASP Threat Dragon (web or desktop) to build data flow diagrams, identify threats with STRIDE, LINDDUN, CIA, DIE, or PLOT4ai methodologies via its au… | `performing-threat-modeling-with-owasp-threat-dragon` | `/home/fernando/.agents/skills/performing-threat-modeling-with-owasp-threat-dragon/SKILL.md` | global / instalada |
| Develops precise YARA and YARA-X rules for malware detection by identifying unique strings, byte sequences, PE header traits, and behavioral indicators in… | `performing-yara-rule-development-for-detection` | `/home/fernando/.agents/skills/performing-yara-rule-development-for-detection/SKILL.md` | global / instalada |
| List, inspect, select, download, or delete animated pets in ChatGPT Work mode. Use for an explicit pet in ChatGPT or established ChatGPT Pets context. Excl… | `pets` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/work-pets/0.1.6/skills/pets/SKILL.md` | plugin / comprobar herramienta |
| Discover and suggest relevant plugins, inspect app permissions and dependencies, and manage plugin connections or removal. Use when the user asks about plu… | `plugin-management` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/plugin-management/0.1.0/skills/plugin-management/SKILL.md` | plugin / comprobar herramienta |
| Reverse engineers malware binaries using NSA''s Ghidra disassembler and decompiler to study internal logic, cryptographic routines, C2 protocols, and evasi… | `reverse-engineering-malware-with-ghidra` | `/home/fernando/.agents/skills/reverse-engineering-malware-with-ghidra/SKILL.md` | global / instalada |
| Perform a read-only, defect-first review of a specified code change and return every actionable finding. Use when another agent delegates review of uncommi… | `review-agent` | `/home/fernando/.codex/skills/.system/review-agent/SKILL.md` | global / instalada |
| When user asks to create, build, regenerate, or update CHANGES.md, roadmap, change map, implementation plan; or says "armar CHANGES", "armar roadmap", "cre… | `roadmap-generator` | `/home/fernando/.config/opencode/skills/roadmap-generator/SKILL.md` | global / instalada |
| scanning network with nmap advanced | `scanning-network-with-nmap-advanced` | `/home/fernando/.agents/skills/scanning-network-with-nmap-advanced/SKILL.md` | global / instalada |
| Hardens AWS IAM configurations to enforce least-privilege access, covering IAM policy scoping, permission boundaries, IAM Access Analyzer integration, and… | `securing-aws-iam-permissions` | `/home/fernando/.agents/skills/securing-aws-iam-permissions/SKILL.md` | global / instalada |
| Use Sites when the user wants a website built for them (including a landing page, portfolio, dashboard, portal, tracker, hub, game, event registration, or… | `sites` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/sites/1.1.7/skills/sites/SKILL.md` | plugin / comprobar herramienta |
| Remix a Site built with the Sites plugin into a new Site. Use when the user has identified the source Site and asks to remix it. | `sites-remix` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/sites/1.1.7/skills/sites-remix/SKILL.md` | plugin / comprobar herramienta |
| Create new skills, modify and improve existing skills, and measure skill performance. Use when users want to create a skill from scratch, edit, or optimize… | `skill-creator` | `/home/fernando/.config/opencode/skills/skill-creator/SKILL.md` | global / instalada |
| Install Codex skills into $CODEX_HOME/skills from a curated list or a GitHub repo path. Use when a user asks to list installable skills, install a curated… | `skill-installer` | `/home/fernando/.codex/skills/.system/skill-installer/SKILL.md` | global / instalada |
| Postgres best practices maintained by Supabase, for Postgres running anywhere. Load this skill BEFORE writing or changing anything that lives in a Postgres… | `supabase-postgres-best-practices` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.agents/skills/supabase-postgres-best-practices/SKILL.md` | proyecto / instalada |
| Identifying and exploiting Cross-Origin Resource Sharing misconfigurations that allow unauthorized cross-domain data access and credential theft during sec… | `testing-cors-misconfiguration` | `/home/fernando/.agents/skills/testing-cors-misconfiguration/SKILL.md` | global / instalada |
| Systematically tests web applications and APIs for broken access control (OWASP A01:2021), including privilege escalation, missing function-level checks, i… | `testing-for-broken-access-control` | `/home/fernando/.agents/skills/testing-for-broken-access-control/SKILL.md` | global / instalada |
| Manually identifies flaws in application business logic - price manipulation, multi-step workflow bypass, and privilege escalation - by intercepting and mo… | `testing-for-business-logic-vulnerabilities` | `/home/fernando/.agents/skills/testing-for-business-logic-vulnerabilities/SKILL.md` | global / instalada |
| Tests web applications for reflected, stored, and DOM-based Cross-Site Scripting by injecting JavaScript payloads with Burp Suite (XSS extensions, Active S… | `testing-for-xss-vulnerabilities` | `/home/fernando/.agents/skills/testing-for-xss-vulnerabilities/SKILL.md` | global / instalada |
| Discovering and exploiting XML External Entity injection vulnerabilities to read server files, perform SSRF, and exfiltrate data during authorized penetrat… | `testing-for-xxe-injection-vulnerabilities` | `/home/fernando/.agents/skills/testing-for-xxe-injection-vulnerabilities/SKILL.md` | global / instalada |
| Assessing JSON Web Token implementations for cryptographic weaknesses, algorithm confusion attacks, and authorization bypass vulnerabilities during securit… | `testing-jwt-token-security` | `/home/fernando/.agents/skills/testing-jwt-token-security/SKILL.md` | global / instalada |
| Tests OAuth 2.0 and OpenID Connect implementations for authorization code interception, redirect URI manipulation, CSRF in OAuth flows, token leakage, scop… | `testing-oauth2-implementation-flaws` | `/home/fernando/.agents/skills/testing-oauth2-implementation-flaws/SKILL.md` | global / instalada |
| Performs initial triage of security incidents using the NIST SP 800-61r3 and SANS PICERL frameworks, classifying incident type, assigning priority by busin… | `triaging-security-incident` | `/home/fernando/.agents/skills/triaging-security-incident/SKILL.md` | global / instalada |
| Inspect, validate, preview, repair, or update a custom pet in ChatGPT Work mode, including its name, description, or sprite sheet. Use for a pet in ChatGPT… | `update-pet` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/work-pets/0.1.6/skills/update-pet/SKILL.md` | plugin / comprobar herramienta |
| React and Next.js performance optimization guidelines from Vercel Engineering. This skill should be used when writing, reviewing, or refactoring React/Next… | `vercel-react-best-practices` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.agents/skills/vercel-react-best-practices/SKILL.md` | proyecto / instalada |
| Create visualizations and interactive tools directly in conversation. Proactively use to show how something works; explore 'what happens when', 'what chang… | `visualize` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/visualize/1.0.89/skills/visualize/SKILL.md` | plugin / comprobar herramienta |
| Investiga competidores o soluciones existentes a partir de una URL: extrae problema que resuelve, usuarios objetivo, features, pricing y posicionamiento en… | `web-scraper` | `/home/fernando/.config/opencode/skills/web-scraper/SKILL.md` | global / instalada |
| Toolkit for interacting with and testing local web applications using Playwright. Supports verifying frontend functionality, debugging UI behavior, capturi… | `webapp-testing` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.agents/skills/webapp-testing/SKILL.md` | proyecto / instalada |
| Create or edit requested Page/Space content, or use for prose you have already decided to save as a standalone Markdown file, only if the user did not expl… | `write-page` | `/home/fernando/.codex/plugins/cache/openai-curated-remote/pages/0.1.26/skills/write-page/SKILL.md` | plugin / comprobar herramienta |

## Compact Rules

Reglas extraídas de las fuentes instaladas, con límites del proyecto explicitados. En documentos de más de 200 líneas se priorizaron frontmatter y secciones de reglas/patrones; se omitieron ejemplos de código extensos. Las prioridades de rendimiento de una skill no sustituyen la gobernanza de cambios.

### active-orchestrator

- Delegar cada fase a su skill; mantener interactivas las fases que preguntan al usuario.
- Escribir solo version, step y owner; preservar secciones ajenas y respaldar antes de cambios atómicos.
- Avanzar de fase solo con indicación explícita; no repetir fases completas al reanudar.
- Recomendar skills antes de instalar y ejecutar solo la selección y alcance autorizados.
- Respetar visual_design.mode persistido; off no vuelve a preguntar por sistema ni mockups.

### agents-md-generator

- Leer stack y estados desde KB, roadmap y registro; no inventar skills.
- Escribir AGENTS.md y CLAUDE.md en raíz con contenido equivalente.
- Verificar qué reglas globales existen antes de referenciarlas o evitar duplicarlas.
- Presentar reglas específicas del stack y escribir solo las confirmadas por el usuario.
- Mapear skill a rol y referenciar compact rules sin copiarlas al archivo de instrucciones.

### analyzing-active-directory-acl-abuse

- Revisar el estado técnico y alcance antes de ejecutar consultas o scripts de estudio.
- Obtener descriptores de seguridad y clases de objetos mediante LDAP autorizado.
- Resolver SID de cada trustee y diferenciar ACE allow/deny.
- Identificar GenericAll, WriteDACL, WriteOwner y escrituras sensibles en objetos privilegiados.
- Documentar objeto, principal, permiso, camino potencial y remediación sin modificar ACL automáticamente.

### analyzing-linux-audit-logs-for-intrusion

- Comprobar auditd, reglas cargadas y eventos perdidos antes de interpretar cobertura.
- Consultar eventos por clave, usuario y ventana temporal con ausearch.
- Generar resúmenes de autenticación, ejecutables y archivos con aureport.
- Correlacionar eventos y ordenar la línea temporal preservando identificadores.
- Revisar reglas persistentes y forwarding solo dentro del alcance autorizado; no confundirlos con reglas temporales.

### analyzing-network-packets-with-scapy

- Analizar copias offline; el auxiliar no captura ni emite tráfico vivo.
- Correlacionar sentidos inversos, secuencias y ACK; separar retransmisiones de conexiones nuevas.
- Excluir fragmentos del transporte y rechazar túneles no admitidos; no afirmar reensamblado.
- Tratar longitud/entropía DNS y desequilibrio SYN como heurísticas, no ataques confirmados.
- Conservar límites del lector, errores y menor precisión cuando faltan secuencias.

### analyzing-network-traffic-with-wireshark

- Delimitar interfaz, hosts, protocolos y volumen antes de capturar.
- Distinguir filtros de captura de filtros de visualización; acotar disco con buffers.
- Correlacionar conversaciones y estadísticas antes de seguir streams.
- Exportar paquetes/objetos pertinentes y conservar hashes y trazabilidad.
- Considerar tráfico cifrado y captura incompleta antes de concluir ausencia de actividad.

### analyzing-sbom-for-supply-chain-vulnerabilities

- Distinguir CycloneDX/SPDX y validar versión y componentes disponibles.
- Relacionar nombres/versiones con CVE y conservar incertidumbre de coincidencias CPE.
- Recorrer dependencias transitivas al calcular alcance y prioridad.
- Respetar límites NVD y contrastar hallazgos con otra fuente de análisis.
- Informar componentes ausentes, bibliotecas embebidas y cobertura parcial del SBOM.

### artifact-template-analytics-dashboard

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de hoja de cálculo y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-business-review

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-design-report

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-experiment-analysis

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-financial-budget

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de hoja de cálculo y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-investment-committee-memo

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-legal-memorandum

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-market-trends-report

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-minimal-letterhead

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-operating-calendar

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de hoja de cálculo y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-operating-review

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-project-kickoff

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-project-tracker

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de hoja de cálculo y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-sales-pipeline

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de hoja de cálculo y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-simple-dark-mode

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-simple-light-mode

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-strategy-memorandum

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-system-design

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de documento y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-team-alignment

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de presentación y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### artifact-template-three-statement-forecast

- Leer artifact-template.json y resolver referencias relativas al directorio de la skill.
- Usar capacidad preinstalada de hoja de cálculo y su workflow de plantilla; si falta, informar y detener esa operación.
- Copiar/importar la referencia sin modificar el original ni sustituir su sistema de formato.
- Completar solo hechos sustentados y conservar estructura y elementos nativos de la referencia.
- Renderizar y verificar fidelidad antes de entregar; caché no acredita capacidad activa en esta sesión.

### building-detection-rules-with-sigma

- Definir logsource, selección y condición desde evidencia de amenaza.
- Mantener metadatos, estado y etiquetas ATT&CK coherentes.
- Validar sintaxis antes de convertir con el backend SIEM correspondiente.
- Probar muestras positivas y negativas y documentar falsos positivos.
- Versionar reglas y desplegar únicamente tras revisión del alcance.

### configuring-certificate-authority-with-openssl

- Separar Root CA offline de la CA emisora.
- Proteger claves privadas y distinguir extensiones CA de certificados finales.
- Aplicar basicConstraints, keyUsage y límites de cadena apropiados.
- Configurar CRL/OCSP y comprobar revocación según el diseño.
- Verificar cadena completa y políticas antes de usar certificados emitidos.

### configuring-network-segmentation-with-vlans

- Definir zonas y matriz de tráfico permitido antes de configurar VLAN.
- Restringir listas de VLAN en trunks y deshabilitar negociación DTP innecesaria.
- Evitar VLAN nativa por defecto y revisar protecciones DHCP/ARP según plataforma.
- Aplicar ACL entre zonas; una VLAN sola no cifra ni resuelve aislamiento completo.
- Probar rutas permitidas y denegadas y conservar evidencia antes de desplegar.

### configuring-oauth2-authorization-flow

- Seleccionar grant adecuado; Authorization Code usa PKCE S256.
- Validar state y coincidencia estricta de redirect URI.
- Limitar scopes y verificar audiencia y expiración en recursos.
- Definir rotación, revocación y almacenamiento de tokens sin exponer secretos.
- No incorporar OAuth al MVP por inventario; mecanismo de acceso sigue pendiente de aprobación.

### configuring-suricata-for-network-monitoring

- Definir HOME_NET e interfaz y distinguir IDS pasivo de IPS inline.
- Validar configuración y rulesets antes de habilitarlos.
- Emitir EVE JSON con campos y community-id necesarios para correlación.
- Ajustar firmas y umbrales sobre una línea base para reducir falsos positivos.
- Medir pérdida de paquetes y recursos; un alertado no demuestra explotación exitosa.

### configuring-tls-1-3-for-secure-communications

- Verificar soporte TLS y cadena de certificados antes de configurar el servicio.
- Seleccionar protocolos, suites y grupos compatibles con requisitos de clientes.
- Restringir 0-RTT por su riesgo de replay.
- Comprobar handshake y rechazo de protocolos deshabilitados con herramientas apropiadas.
- Revisar OCSP/HSTS según despliegue; no trasladar parámetros públicos al HTTP local por inferencia.

### configuring-windows-event-logging-for-detection

- Usar Advanced Audit Policy evitando conflictos con políticas básicas.
- Habilitar creación de procesos y línea de comando cuando el alcance lo requiera.
- Dimensionar logs según volumen y retención del entorno.
- Configurar forwarding autorizado y verificar recepción central.
- Comprobar eventos de prueba y persistencia de la política antes de declarar cobertura.

### create-pet

- Preservar referencias de identidad y todos los artefactos útiles en Library.
- Usar imagegen para arte nuevo y pipeline incluido para ensamblar y validar.
- Respetar contrato v2 de animaciones y 16 direcciones; inspeccionar previews.
- Crear el pet solo después de validar los bytes exactos y obtener upload_session_id.
- Conservar pet.id estable, verificar colección y activar solo si el usuario lo pidió.

### design-system

- Leer política persistida antes de crear una base visual; no sobrescribir identidad existente.
- Si JSON y README discrepan, reconciliar la política antes de aplicar UI.
- Conservar estado anterior y escribir solo visual_design con respaldo y lectura posterior.
- En off crear solo README de política y omitir sistema y mockups rutinarios.
- En modos con sistema, renderizar componentes y esperar aprobación explícita antes de declararlo aprobado.

### design-taste-frontend-v1

- Seguir stack y preferencias explícitas del proyecto antes que defaults de esta skill.
- Verificar dependencias y versiones antes de importar librerías o usar sintaxis específica.
- Implementar estados de carga, vacío, error y respuesta a interacción.
- Adaptar layout a móvil y usar CSS Grid para columnas sin cálculos frágiles.
- Animar transform/opacity y evitar estado React para movimiento continuo; no imponer Tailwind/RSC al proyecto.

### detecting-container-escape-with-falco-rules

- Separar listas, macros y reglas Falco con campos válidos para el motor instalado.
- Detectar montajes, namespaces, sockets y rutas sensibles con contexto de contenedor.
- Incluir comando, imagen y contenedor en salida con prioridad y etiquetas.
- Probar reglas en entorno controlado antes de habilitar enforcement.
- Afinar excepciones por comportamiento legítimo y no confundir ejecución sospechosa con escape confirmado.

### discovery-research

- Reutilizar evidencia disponible sin volver a preguntar decisiones ya resueltas.
- Delegar extracción de cada URL a web-scraper y no ampliar a crawling masivo.
- Cubrir problema, usuarios, casos, funcionalidades, reglas, integraciones y restricciones.
- Separar hechos, supuestos, riesgos y preguntas abiertas.
- Registrar Discovery y su sección de estado solo después de la confirmación explícita del resumen.

### emil-design-eng

- Justificar cada animación por estado, continuidad o respuesta a la acción.
- Usar transiciones interrumpibles y origen de transform coherente con el componente.
- Animar principalmente transform/opacity y medir comportamiento bajo carga.
- Respetar reduced-motion y adaptar hover a dispositivos táctiles.
- En revisiones UI usar tabla Before/After; no introducir librerías o rediseños fuera del alcance.

### exploiting-server-side-request-forgery

- Mapear entradas que producen solicitudes del servidor dentro del alcance autorizado.
- Diferenciar SSRF con respuesta de SSRF ciega y corroborar con evidencia OOB controlada.
- Revisar validación de esquema, dirección, redirecciones y resoluciones DNS.
- Limitar pruebas de recursos internos/metadata al permiso acordado.
- Documentar solicitud, respuesta, impacto y mitigación sin recolectar secretos innecesarios.

### exploiting-sql-injection-vulnerabilities

- Enumerar parámetros y contexto de base antes de probar inyección.
- Comparar respuestas y errores para confirmar un indicio antes de automatizar.
- Acotar carga y profundidad de sqlmap al alcance aprobado.
- Verificar consultas parametrizadas y casos de segundo orden.
- Usar evidencia mínima y datos de prueba; no extraer información real más allá de lo autorizado.

### exploiting-websocket-vulnerabilities

- Inspeccionar handshake, mensajes y suscripciones en cuentas de prueba.
- Comprobar autenticación de conexión y autorización de cada mensaje.
- Evaluar Origin y secuestro cross-site con un escenario reproducible.
- Probar validación de mensajes e identificadores de canal sin ampliar el alcance.
- Documentar contexto de sesión, secuencia de frames e impacto observado.

### find-skills

- Revisar catálogo y skills ya instaladas antes de recomendar duplicados.
- Buscar términos concretos y examinar el SKILL.md original de las candidatas.
- Contrastar adopción y reputación de fuente sin inventar métricas.
- Presentar utilidad, fuente, limitaciones y comando acorde al destino autorizado.
- Instalar solo las elecciones explícitas; en este proyecto conservar alcance local.

### google-docs

- Resolver documento, pestaña y estructura actuales antes de editar.
- Usar copia nativa para plantillas y cambios acotados para documentos existentes.
- Calcular rangos en unidades UTF-16 y atender desplazamientos secuenciales del batch.
- Usar revisión requerida cuando deba rechazarse una edición concurrente.
- Verificar readback y citar únicamente URLs nativas observadas del resultado.

### google-drive

- Resolver archivo por ID/URL o búsqueda antes de cambiar su ciclo de vida.
- Derivar edición de contenido a la skill Docs, Sheets o Slides correspondiente.
- Preservar organización, tipo y permisos salvo cambio explícitamente pedido.
- Usar exportación para archivos nativos y descarga autenticada para bytes almacenados.
- Verificar creación/importación antes de entregar una URL; no inventar IDs ni enlaces.

### google-drive-comments

- Leer archivo y superficie exactos antes de formular comentarios.
- Adjuntar cita textual, rango A1 o número de slide según corresponda.
- Repetir evidencia suficiente en el cuerpo; no confiar en anchors invisibles.
- Usar IDs reales para responder/resolver y agrupar operaciones compatibles.
- Verificar cantidades y resultados; omitir comentarios cuyo objetivo no pueda localizarse.

### google-sheets

- Leer metadatos y rangos pertinentes y cargar referencias de edición antes de escribir.
- Copiar nativamente una plantilla Google Sheets para preservar estructura y formato.
- Mantener fórmulas, tipos de celda, validaciones y rangos concretos.
- Aplicar batches acotados y verificar valores/fórmulas tras cambios.
- Citar el archivo final verificado; no sustituir datos ausentes por cifras inventadas.

### google-slides

- Leer el deck nativo y derivar estilos de la referencia antes de editar.
- Preservar estructura y actualizar contenido retenido que ya no corresponda.
- Cargar referencias de authoring/importación para el flujo seleccionado.
- Verificar geometría, imágenes, tablas y gráficos mediante rendering/readback.
- Entregar solo el enlace del deck final observado y registrar pérdida de fidelidad si persiste.

### hardening-docker-containers-for-production

- Separar endurecimiento de imagen/runtime de la configuración del daemon.
- Construir imágenes mínimas multietapa con usuario sin privilegios.
- Reducir capacidades, montar filesystem de solo lectura y limitar recursos según necesidad.
- Aplicar perfiles seccomp/AppArmor compatibles y evitar montajes sensibles innecesarios.
- Comprobar imagen y runtime con herramientas apropiadas antes de afirmar cumplimiento.

### hardening-docker-daemon-configuration

- Respaldar y revisar daemon.json antes de modificar el host.
- Restringir socket y proteger API remota con autenticación TLS.
- Revisar userns-remap, no-new-privileges y comunicación entre contenedores.
- Configurar rotación de logs y continuidad del daemon según requisitos.
- Validar configuración y compatibilidad antes de reiniciar; distinguir controles del host de los de imagen.

### hardening-linux-endpoint-with-cis-benchmark

- Seleccionar distribución y perfil CIS correctos antes de remediar.
- Evaluar configuración con herramientas de auditoría y conservar evidencia inicial.
- Revisar cambios de servicios, PAM, sysctl y permisos en entorno de prueba.
- Probar SSH desde una sesión adicional y permitir acceso antes de activar firewall.
- Verificar después de aplicar; no equiparar presencia de ajustes con cumplimiento completo.

### imagegen

- Usar image_gen para raster y ediciones cuando esté disponible.
- Inspeccionar referencias locales y adjuntar las imágenes necesarias al editar.
- Conservar invariantes solicitadas y transparencia existente salvo cambio pedido.
- Usar CLI solo por elección explícita del usuario y no pedir secretos en chat.
- Inspeccionar resultado y devolver artefacto real; no simular éxito ni fabricar la salida del generador.

### implementing-aes-encryption-for-data-at-rest

- Usar cifrado autenticado y verificar etiqueta antes de aceptar plaintext.
- Derivar claves desde contraseñas con KDF adecuado, nunca usar la contraseña directamente.
- Generar nonce GCM de 12 bytes y no repetirlo con la misma clave.
- Guardar nonce y metadatos necesarios junto al ciphertext sin exponer la clave.
- Probar descifrado y rechazo de alteraciones; revisar diseño antes de cifrar datos reales.

### implementing-api-rate-limiting-and-throttling

- Definir alcance por usuario/IP/endpoint y elegir algoritmo con sus límites de ráfaga.
- Mantener contadores atómicos y estado compartido solo si existen varias instancias.
- Devolver 429 con Retry-After y metadatos de cuota coherentes.
- No confiar en X-Forwarded-For sin proxy verificado.
- Probar límites y concurrencia; no adoptar cuotas ni Redis por ejemplos de la skill.

### implementing-api-schema-validation-security

- Definir contratos de entrada/salida explícitos y rechazar propiedades no permitidas.
- Validar tipos, límites, enums y campos de solo lectura en backend.
- Probar solicitudes inválidas y errores contra el contrato acordado.
- Validar respuestas para evitar filtrado accidental de propiedades internas.
- No tratar schema validation como sustituto de autorización, escape contextual o consultas parametrizadas.

### implementing-digital-signatures-with-ed25519

- Generar pares de claves con biblioteca criptográfica apropiada.
- Firmar el mensaje completo usando el modo Ed25519 seleccionado de forma explícita.
- Verificar firma y clave esperadas y rechazar mensaje alterado o clave incorrecta.
- Proteger claves privadas y separar identidad del firmante de bytes de clave no confiables.
- Probar archivos y formatos de intercambio sin incorporar cifras o garantías no verificadas.

### implementing-jwt-signing-and-verification

- Permitir explícitamente algoritmos y no aceptar alg=none.
- Validar firma, emisor, audiencia y restricciones temporales requeridas.
- Separar tipos de clave para impedir confusión HMAC/asimétrica.
- Definir rotación y revocación y proteger material privado.
- No introducir JWT al proyecto por esta skill; cualquier código de acceso requiere aprobación CRITICAL.

### implementing-memory-protection-with-dep-aslr

- Inventariar mitigaciones por sistema y proceso antes de cambiarlas.
- Comprobar compatibilidad de DEP y ASLR en aplicaciones de prueba.
- No afirmar CFG efectivo si el binario no fue compilado con soporte.
- Aplicar excepciones acotadas y políticas GPO/Intune solo cuando estén autorizadas.
- Verificar estado efectivo y fallos de compatibilidad después del cambio.

### implementing-secret-scanning-with-gitleaks

- Escanear historial y distinguir secretos activos, inactivos y falsos positivos.
- Tratar baseline como lista revisada, no aceptación automática de secretos antiguos.
- Rotar credenciales expuestas con coordinación del servicio afectado.
- Configurar hooks/CI y reglas propias según repositorio y alcance.
- No reescribir historial compartido ni eliminar evidencia sin coordinación explícita.

### implementing-semgrep-for-custom-sast-rules

- Escribir reglas YAML con lenguaje, patrón y mensaje adecuados.
- Usar taint mode para flujos source/sink cuando corresponda.
- Probar positivos con ruleid y negativos con ok para medir falsos positivos.
- Incluir CWE/OWASP y corrección sugerida cuando sea válida.
- Integrar severidades y exclusiones revisadas; no silenciar clases enteras sin evidencia.

### integrating-dast-with-owasp-zap-in-pipeline

- Escanear una aplicación de prueba accesible y autorizada.
- Distinguir baseline pasivo de full scan activo y controlar su carga.
- Usar contrato OpenAPI real para cobertura de API cuando exista.
- Definir FAIL/WARN y excepciones revisadas por hallazgo.
- Guardar reportes y respetar exit codes; una ejecución limpia no demuestra cobertura total.

### integrating-sast-into-github-actions-pipeline

- Seleccionar analizadores compatibles con lenguajes y licencias del repositorio.
- Ejecutar análisis en PR y conservar reportes SARIF donde corresponda.
- Respetar exit codes y definir severidades que bloquean según política.
- Revisar duplicados y falsos positivos sin ocultar clases de vulnerabilidad.
- Versionar configuración y permisos mínimos de workflows; no publicar resultados sin autorización.

### kb-creator

- Detectar ingesta de docs existentes frente a creación interactiva.
- Reutilizar Discovery y decisiones aprobadas sin rellenar incógnitas como hechos.
- Crear diez documentos canónicos y README en knowledge-base/.
- Conservar propuestas, supuestos y preguntas con estado explícito.
- Escribir solo state.kb al integrar; no alterar step ni otras fases.

### maintain-space

- Confirmar alcance de mantenimiento y leer evidencia y páginas existentes.
- Reconciliar hechos en lugar de acumular duplicados o notas contradictorias.
- Cambiar estados solo contra criterios de cierre y resolver evidencia conflictiva.
- Preservar layout, contenido rico y audiencia al actualizar.
- Verificar escrituras reales y no crear automatizaciones futuras salvo solicitud.

### manage-schedules

- Leer propósito de la página y schedules existentes antes de proponer recurrencias.
- Distinguir contexto ya presente de información que debe buscarse periódicamente.
- Escribir instrucciones precisas y destino correcto para cada schedule.
- Crear/editar/eliminar solo automatizaciones solicitadas con frecuencia y zona definidas.
- Verificar schedule asociado a la página y resumir lo efectivamente configurado.

### mapping-attack-paths-with-bloodhound-ce

- Delimitar dominios/tenants antes de recoger información.
- Separar datasets SharpHound y AzureHound según entorno autorizado.
- Verificar ingestión y marcar principales comprometidos con evidencia.
- Corroborar caminos mediante consultas y contexto de ACL/ADCS/híbrido.
- Priorizar caminos y remediaciones; no ejecutar abuso por aparecer una arista.

### openai-docs

- Identificar producto y superficie antes de afirmar disponibilidad.
- Revisar herramientas y configuración reales para preguntas sobre esta sesión.
- Leer self-knowledge local para orientación general de configuración de Codex.
- Contrastar comportamiento y cambios con documentación/changelog oficiales actuales.
- Preservar el objetivo del usuario en migraciones y declarar incertidumbres del cliente.

### organize-space

- Leer árbol y propósito antes de mover o reestructurar páginas.
- Mantener enlaces, contenido rico y títulos que expresen función.
- Mostrar efectos de acceso de previews de traslado que requieran confirmación.
- Reutilizar tokens y referencias de operación exactos al confirmar/recuperar.
- Distinguir pending de completed y verificar destino; no reconstruir una operación incierta a ciegas.

### performing-api-security-testing-with-postman

- Crear entornos y cuentas de prueba para cada contexto de acceso.
- Parametrizar autenticación sin incrustar tokens en colecciones.
- Cubrir BOLA, funciones, exposición, asignación masiva y límites según API.
- Ejecutar Newman respetando exit codes y conservar evidencia de fallos.
- Mantener colección acorde a endpoints reales y evitar producción salvo alcance específico.

### performing-binary-exploitation-analysis

- Identificar arquitectura y mitigaciones con checksec y análisis manual.
- Calcular offsets con patrones cíclicos en laboratorio.
- Corroborar gadgets y direcciones con depuración del binario/libc exactos.
- Separar escenarios NX, PIE y ASLR y no asumir direcciones fijas.
- Conservar reproducción mínima y alcance; una prueba local no acredita explotación remota.

### performing-cloud-forensics-with-aws-cloudtrail

- Definir cuentas, credenciales y ventana temporal del incidente.
- Consultar CloudTrail vía LookupEvents o Athena según cobertura y volumen.
- Filtrar por API, usuario, origen y agente conservando eventos originales.
- Ordenar línea temporal y revisar acceso, IAM y persistencia.
- Documentar hallazgos y límites de retención sin modificar recursos por inferencia.

### performing-csrf-attack-simulation

- Inventariar operaciones de escritura y su contexto autenticado.
- Analizar token anti-CSRF y vinculación a sesión, no solo su presencia.
- Probar SameSite, Origin/Referer y content-type en navegador de prueba.
- Exigir cambio de estado observable para confirmar un PoC.
- Limitar cuentas y operaciones al escenario autorizado y conservar solicitudes/respuestas.

### performing-directory-traversal-testing

- Localizar parámetros que resuelven rutas y establecer archivos de prueba.
- Diferenciar traversal de LFI/RFI y documentar ruta/base usada.
- Evaluar normalización y codificación con pruebas acotadas.
- Controlar fuzzing y evitar extracción de archivos ajenos al alcance.
- Informar solicitud, evidencia de lectura y mitigación; no inferir RCE de una lectura.

### performing-dns-enumeration-and-zone-transfer

- Consultar tipos específicos; ANY no es inventario completo.
- Distinguir NXDOMAIN, NODATA, timeout y respuestas incompletas.
- Realizar AXFR solo contra zona/servidor autorizados y conservar errores.
- No equiparar DNSKEY/RRSIG o AD con validación criptográfica propia.
- Contextualizar wildcard, TTL y SPF/DKIM/DMARC; no declarar vulnerabilidad por mera presencia o ausencia aparente.

### performing-fuzzing-with-aflplusplus

- Instrumentar el binario y comprobar que admite el formato de entrada.
- Preparar y minimizar corpus válido antes de lanzar campañas.
- Ejecutar AFL++ con recursos y duración acotados.
- Minimizar crashes y reproducirlos bajo depurador/CASR.
- Reportar fallos únicos con entrada y comandos reproducibles, no solo contador bruto.

### performing-linux-log-forensics-investigation

- Trabajar con copias de logs y conservar errores de parsing.
- Normalizar instantes a UTC con año/zona explícitos cuando faltan.
- Distinguir orden textual de orden temporal y documentar rollover inferido.
- Separar recuentos acumulados de detección con ventana temporal.
- No tratar JSON journal exportado como adquisición o lectura de journals binarios.

### performing-memory-forensics-with-volatility3

- Preservar dump y hashes antes del análisis.
- Identificar sistema y símbolos compatibles con la versión exacta.
- Correlacionar procesos, módulos, conexiones y registros de varios plugins.
- Extraer artefactos sensibles solo dentro del alcance y proteger resultados.
- Distinguir artefactos ausentes de cobertura incompleta y documentar reproducción.

### performing-network-traffic-analysis-with-zeek

- Definir interfaz y redes internas según segmento autorizado.
- Mantener formatos estructurados y correlacionar logs mediante uid/community-id cuando aplique.
- Validar scripts propios e indicadores antes de activar detección.
- Rotar/retener logs y monitorizar capture_loss para cuantificar cobertura.
- Ajustar señales a una línea base y verificar forwarding por evidencia real.

### performing-privilege-escalation-on-linux

- Enumerar identidad, permisos y servicios antes de elegir un vector.
- Revisar SUID/SGID, sudo, capabilities y tareas con archivos modificables.
- Comparar versión y configuración con requisitos reales del vector.
- Probar solo en laboratorio/host y cuenta autorizados.
- Registrar evidencia y remediación; no ejecutar exploits ni modificar el host por inventario.

### performing-security-headers-audit

- Conservar URL, errores y respuestas originales; no ignorar fallos de cobertura.
- Interpretar HSTS solo en HTTPS y revisar alcance de subdominios.
- Separar CSP de Report-Only y políticas de script de estilo.
- Verificar valores de XFO/nosniff y atributos por cookie, no mera presencia.
- No convertir faltas de cabeceras en prueba de XSS/CSRF/CORS explotable.

### performing-sqlite-database-forensics

- Preservar copia coherente de base y WAL/SHM cuando existan.
- Revisar cabecera, tamaño de página y tipos B-tree antes de recuperar.
- Separar registros activos de freelist, WAL y espacio no asignado.
- Decodificar timestamps según aplicación/origen, no con una fórmula única.
- Corroborar registros recuperados y reportar límites sin alterar la evidencia original.

### performing-static-malware-analysis-with-pe-studio

- Calcular hashes y verificar integridad de la muestra sin ejecutarla.
- Examinar cabeceras PE, secciones, imports y recursos.
- Extraer strings ASCII/Unicode y revisar overlay.
- Identificar empaquetado/entropía sin tomar pocos imports como benignidad.
- Corroborar timestamps e indicadores y conservar reporte de análisis estático.

### performing-threat-modeling-with-owasp-threat-dragon

- Definir activos, actores, límites y objetivos del modelo.
- Dibujar flujos de datos y fronteras de confianza verificables.
- Aplicar STRIDE/LINDDUN según objetivo sin aceptar amenazas automáticas sin revisión.
- Asignar mitigaciones, severidad y responsable y priorizar por riesgo.
- Mantener modelo versionado y revisar al cambiar arquitectura; conservar reporte de decisiones.

### performing-yara-rule-development-for-detection

- Elegir cadenas/patrones discriminantes de muestras representativas.
- Combinar condiciones que reduzcan coincidencias sobre datos genéricos.
- Probar corpus positivo y negativo con YARA/YARA-X compatible.
- Medir rendimiento y evitar patrones costosos sin necesidad.
- Registrar metadatos y hashes y corroborar resistencia a variantes antes de desplegar.

### pets

- Resolver un pet.id inequívoco y paginar lista cuando sea necesario.
- Distinguir pet activo de coincidencia de nombre; los nombres no son únicos.
- Verificar active_pet_id tras seleccionar o apagar.
- Tratar URLs de descarga como efímeras y conservar ID estable.
- Borrar solo custom pet pedido explícitamente; no confundir ocultar/reemplazar con eliminar.

### plugin-management

- Preferir herramienta incorporada o plugin conectado que ya resuelva la tarea.
- Verificar capacidades y dependencias antes de proponer otro plugin.
- Cambiar permisos o desinstalar solo por solicitud explícita.
- Respetar las condiciones vigentes del tool para solicitar instalación.
- Verificar resultado y no afirmar conexión/disponibilidad por archivos en caché.

### reverse-engineering-malware-with-ghidra

- Importar binario con arquitectura correcta y ejecutar autoanálisis.
- Localizar entry points y funciones relevantes con referencias cruzadas.
- Distinguir código de biblioteca y resolver llamadas indirectas.
- Corroborar pseudocódigo, criptografía y protocolo con bytes/ensamblador.
- Guardar proyecto y documentar nombres, direcciones e indicadores sin ejecutar la muestra.

### review-agent

- Leer instrucciones aplicables, diff completo y contexto antes de evaluar.
- Mantener revisión de solo lectura: sin editar, commits, push, comentarios externos ni delegación.
- Reportar solo defectos concretos introducidos por el cambio.
- Asignar P0/P1/P2/P3 según impacto y aportar ubicación y condición de fallo.
- Decir No findings cuando no existan hallazgos que cumplan el criterio.

### roadmap-generator

- Verificar diez canónicos y OpenSpec antes de escribir CHANGES.md en raíz.
- Usar IDs C-NN y nombres kebab-case con scope operativo y referencias reales.
- Ordenar dependencias de infraestructura, modelos, acceso y consumidores sin ciclos.
- Mantener gates, plan de agentes y camino de precedencias coherentes con el grafo.
- Actualizar solo state.roadmap y conservar aprobaciones/pendientes y alcance local del proyecto.

### scanning-network-with-nmap-advanced

- Interpretar XML y estados sin confundir open|filtered o down con certezas.
- Revisar alcance, sondas y tiempos antes de una ejecución de red.
- Elegir NSE por nombre individual revisado; safe/vuln no son permisos.
- Pedir -sV explícito si se requiere versionado y distinguirlo de -sU.
- No inferir CVE por banner ni afirmar invisibilidad con decoys/fragmentación.

### securing-aws-iam-permissions

- Inventariar identidades, políticas y uso real antes de reducir permisos.
- Usar recursos/condiciones específicos y revisar Access Analyzer.
- Aplicar boundaries y MFA según riesgo sin suponer que una boundary concede acceso.
- Preferir roles y credenciales temporales cuando el entorno lo permita.
- Coordinar rotación e investigar CloudTrail antes de eliminar claves o evidencia.

### sites

- Abrir fuente correcta y preservar identidad/audiencia del sitio existente.
- En trabajo local usar checkout y omitir registro, sincronización y publicación.
- Mantener el framework pedido por el usuario frente al starter predeterminado.
- Validar build, navegador y paquete antes de hosting.
- Publicar solo dentro del alcance autorizado y verificar URL/estado; no activar Sites por catalogarlo.

### sites-remix

- Resolver fuente inequívoca y crear remix de su versión publicada.
- Conservar nueva identidad y reconciliar creación incierta antes de reintentar.
- Distinguir código copiado de datos/configuración/runtime no copiados.
- Recrear solo recursos requeridos por funciones retenidas y verificar esquema sin inventarlo.
- Mantener destino sin publicar hasta que el alcance lo autorice y comprobar resultado final.

### skill-creator

- Precisar objetivo, disparadores, entradas/salidas y límites de la skill.
- Escribir instrucciones concisas y referencias con rutas correctas y recursos reutilizables.
- Crear prompts de evaluación representativos y baseline para comparar.
- Ejecutar pruebas/evaluación aisladas conforme a la autorización del entorno.
- Revisar resultados con el usuario y empaquetar solo artefactos necesarios.

### skill-installer

- Resolver fuente, referencia y path exactos antes de copiar.
- Usar helpers existentes y abortar ante destino existente sin sobrescritura tácita.
- Indicar --dest explícito en instalaciones autorizadas solo para proyecto.
- Conservar recursos de la skill y verificar SKILL.md y archivos instalados.
- Informar disponibilidad en el próximo turno; no instalar dependencias adicionales por inferencia.

### supabase-postgres-best-practices

- Cargar referencias pertinentes antes de DDL, migraciones o consultas PostgreSQL.
- Priorizar rendimiento de consultas/conexiones y comprobar planes con evidencia.
- Indexar filtros y joins pertinentes sin agregar índices por plantilla.
- Mantener transacciones cortas y evitar llamadas externas mientras se retienen locks.
- Aplicar solo PostgreSQL local y UUID acordados; no adoptar Supabase, RLS, bigint o UUIDv7 por defaults.

### testing-cors-misconfiguration

- Inspeccionar ACAO/ACAC por endpoint y contexto autenticado.
- Variar Origin y preflight para probar política real, no solo cabeceras.
- Corroborar lectura cross-origin desde navegador y distinguirla de respuesta curl.
- Revisar origen null/reflejado y validaciones de subdominios con cuentas de prueba.
- Documentar datos accesibles e impacto mínimo dentro del alcance autorizado.

### testing-for-broken-access-control

- Mapear endpoints, actores y permisos esperados en una matriz.
- Comparar peticiones directas con sesiones/cuentas distintas.
- Probar acceso horizontal, vertical y de función sobre recursos de prueba.
- Revisar aislamiento entre organizaciones solo si existe ese alcance.
- Conservar evidencia de lectura/escritura indebida; ocultar controles UI no prueba autorización backend.

### testing-for-business-logic-vulnerabilities

- Derivar invariantes de requisitos y reconstruir el flujo completo.
- Probar omisión/reordenamiento de pasos y manipulación de parámetros.
- Verificar condiciones de carrera con solicitudes realmente concurrentes.
- Comparar estado persistido y resultado económico/funcional con la regla esperada.
- Limitar cuentas/datos y documentar reproducción sin ampliar funciones del producto.

### testing-for-xss-vulnerabilities

- Mapear entradas y contexto de salida antes de elegir la prueba.
- Distinguir XSS reflejado, almacenado y DOM y recorrer roles afectados.
- Corroborar ejecución en navegador de prueba; una cadena reflejada no basta.
- Revisar escape contextual, sanitización y CSP sin asumir que una cabecera resuelve todo.
- Usar PoC mínimo y no recolectar sesiones o datos reales fuera del alcance.

### testing-for-xxe-injection-vulnerabilities

- Identificar puntos reales de parsing XML y tipos de archivo procesados.
- Diferenciar resolución de entidad local y detección ciega con evidencia controlada.
- Usar archivos/servidores de prueba autorizados para demostrar acceso.
- Acotar requests y no extraer datos sensibles por un ejemplo de la skill.
- Documentar parser/configuración observados y corregir carga de entidades externas tras revisión.

### testing-jwt-token-security

- Separar decodificación de verificación criptográfica y validar estructura.
- Probar rechazo de none y confusión de algoritmos con tokens de prueba.
- Revisar iss, aud, exp, nbf y permisos sin aceptar claims por mera presencia.
- Verificar rotación, revocación y vida útil contra política real.
- Mantener fuerza bruta y manipulación dentro del alcance; no elegir JWT para el MVP por este catálogo.

### testing-oauth2-implementation-flaws

- Reconstruir autorización y canje de código como operaciones separadas.
- Probar coincidencia redirect_uri y estado vinculado al cliente/sesión.
- Verificar PKCE efectivo en emisión y canje, no solo soporte declarado.
- Revisar scopes, expiración y almacenamiento de tokens y fugas por redirección.
- Demostrar resultados con cuentas de prueba y conservar flujo reproducible autorizado.

### triaging-security-incident

- Recoger alerta, fuentes y ventana temporal antes de clasificar.
- Correlacionar IOCs, activo afectado y contexto de negocio.
- Asignar severidad por impacto y documentar incertidumbre.
- Preservar evidencia volátil antes de remediar cuando sea pertinente.
- Escalar con timeline, hallazgos y acciones realizadas; no declarar contención no verificada.

### update-pet

- Resolver un custom pet.id inequívoco y conservarlo durante toda la edición.
- No llamar create_pet para reparar o actualizar un pet existente.
- Separar actualización de metadatos de cambios de sprite sheet.
- Validar bytes/previews y seguir contrato de upload para cambios de arte.
- Verificar metadata e identidad con readback y conservar selección activa salvo solicitud.

### vercel-react-best-practices

- Aplicar reglas de React cliente compatibles con Vite; omitir RSC/Next.js no presentes.
- Ejecutar trabajo asíncrono independiente en paralelo y diferir awaits innecesarios.
- Evitar estado derivado por efectos y suscripciones más amplias de lo necesario.
- Usar dependencias estables y actualizaciones funcionales; no memoizar expresiones triviales.
- Reducir bundle e imports innecesarios sin añadir SWR o librerías por defecto.

### visualize

- Crear visual inline solo si ayuda materialmente a explorar o explicar en conversación.
- Escribir fragmento autocontenido conforme al contrato de archivo de la skill.
- Mantener presentación/interacciones locales y controles semánticos con etiquetas.
- Respetar estilos de runtime y no cargar librerías de iconos desde la red.
- Para entregables científicos/exportables usar artefacto adecuado; no reemplazar implementación del proyecto con un mockup inline.

### web-scraper

- Validar una URL concreta y usar fuentes públicas sin crawling masivo.
- Mantener esquema fijo de ocho campos en discovery/sources/.
- Registrar fuente, fecha y distinción entre evidencia y declaración comercial.
- Marcar datos no publicados sin inventar precio, usuarios o funciones.
- Informar bloqueo/JS/anti-bot y no afirmar pruebas de producto por leer marketing.

### webapp-testing

- Ejecutar helpers con --help antes de usarlos y tratarlos como caja negra.
- Inspeccionar DOM renderizado y esperar estado listo antes de elegir selectores.
- Usar controles/roles descriptivos y capturar errores y screenshots relevantes.
- Gestionar ciclo de servidores y cerrar navegador al terminar.
- Verificar flujos locales reales; no sustituir concurrencia PostgreSQL o recepción SMTP por comprobación UI.

### write-page

- Resolver destino y leer referencias de edición antes de modificar una Page.
- Preservar estructura y contenido rico mediante cambios acotados.
- Usar solo formatos soportados por el catálogo de Page content.
- No guardar contenido en un destino distinto por inferir permiso de la selección.
- Verificar resultado y respetar confirmaciones exigidas por la herramienta sin pedirlas otra vez si ya están autorizadas.

## Project Conventions

No se encontró índice de convenciones en la raíz: `AGENTS.md`, `agents.md`, `CLAUDE.md`, `.cursorrules`, `GEMINI.md` y `copilot-instructions.md` están ausentes. El `AGENTS.md` interno de la skill de Vercel es una referencia de esa skill, no un archivo de convenciones de la raíz del proyecto. La generación de instrucciones corresponde a la fase siguiente; este registro no la ejecuta.

Los documentos existentes de apoyo siguientes no se presentan como índices de convenciones encontrados:

| File | Path | Notes |
|---|---|---|
| `CHANGES.md` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/CHANGES.md` | Índice de 16 changes; alcance local, dependencias y gobernanza. |
| `knowledge-base/README.md` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/knowledge-base/README.md` | Estados de revisión y precedencia; 01–07 aprobados, 08–10 con propuestas pendientes. |
| `knowledge-base/10_preguntas_abiertas.md` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/knowledge-base/10_preguntas_abiertas.md` | Registro PB/AT/AP; AT-11 fue resuelto posteriormente por política off. |
| `design-system/README.md` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/design-system/README.md` | Decisión explícita posterior: Mode off, Approval skipped. |
| `.active-orchestrator-state.json` | `/home/fernando/Documents/Universidad/3erAÑO/Desarrollo de software/odontologia/.active-orchestrator-state.json` | Estado de fundación, política y procedencia de las tres instalaciones locales. |

Leer estos documentos para las decisiones vigentes. Las instrucciones de gobernanza y memoria recibidas en la sesión siguen aplicándose aunque aún no exista un AGENTS.md local.
