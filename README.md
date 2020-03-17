<h1 align="center">
  scm-review-plugin
</h1>

With the review plugin, the SCM-Manager supports so-called pull or merge requests, which can be used to implement a dual control principle in software development. The process usually looks at the implementation of a feature or the correction of a bug (bugfix).

## Build and testing

The plugin can be compiled and packaged with the normal maven lifecycle:

* compile - `mvn compile` - compiles Java code and creates the ui bundle
* test - `mvn test` - executes test for Java and JavaScript
* package - `mvn package` - creates the final plugin bundle (smp package) in the target folder
* install - `mvn install` - installs the plugin (smp and jar) in the local maven repository
* deploy - `mvn deploy` - deploys the plugin (smp and jar) to the configured remote repository
* clean - `mvn clean` - removes the target directory

For the development and testing the `serve` lifecycle of the plugin can be used:

* run - `mvn run` - starts scm-manager with the plugin pre installed.

If the plugin was started with `mvn run`, the default browser of the os should be automatically opened.
If the browser does not start automatically, start it manually and go to [http://localhost:8081/scm](http://localhost:8081/scm).

In this mode each change to web files (src/main/js or src/main/webapp), should trigger a reload of the browser with the made changes.
If you compile a class (e.g.: with your íde from src/main/java to target/classes), 
the SCM-Manager context will restart automatically. So you can see your changes without restarting the server.

## Directory & File structure

A quick look at the top-level files and directories you'll see in a SCM-Manager project.

    .
    ├── node_modules/
    ├── src/
    |   ├── main/
    |   |   ├── java/
    |   |   ├── js/
    |   |   └── resources/
    |   ├── test/
    |   |   ├── java/
    |   |   └── resources/
    |   └── target/
    ├── .editorconfig
    ├── .gitignore
    ├── CHANGELOG.md
    ├── LICENSE
    ├── package.json
    ├── pom.xml
    ├── README.md
    ├── tsconfig.json
    └── yarn.lock

1.  **`node_modules/`**: This directory contains all of the modules of code that your project depends on (npm packages) are automatically installed.

2.  **`src/`**: This directory will contain all of the code related to what you see or not. `src` is a convention for “source code”.
    1. **`main/`**
        1. **`java/`**: This directory contain the Java code.
        2. **`js/`**: This directory contains the JavaScript code for the web ui, inclusive unit tests: suffixed with `.test.js`
        3. **`resources/`**: This directory contains the the classpath resources.
    2. **`test/`**
        1. **`java/`**: This directory contains the Java unit tests.
        3. **`resources/`**: This directory containers classpath resources for unit tests.
    3. **`target/`**: This is the build directory.
    
3.  **`.editorconfig`**: This is a configuration file for your editor using [EditorConfig](https://editorconfig.org/). The file specifies a style that IDEs use for code.

4.  **`.gitignore`**: This file tells git which files it should not track / not maintain a version history for.

5.  **`CHANGELOG.md`**: All notable changes to this project will be documented in this file.

6.  **`LICENSE`**: This project is licensed under the MIT license.

7.  **`package.json`**: Here you can find the dependency/build configuration.

8.  **`pom.xml`**: Maven configuration, which also includes things like metadata.

9.  **`README.md`**: A text file containing useful reference information about your project.

10. **`tsconfig.json`** This is the typescript configuration file.

11. **`yarn.lock`**: This is the ui dependency configuration.

## Need help?

Looking for more guidance? Full documentation lives [in the SCM-Manager repository](https://github.com/scm-manager/scm-manager/blob/develop/docs/Home.md). Do you have further ideas or need support?

- **Community Support** - Contact the SCM-Manager support team for questions about SCM-Manager, to report bugs or to request features through the official channels. [Contact our Team](https://www.scm-manager.org/support/).

- **Enterprise Support** - You require support with the integration of SCM-Manager into your processes, with the customization of the tool or simply a service level agreement (SLA)? **Contact our development partner Cloudogu! Their team is looking forward to discussing your individual requirements with you and will be more than happy to give you a quote.** [Request Enterprise Support](https://cloudogu.com/en/scm-manager-enterprise/).

