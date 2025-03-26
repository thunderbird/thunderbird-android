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
        this.innerHTML = '<ol class="chapter"><li class="chapter-item affix "><li class="spacer"></li><li class="chapter-item "><a href="CONTRIBUTING.html"><strong aria-hidden="true">1.</strong> Contributing</a></li><li class="chapter-item "><a href="DESIGN.html"><strong aria-hidden="true">2.</strong> Design</a></li><li class="chapter-item "><a href="ci/index.html"><strong aria-hidden="true">3.</strong> Release</a><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="ci/RELEASE.html"><strong aria-hidden="true">3.1.</strong> Release Process</a></li><li class="chapter-item "><a href="ci/AUTOMATION.html"><strong aria-hidden="true">3.2.</strong> Release Automation</a></li><li class="chapter-item "><a href="ci/HISTORICAL_RELEASE.html"><strong aria-hidden="true">3.3.</strong> Manual Release (historical)</a></li></ol></li><li class="chapter-item "><a href="translations.html"><strong aria-hidden="true">4.</strong> Translations</a></li><li class="chapter-item "><a href="contributing/java-to-kotlin-conversion-guide.html"><strong aria-hidden="true">5.</strong> Java to Kotlin Conversion Guide</a></li><li class="chapter-item "><a href="architecture/adr/index.html"><strong aria-hidden="true">6.</strong> Architecture Decision Records</a><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><div><strong aria-hidden="true">6.1.</strong> Accepted</div><a class="toggle"><div>❱</div></a></li><li><ol class="section"><li class="chapter-item "><a href="architecture/adr/0001-switch-from-java-to-kotlin.html"><strong aria-hidden="true">6.1.1.</strong> 0001 - Switch From Java to Kotlin</a></li><li class="chapter-item "><a href="architecture/adr/0002-ui-wrap-material-components-in-atomic-design-system.html"><strong aria-hidden="true">6.1.2.</strong> 0002 - UI - Wrap Material Components in Atomic Design System</a></li><li class="chapter-item "><a href="architecture/adr/0003-switch-test-assertions-from-truth-to-assertk.html"><strong aria-hidden="true">6.1.3.</strong> 0003 - Test - Switch Test Assertions From Truth to Assertk</a></li><li class="chapter-item "><a href="architecture/adr/0004-naming-conventions-for-interfaces-and-their-implementations.html"><strong aria-hidden="true">6.1.4.</strong> 0004 - Naming Conventions for Interfaces and Their Implementations</a></li><li class="chapter-item "><a href="architecture/adr/0005-central-project-configuration.html"><strong aria-hidden="true">6.1.5.</strong> 0005 - Central Project Configuration</a></li><li class="chapter-item "><a href="architecture/adr/0006-white-label-architecture.html"><strong aria-hidden="true">6.1.6.</strong> 0006 - White Label Architecture</a></li><li class="chapter-item "><a href="architecture/adr/0007-project-structure.html"><strong aria-hidden="true">6.1.7.</strong> 0007 - Project Structure</a></li></ol></li><li class="chapter-item "><div><strong aria-hidden="true">6.2.</strong> Proposed</div></li><li class="chapter-item "><div><strong aria-hidden="true">6.3.</strong> Rejected</div></li></ol></li><li class="chapter-item "><li class="spacer"></li><li class="chapter-item affix "><a href="HOW-TO-DOCUMENT.html">How to Document</a></li></ol>';
        // Set the current, active page, and reveal it if it's hidden
        let current_page = document.location.href.toString().split("#")[0];
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
