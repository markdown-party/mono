# kotlin-echo

[![.github/workflows/tests.yml](https://github.com/markdown-party/kotlin-echo/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/markdown-party/kotlin-echo/actions/workflows/tests.yml)

`markdown-party/kotlin-echo` is a library that manages a distributed log of operations, and provides
some simple abstractions to replicate events across multiple sites. It makes heavy use of
[Kotlin coroutines](https://kotlinlang.org/docs/coroutines-guide.html), so concurrent communication
with multiple sites is possible.

### About

I'm developing this library as part of my BSc. thesis at HEIG-VD on building a distributed Markdown
editor. Feel free to reach out to me at
[alexandre.piveteau@heig-vd.ch](mailto:alexandre.piveteau@heig-vd.ch).

## Using the library

### Installation

The library is made available through
[GitHub packages](https://github.com/markdown-party/kotlin-echo/packages). Assuming you've properly
[set up your access credentials](https://docs.github.com/en/packages/guides/configuring-apache-maven-for-use-with-github-packages),
you can then add the following dependency in your `build.gradle` file :

```groovy
implementation "markdown.party:echo:0.2.0-SNAPSHOT"
```

`-SNAPSHOT` releases are regularly released. Every week, the minor version number is incremented.
When the library becomes stable enough, non- `-SNAPSHOT` releases will be uploaded.

### Principles

> TBA

## Local setup

This project uses Kotlin 1.4.30 and is build with [Gradle](https://gradle.org). To run the unit
tests locally, please proceed as follows :

```bash
# Clone the repository locally.
> git clone git@github.com:markdown-party/kotlin-echo.git && cd kotlin-echo

# Run Gradle tests.
> ./gradlew test
```
