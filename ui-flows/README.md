# UI flows

Ui flows are using [Maestro](https://maestro.mobile.dev/), that allows to write UI E2E tests for Android.

The flows are located in the `ui-utils` folder with this structure:

- `custom` - flows that should not be committed to git
- `shared` - flows usable by other flows
- `validate` - flows that assert behavior of the app

## Requirements

- Android Pixel 2 emulator 5.0" screen with 1080x1920 resolution and 420dpi
- API 31
- English as system language

## Install

To be able to run the flows, you need to [install the CLI tools](https://maestro.mobile.dev/getting-started/installing-maestro)

## Run

Ensure a device or emulator is running and execute:

- `maestro test ui-flows/validate/compose_simple_message.yml `
- `maestro test ui-flows/validate/message_details_show_contact_names.yml`

## Write

Have a look at the [documentation](https://maestro.mobile.dev/) on how to write flows.
