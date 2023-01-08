# BlockShuffle

Before starting, create a [personal github access token](https://github.com/settings/tokens) and make sure it as `read:packages` permissions. 

With this information, set the following environment variables:

```
GITHUB_ACTOR = <Your Github username>
GITHUB_TOKEN = <Your generation personal access token>
```

A jar can be created by running `./gradlew jar`

The github secret `StorageSAS` must be set in order to push the jar plugin on push to main branch.