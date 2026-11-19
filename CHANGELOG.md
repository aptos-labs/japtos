# Changelog

All notable changes to the Japtos SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- SLH-DSA-SHA2-128s post-quantum signature support (AIP-137), modeled on
  aptos-ts-sdk PR #802: key generation, signing/verification via BouncyCastle
  FIPS 205 (cross-checked against `@noble/post-quantum`), BCS
  (de)serialization (`AnyPublicKey` variant 5, `AnySignature` variant 4),
  `SingleKeyAuthenticator` (variant 2) with `SingleSender` transaction wrapping,
  `SlhDsaSha2128sAccount` and scheme-aware `Account` factories, AIP-80
  private-key strings (`slh-dsa-sha2-128s-priv-`), and hardened HD derivation
  (SLIP-0010 with the `SLH-DSA-SHA2-128s seed` curve parameter).

### Changed
- Adopted the Aptos Innovation-Enabling Source Code License (replacing Apache License 2.0).
  Added a top-level `LICENSE` file, bundled it into the published jar, and updated the
  `pom.xml` license metadata.

## [1.1.8] - 2024

### Changed
- Updated BouncyCastle dependency to version 1.82

## [1.1.6] - 2024

### Added
- Comprehensive logging system with configurable log levels
- Support for custom network configurations
- Enhanced JavaDoc documentation

### Changed
- Improved error handling and exception messages
- Updated dependencies for better compatibility

## [1.1.0] - 2024

### Added
- Gas Station (sponsored transactions) support
- Multi-Key account support with mixed signature schemes
- Keyless public key support for OAuth/passkey authentication
- Move Option type support for optional parameters
- Plugin system for extensibility
- BIP39/BIP44 hierarchical deterministic wallet support

### Changed
- Refactored account creation APIs for better flexibility
- Improved transaction signing and submission flow

## [1.0.0] - 2024

### Added
- Initial release of Japtos SDK
- Ed25519 account support
- Multi-Ed25519 multi-signature account support
- Transaction construction and signing
- BCS serialization support
- HTTP client for Aptos API interactions
- Account address and authentication key management
- Basic cryptographic utilities

[1.1.8]: https://github.com/aptos-labs/japtos/compare/v1.1.6...v1.1.8
[1.1.6]: https://github.com/aptos-labs/japtos/compare/v1.1.0...v1.1.6
[1.1.0]: https://github.com/aptos-labs/japtos/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/aptos-labs/japtos/releases/tag/v1.0.0

