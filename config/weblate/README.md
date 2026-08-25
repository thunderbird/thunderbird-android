# Weblate configuration

This directory contains resource files used only to configure Weblate.

The Compose resource at:

```text
composeResources/values/values.xml
```

provides a minimal base-language file for a Weblate base component. It exists to work around Weblate’s requirement for 
a valid source file when creating a component whose configuration is reused by other components.

The resource is not part of any application module and must not be included in a build. Its string is marked 
with `translatable="false"` because it is only a configuration placeholder.
