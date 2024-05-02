# Add documentation

The documentation in this repository describes the Thunderbird for Android project. It is intended to provide a comprehensive overview of the project, contribution, architecture, and the decisions made during development.

We use [mdbook](https://rust-lang.github.io/mdBook/) to generate the documentation. The source files for the documentation are located in the `docs/` directory.

## Contributing to the Documentation

To add or modify the documentation, you need to edit the markdown files in the `docs/` directory.

The documentation is written using standard Markdown syntax, including GitHub flavored Markdown. You can use headers, lists, links, code blocks, and other Markdown features to structure your content.

### Adding a New Page

To add a new page to the documentation, you need to create a new markdown file in the `docs/` directory. You can add a new page to the table of contents by adding an entry to the `SUMMARY.md` file.

### Organizing Pages in Subfolders

Subfolders can be used in the `docs/` directory to organize the documentation. This can be useful if related topics should be grouped together. For example, we have a subfolder named `architecture/` for all documentation related to our application's architecture.

### Linking New Pages in the Summary

The `SUMMARY.md` file serves as the table of contents (TOC) for the documentation. To include the new page in the TOC, a link needs to be added in the `SUMMARY.md` file. The link should be added in the following format:

```markdown
- [Page Title](relative/path/to/file.md)
```

Indentation is used to create hierarchy in the TOC:

```markdown
- [Page Title](relative/path/to/file.md)
  - [Subpage Title](relative/path/to/subfolder/file.md)
```

## Building the Documentation

To build the documentation, you need to have `mdbook` and its dependencies installed. You can install `mdbook` using Cargo, the Rust package manager:

```bash
cargo install mdbook
```

Once you have `mdbook` installed, you can build the documentation by running the following command:

```bash
mdbook build
```

The generated documentation will be available in the `docs/book/` directory.

To preview the documentation, you can run the following command:

```bash
mdbook serve --open
```

The `mdbook serve` command will serve the book at http://localhost:3000 (by default) and rebuild the book on changes. The `--open` option will open the book in your web browser.
