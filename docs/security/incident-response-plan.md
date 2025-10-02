# Thunderbird for Android — Incident Response Plan

This template that will help guide you through the process of handling security incidents and investigations.

There are 5 phases:
1. Validation
2. Mitigation
3. Scoping
4. Mitigation Notification
5. Remediation

Each phase should be completed before moving to the next.

---

## Guidance

- [Vulnerability Reporting Form](https://github.com/thunderbird/thunderbird-android/security/advisories/new) (see SECURITY.md)
  - Note: Vulnerability Reports include CVSS scoring calculator
- The [CIA triad](https://www.energy.gov/femp/operational-technology-cybersecurity-energy-systems#cia) is used to evaluate security risks. Every vulnerability should be assessed against these principles:
  - Confidentiality
    - Keep data private and protected from unauthorized access
    - Example: Can the attacker read or exfiltrate email content, account settings, auth tokens, or attachment cache?
  - Integrity
    - Ensure data is accurate and not tampered with
    - Example: Can the attacker modify mailbox state, filters, server settings, or message contents rendered to the user?
  - Availability
    - Keep systems and data accessible when needed
    - Example: Can crafted content crash the app, deadlock sync, or brick startup (persistent DoS via mailbox state)?

---

## Phase 1 - Validation

### Updates

_In this section, summarize the report, steps to recreate, mitigating factors, and potential impact._

_Example:_

_We received a report of a crash triggered by malformed S/MIME messages._
- _Verified on Android 15 with Thunderbird 13.0_
- _Requires custom-crafted email and user interaction_
- _See sample email link for recreation_
- _Potential Impact: Denial-of-service and possible memory corruption_

### Tasks

- [ ] Understand the vulnerability
- [ ] Update the vulnerability report with understanding
- [ ] Determine severity based on CVSS scoring calculator
- [ ] Decide if case will become an investigation. Either:
  - [ ] Dismiss report as not-actionable
  - [ ] Convert report to an investigation

### Results

- Is there a direct risk of CIA being broken? `Yes|No`
- Which part of CIA could be broken? `Confidentiality|Integrity|Availability`
- What user data is at risk?
- What is required to exploit the vulnerability?
- What is the severity? `Low|Moderate|High|Critical`
- Vulnerability was introduced on: `YYYY-MM-DD`
- Pull request where vulnerability was introduced? `<url>`
- Versions of Thunderbird affected: `<#.#>, ...`

---

## Phase 2 - Mitigation

### Updates

_In this section, note any blockers, challenges, or progress on mitigation. This phase is only necessary in circumstances where the remediation is not possible in a reasonable amount of time._

_Example:_
_We disabled the inline S/MIME rendering feature flag as a temporary mitigation. Root cause identified in MIME parsing logic._

### Tasks

- [ ] Re-assess severity and update if necessary
- [ ] Assess whether vulnerability also exists in other code paths
- [ ] Update vulnerability report with mitigation

### Results

- The vulnerability required mitigation on: `Nightly|Beta|Release`
- The vulnerability mitigated on: `YYYY-MM-DD`
- The vulnerability mitigated on the following releases: `<#.#>, ...`
- Link to mitigation work: `<url>`

---

## Phase 3 - Scoping

### Updates

_In this section describe the scoping results. Interrogate data to determine if the vulnerability was exploited, and what the impact was._

_Example:_
_Crash telemetry indicates 1,200 users impacted on version 13. No evidence of public exploit in use._

### Tasks

- [ ] Review available information sources (crash reports, GitHub issues, vulnerability reports, social networks, blogs, etc)
- [ ] Determine if there was a confirmed breach in CIA
- [ ] Confirm who was affected or might have been affected

### Results

- Scoping analysis link: `<url>`
- Confidence in scoping completeness: `low|medium|high`
- Was there a CIA breach? `Yes|No`
  - If yes, elaborate:
- How many users were affected: `<#>`
- All needed data available? `Yes|No`
  - If no, elaborate:

---

## Phase 4 - Mitigation Notification

### Updates

_In this section describe the mitigation notification plans. This phase is only necessary in circumstances where the remediation is not possible in a reasonable amount of time._

_Example:_
_We will notify users with our findings on this vulnerability, provide instruction on what version contains the mitigation, and how to audit their device for exploitation._

_We will notify via:_
- _Release notes_
- _Thunderbird blog post (if high severity)_

### Tasks

- [ ] Draft notification content
- [ ] Internal FAQ + Support/Comms alert
- [ ] Update GitHub and Play Store release notes
- [ ] Optional blog post

### Results

- Notifications were sent/published on: `YYYY-MM-DD:HH-MM-SSZ`
- Link to notification content? `<url>`
- Is there a link to a blog/changelog that was published? `<url>`

---

## Phase 5 - Remediation

### Updates

_In this section describe the remediation plans._

_Example:_
_We are providing a patch to the MIME parsing logic to fix the vulnerability. We will again notify users with our findings on this vulnerability, provide instruction on what version contains the mitigation, and how to audit their device for exploitation._

_We will notify via:_
- _CVE_
- _Thunderbird for Android Security Advisory_
- _Release notes_
- _Thunderbird blog post (if high severity)_

### Tasks

- [ ] Request CVE assignment. Reach out to Mozilla Security Team Members Tom Ritter and Dan Veditz with security@mozilla.org as the back-up.
- [ ] Publish advisory to [Thunderbird for Android Security Advisories](https://github.com/thunderbird/thunderbird-android/security/advisories)
- [ ] Update release notes
- [ ] Optional blog post

### Results

- CVE and advisory were published on: `YYYY-MM-DD:HH-MM-SSZ`
- Link to notification content? `<url>`
- Is there a link to a blog/changelog that was published? `<url>`

