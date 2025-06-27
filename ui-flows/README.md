# UI flows

Ui flows are using [Maestro](https://maestro.dev/), that allows to write UI E2E tests for Android.

The flows are located in the `ui-flows` folder with this structure:

- `custom` - flows that should not be committed to git
- `shared` - flows usable by other flows
- `validate` - flows that assert behavior of the app

## Requirements

- Android Pixel 2 emulator 5.0" screen with 1080x1920 resolution and 420dpi
- API 31
- English as system language

## Install

To be able to run the flows, you need to [install the CLI tools](https://docs.maestro.dev/getting-started/installing-maestro)

## Run

Ensure a device or emulator is running and execute:

- `maestro test ui-flows/validate/compose_simple_message.yml `
- `maestro test ui-flows/validate/message_details_show_contact_names.yml`

## Write

Have a look at the [documentation](https://docs.maestro.dev/) on how to write flows.

### Best Practices

- Use ID-based selectors when text might change and better maintainability as text selectors can be brittle
- Add comments to explain the purpose of each section of the test
- Use shared flows for common operations to avoid duplication
- Add appropriate wait commands (like `waitForAnimationToEnd`) when needed to ensure UI stability
- Use environment variables with shared flows to make them more reusable
