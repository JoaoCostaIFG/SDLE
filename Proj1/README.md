# SDLE Project #1

SDLE Project for group **T3G11** (FEUP, MEIC 2021/22).

## Group members

1. Davide Castro - `up201806512@edu.fe.up.pt`
2. Henrique Ribeiro - `up201806529@edu.fe.up.pt`
3. João Costa - `up201806560@edu.fe.up.pt`
4. Tiago Silva - `up201806516@edu.fe.up.pt`

## Dependencies

- Java 17 - programming language
- Maven - dependency management and compiling
  - [jeromq 0.5.3-SNAPSHOT](https://github.com/zeromq/jeromq)
  - maven-jar-plugin 3.2.0
  - exec-maven-plugin 3.0.0
  - maven-compiler-plugin 3.8.1

## Compile

To compile the project, **java 17** (or **open-jdk 17**) needs to be installed.
The project is compiled by calling `mvn compile` (using **maven**).

Note: although java version 17 is suggested, because it was the version used
during development and testing, other (untested) java versions may work too.

## Run

Besides being used to compile the project, **maven** can be used to run the
program as well.

### Usage

The arguments of the program are as follows: `<proxy|id <put|get>> [arg1 [arg2]]`.

There are 3 different **roles**:

- **proxy** - the central server. One instance of this should be running in
  order to allow messages to be delivered/shared between clients. E.g.:
  `proxy`.
- **put** - a client that is publishing information. This will post a given
  number of random updates to a given topic E.g.:
  `id1 put topic number_of_posts`.
- **get** - a client that is subscribing to a topic. An optional second argument
  can be given specifying the number of updates to retrieve before unsubscribing
  from the topic and quitting. If this second argument is not specified, the
  program will stay retrieving updates forever. E.g.: `id1 get topic`.
>>>>>>> proj1/master
