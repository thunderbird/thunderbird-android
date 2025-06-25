# User Flows

The user flows diagrams below illustrate typical paths users take through the application, helping developers understand how different components interact from a user perspective.

For information about the repository structure and module organization, see the [Project Structure document](project-structure.md).

## Mail

### Reading email

![read email sequence](../assets/ReadEmail.png)

![read email classes](../assets/ReadEmailClasses.png)

### Sending email

![send email sequence](../assets/SendEmail.png)

## Verifying Flows

We plan to test these user flows using [maestro](https://maestro.dev/), a tool for automating UI tests. Maestro allows us to write tests in a
simple YAML format, making it easy to define user interactions and verify application behavior.

The current flows could be found in the *`ui-flows` directory in the repository.
