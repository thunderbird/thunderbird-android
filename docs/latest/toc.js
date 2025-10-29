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
        this.innerHTML = '<ol class="chapter"><li class="chapter-item affix "><a href="about.html">About Thunderbird for Android</a></li><li class="chapter-item affix "><li class="spacer"></li><li class="chapter-item "><a href="CONTRIBUTING.html"><strong aria-hidden="true">1.</strong> Contributing</a><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="contributing/development-environment.html"><strong aria-hidden="true">1.1.</strong> Development Environment</a></li><li class="chapter-item "><a href="contributing/contribution-workflow.html"><strong aria-hidden="true">1.2.</strong> Contribution Workflow</a></li><li class="chapter-item "><a href="contributing/development-guide.html"><strong aria-hidden="true">1.3.</strong> Development Guide</a></li><li class="chapter-item "><a href="contributing/code-quality-guide.html"><strong aria-hidden="true">1.4.</strong> Code Quality Guide</a></li><li class="chapter-item "><a href="contributing/code-review-guide.html"><strong aria-hidden="true">1.5.</strong> Code Review Guide</a></li><li class="chapter-item "><a href="contributing/git-commit-guide.html"><strong aria-hidden="true">1.6.</strong> Git Commit Guide</a></li><li class="chapter-item "><a href="contributing/testing-guide.html"><strong aria-hidden="true">1.7.</strong> Testing Guide</a></li><li class="chapter-item "><a href="contributing/translations.html"><strong aria-hidden="true">1.8.</strong> Translations</a></li><li class="chapter-item "><a href="contributing/managing-strings.html"><strong aria-hidden="true">1.9.</strong> Managing Strings</a></li><li class="chapter-item "><a href="contributing/java-to-kotlin-conversion-guide.html"><strong aria-hidden="true">1.10.</strong> Java to Kotlin Conversion Guide</a></li></ol></li><li class="chapter-item "><a href="architecture/README.html"><strong aria-hidden="true">2.</strong> Architecture</a><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="architecture/module-organization.html"><strong aria-hidden="true">2.1.</strong> Module Organization</a></li><li class="chapter-item "><a href="architecture/module-structure.html"><strong aria-hidden="true">2.2.</strong> Module Structure</a></li><li class="chapter-item "><a href="architecture/feature-modules.html"><strong aria-hidden="true">2.3.</strong> Feature Modules</a></li><li class="chapter-item "><a href="architecture/ui-architecture.html"><strong aria-hidden="true">2.4.</strong> UI Architecture</a></li><li class="chapter-item "><a href="architecture/theme-system.html"><strong aria-hidden="true">2.5.</strong> Theme System</a></li><li class="chapter-item "><a href="architecture/design-system.html"><strong aria-hidden="true">2.6.</strong> Design System</a></li><li class="chapter-item "><a href="architecture/user-flows.html"><strong aria-hidden="true">2.7.</strong> User Flows</a></li><li class="chapter-item "><a href="architecture/legacy-module-integration.html"><strong aria-hidden="true">2.8.</strong> Legacy Module Integration</a></li><li class="chapter-item "><a href="architecture/adr/README.html"><strong aria-hidden="true">2.9.</strong> Architecture Decision Records</a><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><div><strong aria-hidden="true">2.9.1.</strong> Accepted</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="architecture/adr/0001-switch-from-java-to-kotlin.html"><strong aria-hidden="true">2.9.1.1.</strong> 0001 - Switch From Java to Kotlin</a></li><li class="chapter-item "><a href="architecture/adr/0002-ui-wrap-material-components-in-atomic-design-system.html"><strong aria-hidden="true">2.9.1.2.</strong> 0002 - UI - Wrap Material Components in Atomic Design System</a></li><li class="chapter-item "><a href="architecture/adr/0003-switch-test-assertions-from-truth-to-assertk.html"><strong aria-hidden="true">2.9.1.3.</strong> 0003 - Test - Switch Test Assertions From Truth to Assertk</a></li><li class="chapter-item "><a href="architecture/adr/0004-naming-conventions-for-interfaces-and-their-implementations.html"><strong aria-hidden="true">2.9.1.4.</strong> 0004 - Naming Conventions for Interfaces and Their Implementations</a></li><li class="chapter-item "><a href="architecture/adr/0005-central-project-configuration.html"><strong aria-hidden="true">2.9.1.5.</strong> 0005 - Central Project Configuration</a></li><li class="chapter-item "><a href="architecture/adr/0006-white-label-architecture.html"><strong aria-hidden="true">2.9.1.6.</strong> 0006 - White Label Architecture</a></li><li class="chapter-item "><a href="architecture/adr/0007-project-structure.html"><strong aria-hidden="true">2.9.1.7.</strong> 0007 - Project Structure</a></li><li class="chapter-item "><a href="architecture/adr/0008-change-shared-modules-package-name.html"><strong aria-hidden="true">2.9.1.8.</strong> 0008 - Change Shared Module package to net.thunderbird</a></li></ol></li><li class="chapter-item "><div><strong aria-hidden="true">2.9.2.</strong> Proposed</div></li><li class="chapter-item "><div><strong aria-hidden="true">2.9.3.</strong> Rejected</div></li></ol></li></ol></li><li class="chapter-item "><div><strong aria-hidden="true">3.</strong> User Guide</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><div><strong aria-hidden="true">3.1.</strong> Setup</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="user-guide/setup/installing-adb.html"><strong aria-hidden="true">3.1.1.</strong> Installing ADB</a></li></ol></li><li class="chapter-item "><div><strong aria-hidden="true">3.2.</strong> Troubleshooting</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="user-guide/troubleshooting/collecting-debug-logs.html"><strong aria-hidden="true">3.2.1.</strong> Collecting Debug Logs</a></li><li class="chapter-item "><a href="user-guide/troubleshooting/find-your-app-version.html"><strong aria-hidden="true">3.2.2.</strong> Find your app version</a></li></ol></li></ol></li><li class="chapter-item "><div><strong aria-hidden="true">4.</strong> Release</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="release/RELEASE.html"><strong aria-hidden="true">4.1.</strong> Release Process</a></li><li class="chapter-item "><a href="release/AUTOMATION.html"><strong aria-hidden="true">4.2.</strong> Release Automation</a></li><li class="chapter-item "><a href="release/developer-checklist.html"><strong aria-hidden="true">4.3.</strong> Developer Release Checklist</a></li><li class="chapter-item "><a href="release/HISTORICAL_RELEASE.html"><strong aria-hidden="true">4.4.</strong> Manual Release (historical)</a></li></ol></li><li class="chapter-item "><div><strong aria-hidden="true">5.</strong> Security</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="security/threat-modeling-guide.html"><strong aria-hidden="true">5.1.</strong> Threat Modeling Guide</a></li></ol></li><li class="chapter-item "><li class="spacer"></li><li class="chapter-item affix "><a href="HOW-TO-DOCUMENT.html">How to Document</a></li></ol>';
        // Set the current, active page, and reveal it if it's hidden
        let current_page = document.location.href.toString().split("#")[0].split("?")[0];
        if (current_page.endsWith("/")) {
            current_page += "index.html";
        }
        var links = Array.prototype.slice.call(this.querySelectorAll("a"));
        var l = links.length;
        for (var i = 0; i < l; ++i) {
            var link = links[i];
            var href = link.getAttribute("href");
            if (href && !href.startsWith("#") && !/^(?:[a-z+]+:)?\/\//.test(href)) {
                link.href = path_to_root + href;
            }
            // The "index" page is supposed to alias the first chapter in the book.
            if (link.href === current_page || (i === 0 && path_to_root === "" && current_page.endsWith("/index.html"))) {
                link.classList.add("active");
                var parent = link.parentElement;
                if (parent && parent.classList.contains("chapter-item")) {
                    parent.classList.add("expanded");
                }
                while (parent) {
                    if (parent.tagName === "LI" && parent.previousElementSibling) {
                        if (parent.previousElementSibling.classList.contains("chapter-item")) {
                            parent.previousElementSibling.classList.add("expanded");
                        }
                    }
                    parent = parent.parentElement;
                }
            }
        }
        // Track and set sidebar scroll position
        this.addEventListener('click', function(e) {
            if (e.target.tagName === 'A') {
                sessionStorage.setItem('sidebar-scroll', this.scrollTop);
            }
        }, { passive: true });
        var sidebarScrollTop = sessionStorage.getItem('sidebar-scroll');
        sessionStorage.removeItem('sidebar-scroll');
        if (sidebarScrollTop) {
            // preserve sidebar scroll position when navigating via links within sidebar
            this.scrollTop = sidebarScrollTop;
        } else {
            // scroll sidebar to current active section when navigating via "next/previous chapter" buttons
            var activeSection = document.querySelector('#sidebar .active');
            if (activeSection) {
                activeSection.scrollIntoView({ block: 'center' });
            }
        }
        // Toggle buttons
        var sidebarAnchorToggles = document.querySelectorAll('#sidebar a.toggle');
        function toggleSection(ev) {
            ev.currentTarget.parentElement.classList.toggle('expanded');
        }
        Array.from(sidebarAnchorToggles).forEach(function (el) {
            el.addEventListener('click', toggleSection);
        });
    }
}
window.customElements.define("mdbook-sidebar-scrollbox", MDBookSidebarScrollbox);
