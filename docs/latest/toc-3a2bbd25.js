// Populate the sidebar
//
// This is a script, and not included directly in the page, to control the total size of the book.
// The TOC contains an entry for each page, so if each page includes a copy of the TOC,
// the total size of the page becomes O(n**2).
class MDBookSidebarScrollbox extends HTMLElement {
    constructor() {
        super();
    }
    connectedCallback() {
        this.innerHTML = '<ol class="chapter"><li class="chapter-item expanded "><span class="chapter-link-wrapper"><a href="about.html">About Thunderbird for Android</a></span></li><li class="chapter-item expanded "><li class="spacer"></li></li><li class="chapter-item expanded "><span class="chapter-link-wrapper"><a href="CONTRIBUTING.html"><strong aria-hidden="true">1.</strong> Contributing</a><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/development-environment.html"><strong aria-hidden="true">1.1.</strong> Development Environment</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/contribution-workflow.html"><strong aria-hidden="true">1.2.</strong> Contribution Workflow</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/development-guide.html"><strong aria-hidden="true">1.3.</strong> Development Guide</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/code-quality-guide.html"><strong aria-hidden="true">1.4.</strong> Code Quality Guide</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/code-review-guide.html"><strong aria-hidden="true">1.5.</strong> Code Review Guide</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/git-commit-guide.html"><strong aria-hidden="true">1.6.</strong> Git Commit Guide</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/testing-guide.html"><strong aria-hidden="true">1.7.</strong> Testing Guide</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/translations.html"><strong aria-hidden="true">1.8.</strong> Translations</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/managing-strings.html"><strong aria-hidden="true">1.9.</strong> Managing Strings</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="contributing/java-to-kotlin-conversion-guide.html"><strong aria-hidden="true">1.10.</strong> Java to Kotlin Conversion Guide</a></span></li></ol><li class="chapter-item expanded "><span class="chapter-link-wrapper"><a href="architecture/index.html"><strong aria-hidden="true">2.</strong> Architecture</a><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/module-organization.html"><strong aria-hidden="true">2.1.</strong> Module Organization</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/module-structure.html"><strong aria-hidden="true">2.2.</strong> Module Structure</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/feature-modules.html"><strong aria-hidden="true">2.3.</strong> Feature Modules</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/ui-architecture.html"><strong aria-hidden="true">2.4.</strong> UI Architecture</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/theme-system.html"><strong aria-hidden="true">2.5.</strong> Theme System</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/design-system.html"><strong aria-hidden="true">2.6.</strong> Design System</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/user-flows.html"><strong aria-hidden="true">2.7.</strong> User Flows</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/legacy-module-integration.html"><strong aria-hidden="true">2.8.</strong> Legacy Module Integration</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/index.html"><strong aria-hidden="true">2.9.</strong> Architecture Decision Records</a><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">2.9.1.</strong> Accepted</span><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0001-switch-from-java-to-kotlin.html"><strong aria-hidden="true">2.9.1.1.</strong> 0001 - Switch From Java to Kotlin</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0002-ui-wrap-material-components-in-atomic-design-system.html"><strong aria-hidden="true">2.9.1.2.</strong> 0002 - UI - Wrap Material Components in Atomic Design System</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0003-switch-test-assertions-from-truth-to-assertk.html"><strong aria-hidden="true">2.9.1.3.</strong> 0003 - Test - Switch Test Assertions From Truth to Assertk</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0004-naming-conventions-for-interfaces-and-their-implementations.html"><strong aria-hidden="true">2.9.1.4.</strong> 0004 - Naming Conventions for Interfaces and Their Implementations</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0005-central-project-configuration.html"><strong aria-hidden="true">2.9.1.5.</strong> 0005 - Central Project Configuration</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0006-white-label-architecture.html"><strong aria-hidden="true">2.9.1.6.</strong> 0006 - White Label Architecture</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0007-project-structure.html"><strong aria-hidden="true">2.9.1.7.</strong> 0007 - Project Structure</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="architecture/adr/0008-change-shared-modules-package-name.html"><strong aria-hidden="true">2.9.1.8.</strong> 0008 - Change Shared Module package to net.thunderbird</a></span></li></ol><li class="chapter-item "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">2.9.2.</strong> Proposed</span></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">2.9.3.</strong> Rejected</span></span></li></ol></li></ol><li class="chapter-item expanded "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">3.</strong> User Guide</span><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">3.1.</strong> Setup</span><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="user-guide/setup/installing-adb.html"><strong aria-hidden="true">3.1.1.</strong> Installing ADB</a></span></li></ol><li class="chapter-item "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">3.2.</strong> Troubleshooting</span><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="user-guide/troubleshooting/collecting-debug-logs.html"><strong aria-hidden="true">3.2.1.</strong> Collecting Debug Logs</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="user-guide/troubleshooting/find-your-app-version.html"><strong aria-hidden="true">3.2.2.</strong> Find your app version</a></span></li></ol></li></ol><li class="chapter-item expanded "><span class="chapter-link-wrapper"><a href="developer/index.html"><strong aria-hidden="true">4.</strong> Developer</a><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="developer/db-migration-checklist.html"><strong aria-hidden="true">4.1.</strong> Database Migration Checklist</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="developer/foldable-device-support.html"><strong aria-hidden="true">4.2.</strong> Foldable Device Support</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="developer/preference-migration-guide.html"><strong aria-hidden="true">4.3.</strong> Preference Migration Guide</a></span></li></ol><li class="chapter-item expanded "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">5.</strong> Release</span><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="release/RELEASE.html"><strong aria-hidden="true">5.1.</strong> Release Process</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="release/AUTOMATION.html"><strong aria-hidden="true">5.2.</strong> Release Automation</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="release/developer-checklist.html"><strong aria-hidden="true">5.3.</strong> Developer Release Checklist</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="release/testing-checklist.html"><strong aria-hidden="true">5.4.</strong> Release Testing Checklist</a></span></li><li class="chapter-item "><span class="chapter-link-wrapper"><a href="release/HISTORICAL_RELEASE.html"><strong aria-hidden="true">5.5.</strong> Manual Release (historical)</a></span></li></ol><li class="chapter-item expanded "><span class="chapter-link-wrapper"><span><strong aria-hidden="true">6.</strong> Security</span><a class="chapter-fold-toggle"><div>❱</div></a></span><ol class="section"><li class="chapter-item "><span class="chapter-link-wrapper"><a href="security/threat-modeling-guide.html"><strong aria-hidden="true">6.1.</strong> Threat Modeling Guide</a></span></li></ol><li class="chapter-item expanded "><li class="spacer"></li></li><li class="chapter-item expanded "><span class="chapter-link-wrapper"><a href="HOW-TO-DOCUMENT.html">How to Document</a></span></li></ol>';
        // Set the current, active page, and reveal it if it's hidden
        let current_page = document.location.href.toString().split('#')[0].split('?')[0];
        if (current_page.endsWith('/')) {
            current_page += 'index.html';
        }
        const links = Array.prototype.slice.call(this.querySelectorAll('a'));
        const l = links.length;
        for (let i = 0; i < l; ++i) {
            const link = links[i];
            const href = link.getAttribute('href');
            if (href && !href.startsWith('#') && !/^(?:[a-z+]+:)?\/\//.test(href)) {
                link.href = path_to_root + href;
            }
            // The 'index' page is supposed to alias the first chapter in the book.
            if (link.href === current_page
                || i === 0
                && path_to_root === ''
                && current_page.endsWith('/index.html')) {
                link.classList.add('active');
                let parent = link.parentElement;
                while (parent) {
                    if (parent.tagName === 'LI' && parent.classList.contains('chapter-item')) {
                        parent.classList.add('expanded');
                    }
                    parent = parent.parentElement;
                }
            }
        }
        // Track and set sidebar scroll position
        this.addEventListener('click', e => {
            if (e.target.tagName === 'A') {
                const clientRect = e.target.getBoundingClientRect();
                const sidebarRect = this.getBoundingClientRect();
                sessionStorage.setItem('sidebar-scroll-offset', clientRect.top - sidebarRect.top);
            }
        }, { passive: true });
        const sidebarScrollOffset = sessionStorage.getItem('sidebar-scroll-offset');
        sessionStorage.removeItem('sidebar-scroll-offset');
        if (sidebarScrollOffset !== null) {
            // preserve sidebar scroll position when navigating via links within sidebar
            const activeSection = this.querySelector('.active');
            if (activeSection) {
                const clientRect = activeSection.getBoundingClientRect();
                const sidebarRect = this.getBoundingClientRect();
                const currentOffset = clientRect.top - sidebarRect.top;
                this.scrollTop += currentOffset - parseFloat(sidebarScrollOffset);
            }
        } else {
            // scroll sidebar to current active section when navigating via
            // 'next/previous chapter' buttons
            const activeSection = document.querySelector('#mdbook-sidebar .active');
            if (activeSection) {
                activeSection.scrollIntoView({ block: 'center' });
            }
        }
        // Toggle buttons
        const sidebarAnchorToggles = document.querySelectorAll('.chapter-fold-toggle');
        function toggleSection(ev) {
            ev.currentTarget.parentElement.parentElement.classList.toggle('expanded');
        }
        Array.from(sidebarAnchorToggles).forEach(el => {
            el.addEventListener('click', toggleSection);
        });
    }
}
window.customElements.define('mdbook-sidebar-scrollbox', MDBookSidebarScrollbox);

