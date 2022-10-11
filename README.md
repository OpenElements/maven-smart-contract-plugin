# maven-smart-contract-plugin

A Maven Plugin that compiles [solidity](https://docs.soliditylang.org/) contracts. To use the plugin the solidity
compiler `solc` must be installed.

The plugin currently only provides the goal `smart-contract:compile` that will compile all smart
contracts in `src/main/contracts`. The compiled files can be found under `target/contracts`. Currently the plugin
creates the binaries (*.bin), the Application Binary Interface (*.abi) and a metadata JSON (*_meta.json) for each
contract. Properties to configure the plugin might be added in future.

## Future versions

The following features might be added in future to the plugin:

- Since the solidity compiler can be used by using a docker container I plan to support that in future. By doing so the
  `solc` compiler must not be installed on the local machine. Only docker is needed that is much more common.
- Adding properties to configure the plugin