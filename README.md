```markdown
# OperationalTransformation

A small, well-documented implementation of Operational Transformation (OT) for collaborative text editing. This repository contains utilities to represent operations, transform concurrent operations, and apply them to shared documents. Designed to be language-agnostic and easy to adapt to JavaScript/TypeScript or other platforms.

## Table of contents
- [Overview](#overview)
- [Features](#features)
- [Quick start](#quick-start)
- [Core concepts & API](#core-concepts--api)
- [Examples](#examples)
- [Algorithms & references](#algorithms--references)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)

## Overview
Operational Transformation (OT) is a technique for supporting real-time collaborative editing by transforming concurrent operations so they can be applied in different orders while producing the same final document. This repo provides a clear, testable OT core that can be plugged into a sync layer (server + client) or used for education.

## Features
- Represent basic text operations: insert, delete, retain
- Transform pairs of concurrent operations (OT transform)
- Compose and invert operations
- Apply operations to documents and check consistency
- Minimal, well-tested core suitable for building editors or learning OT

## Quick start

Install (example for npm projects — adapt if using a different language):
```bash
# If this repo is published as a package, otherwise copy the source into your project
npm install your-ot-package
```

Usage (JavaScript / TypeScript example; adapt to actual API names in this repo):
```js
import { Operation, apply, transform, compose } from 'operational-transformation';

// Two users start with same document
let doc = "Hello World";

// User A inserts " beautiful" after "Hello"
const opA = Operation.fromComponents([{ retain: 5 }, { insert: " beautiful" }, { retain: 6 }]);

// User B deletes " World"
const opB = Operation.fromComponents([{ retain: 5 }, { delete: 6 }]);

// Transform ops against each other
const [opAPrime, opBPrime] = transform(opA, opB);

// Apply in differing orders and end up with same result
const result1 = apply(apply(doc, opA), opBPrime);
const result2 = apply(apply(doc, opB), opAPrime);

console.assert(result1 === result2, 'OT correctness check');
```

## Core concepts & API
The implementation exposes primitives similar to common OT systems:

- Operation: represents a sequence of components (retain, insert, delete).
  - fromComponents(components): build an operation
  - toJSON()/fromJSON(): serialization helpers
- apply(document, operation): apply an operation to a string document
- transform(opA, opB): returns [opA', opB'] — transformed operations that commute
- compose(opA, opB): combine consecutive operations into one (when applicable)
- invert(op, baseDocument): produce an operation that reverts `op` given the base

Component representation (example):
- { retain: N } — skip N characters
- { insert: "text" } — insert text
- { delete: N } — delete N characters

Edge cases handled:
- Mixed inserts at same position (define tie-breaking rules, e.g. userId/order)
- Deleting beyond document length (validated)
- Retain merging and normalization

## Examples
- Simple two-user concurrent edit (see Quick start)
- Multi-step history composition and rebasing
- Server integration: store canonical history, rebase incoming client ops against missed ops
- CRDT comparison notes and example conversion ideas

## Algorithms & references
This implementation follows standard OT patterns and is influenced by:
- "Concurrent Editing in Distributed Systems" style OT design
- Google Wave / Jupiter model
- Sun, Ellis, & Gibbs, "Concurrency Control in Real-time Collaborative Editing Systems"
- Additional resources:
  - https://en.wikipedia.org/wiki/Operational_transformation
  - https://hal.inria.fr/inria-00070867/document

## Development
Run tests:
```bash
# install dependencies
npm install

# run tests
npm test
```

Linting:
```bash
npm run lint
```

Structure (suggested):
- src/ — core implementation (Operation, transform, apply, compose, invert)
- test/ — unit tests for correctness and edge cases
- examples/ — small example apps and scripts

## Contributing
Contributions are welcome. Please follow these steps:
1. Fork the repository
2. Create a feature branch: git checkout -b feat/my-feature
3. Add tests for new behavior
4. Run tests and linters
5. Open a pull request with a clear description of changes

Guidelines:
- Keep the core small and well-documented
- Add benchmarks for performance-sensitive changes
- Discuss protocol-breaking changes in an issue first

## License
MIT License — see LICENSE file for details.
```
