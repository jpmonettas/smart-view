# smart-view

Visualize and explore your solidity smart contracts.

# Build 

Before anything install:

- Java
- [Leiningen] (https://leiningen.org/)

Now run:

```bash
$ lein uberjar
```

A smart-view.jar file should be now generated under target folder.

# Run 

```bash
$ java -jar ./target/smart-view.jar full-path-to-solidity-files-folder
```

Now open http://localhost:3000

# How does it looks?

<img src="/docs/contracts-map-1.png?raw=true"/>

# How does it works?

- It walks over every solidity file i full-path-to-solidity-files-folder
- Analize parse it's contents using [antlr4](https://github.com/antlr/antlr4) and this [grammar](https://github.com/solidityj/solidity-antlr4)
- From the AST generates a bunch of facts inside a [datascript](https://github.com/tonsky/datascript/) database (functions, vars, enums, structs, events ...)
- Serializes the database and send it to a SPA for querying facts in different ways and generate visualizations

# Roadmap

Since you end up with a database in the browser full of facts about your smart contracts, lots of querys and visualizations can be derived.
