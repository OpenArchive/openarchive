fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android beta
```
fastlane android beta
```
Submit a new Beta Build to Beta
### android deploy
```
fastlane android deploy
```
Deploy a new version to the Google Play

```
fastlane android slack distribution
```
### android slack distribution
Get Slack OAuth Token from [Getting Slack OAuth Token](https://api.slack.com/authentication/oauth-v2)
If .env doesn't exist , add file under fastlane
Add hook and token as .env.example
Run _fastlane beta_ in terminal

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
