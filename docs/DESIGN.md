# Repository structure

The project is divided into several directories below which are nested gradle projects.

## app

This contains the highest level code such as UI and core logic.

## backend

APIs for sending and receiving messages

## mail

Low level code for dealing with internet mail protocols

## plugins

Additional, standalone, libraries used by K-9

![modules](Modules.png)

# Walkthrough

To help you understand the design, the following sequence diagrams show typical flows through the
classes. Each class is colour-coded by its top-level project. 

## Reading email

![read email sequence](ReadEmail.png)

![read email classes](ReadEmailClasses.png)

## Sending email

![send email sequence](SendEmail.png)

# Running Unit Tests

## System Requirements

 * [Oracle Java SE SDK 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

## Instructions

Run at project folder path:

```
./gradlew assembleDebug testDebugUnitTest ktlintCheck
```