```text
Usage: html-cleaner [OPTIONS] INPUT [OUTPUT]

  A tool that modifies HTML to only keep allowed elements and attributes the
  same way that K-9 Mail does.

Options:
  -h, --help  Show this message and exit

Arguments:
  INPUT   HTML input file (needs to be UTF-8 encoded)
  OUTPUT  Output file
```

You can run this tool using the [html-cleaner](../../html-cleaner) script in the root directory of this repository.
It will compile the application and then run it using the given arguments. This allows you to make modifications to the
[HTML cleaning code](../../app/html-cleaner/src/main/java/app/k9mail/html/cleaner) and test the changes right away.
