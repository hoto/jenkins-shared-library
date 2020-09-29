[![CircleCI](https://circleci.com/gh/hoto/jenkinsfile-loader/tree/master.svg?style=svg)](https://circleci.com/gh/hoto/jenkinsfile-loader/tree/master)
### Jenkins Shared Library

Used for demonstration purposes in [jenkinsfile-examples](https://github.com/hoto/jenkinsfile-examples)

### How To Use:

The Jenkins Shared Library is intended to be used in internal builds to standardise test, build, and push steps across similar pipelines, e.g. image build pipelines.
The vars/ directory contains the pipeline functions which can be called in the Jenkinsfile of the consumer jobs, such as:

[pipelinedemo](https://github.com/controlplaneio/demo-api/blob/master/Jenkinsfile)
[pipeguard](https://github.com/controlplaneio/cp-config/blob/master/Jenkinsfile)

To implement the library in a Jenkinsfile the base syntax criteria is as follows:

```
#!/usr/bin/env groovy

@Library('NAME_OF_LIBRARY') _

NAME_OF_PIPELINE([
  CONFIG: [
    VAR1: true,
    VAR2: 'string',
  ],
])
```

See a blog of this repo here:
* [Private Jenkins Shared Libraries](https://medium.com/@AndrzejRehmann/private-jenkins-shared-libraries-540abe7a0ab7)

### Documentation

* https://jenkins.io/doc/book/pipeline/shared-libraries/
* https://jenkins.io/blog/2017/10/02/pipeline-templates-with-shared-libraries/

### Run unit tests

On windows use `gradlew.bat` instead of `./gradlew`.
You can also install gradle globally and then run just `gradle` and ignore `gradlew` script.
Or just import in your favourite java IDE.

    ./gradlew clean test


