# Changelog

## [1.0.4](https://github.com/dnpm-dip/api-gateway/compare/v1.0.3...v1.0.4) (2025-09-08)


### Bug Fixes

* Upgraded service-base, mtb-validation-service and rd-validation-service versions ([9b2db3e](https://github.com/dnpm-dip/api-gateway/commit/9b2db3ef1fc64d84a3b9af422c9cedf491b53f98))

## [1.0.3](https://github.com/dnpm-dip/api-gateway/compare/v1.0.2...v1.0.3) (2025-08-21)


### Bug Fixes

* Updated component versions ([c4fb903](https://github.com/dnpm-dip/api-gateway/commit/c4fb9038cffa629a84df48c2e66ce690f92b081a))

## [1.0.2](https://github.com/dnpm-dip/api-gateway/compare/v1.0.1...v1.0.2) (2025-08-19)


### Bug Fixes

* Updated MTB and RD dependency versions; Adapted TAN generation; Some code clean-up ([424804c](https://github.com/dnpm-dip/api-gateway/commit/424804c7c7d696aefb5f6a8fed9d3fee5088e42a))

## [1.0.1](https://github.com/dnpm-dip/api-gateway/compare/v1.0.0...v1.0.1) (2025-08-11)


### Bug Fixes

* Updated dependencies in build.sbt; Fixes to files for docker build/release ([1669300](https://github.com/dnpm-dip/api-gateway/commit/166930073125210b43e1cb5aacf4a82e94888e83))

## 1.0.0 (2025-08-08)


### Features

* Adaptation to AdminRouter for connection status endpoint; Added API MetaInfo endpoint ([1c0c041](https://github.com/dnpm-dip/api-gateway/commit/1c0c041a3a9dc68dc394b1756414adbabdd6ea24))
* Adapted MVH endpoints to base service API ([4c99f59](https://github.com/dnpm-dip/api-gateway/commit/4c99f59b44d664f3e11a21e98df2599c80446d49))
* Adapted query ownership check to return 404 in case the query is absent due to time-out; Added health check to Dockerfile ([d3f713e](https://github.com/dnpm-dip/api-gateway/commit/d3f713e8c04c7549936eb35ea97a4d4cb0a8f859))
* Adapted route to endpoint for peer-to-peer atientRecordRequest ([35c1cb2](https://github.com/dnpm-dip/api-gateway/commit/35c1cb2520a4fa776586cb8420ed95cafa286d50))
* Adapted to changed base composents for MVH ([e2395e5](https://github.com/dnpm-dip/api-gateway/commit/e2395e5141a1ce67adbb42262b80b96bf2eb094c))
* Added API endpoint for Query default filter ([93e8c1f](https://github.com/dnpm-dip/api-gateway/commit/93e8c1fbdf9d4796eefcf8153ced0880a873b7bd))
* Added API endpoint to list submission reports for ETL clients; Adapted mappings of domain errors to HTTP responses to refactored MVH base service ([a672702](https://github.com/dnpm-dip/api-gateway/commit/a6727025e7f035729b4a4f8f82d8a92e206610ac))
* Added endpoints for alternative implementation of therapy responses statistics over a query result-set ([2565a08](https://github.com/dnpm-dip/api-gateway/commit/2565a089bd68cdcc13ec46f9b29b7932955be261))
* Added pagination to all 'Collection' resource endpoint; fix: Added 'Cache-control' header for correct client-side caching behaviour of resources dependent on query state ([3d4742f](https://github.com/dnpm-dip/api-gateway/commit/3d4742f4ef47df9db1bf166bb81f0ad9b7fd6ad8))
* Added random generation of DataUpload (i.e. incl. MVH metadata) ([71c5895](https://github.com/dnpm-dip/api-gateway/commit/71c5895fcb167668d6ec2685c38791b1e57bc98f))
* Improved dockerization script; fix: Minor code clean-up ([2b97035](https://github.com/dnpm-dip/api-gateway/commit/2b97035b109fca342bd5b8d047c12233f9e37444))
* Upgraded to Play 3.0 ([274bf7e](https://github.com/dnpm-dip/api-gateway/commit/274bf7eab864810f79b6407a580472be1170c479))


### Bug Fixes

* Adapted controller to changed Orchestrator entry point; Corrected MVH-submission generator logic ([a79f13a](https://github.com/dnpm-dip/api-gateway/commit/a79f13a1bdc993b6d9734352500c46500f54e38a))
* Adapted generator of dummy MVH submissions to always set submission type 'test' ([a992b9b](https://github.com/dnpm-dip/api-gateway/commit/a992b9ba46645854f0b1a9f53379f2f9bfe67cc4))
* Adapted scalac linting and fixed many reported errors (mostly unused imports) ([b630f4d](https://github.com/dnpm-dip/api-gateway/commit/b630f4dd42b1d6aa7cd78bed07ce6984f83de796))
* Adapted schema endpoint to return formatted JSON ([8839e7d](https://github.com/dnpm-dip/api-gateway/commit/8839e7dc3672144493c4fe22d94bc09154077126))
* Added explanatory note to syntactic/semantic validations performed in ETL API ([b4a58bd](https://github.com/dnpm-dip/api-gateway/commit/b4a58bd7d2a8135d864353f45d2ed8612049a2c5))
* Added note about /api prefix depending on reverse proxy setup for backend exposure ([a8f4fb2](https://github.com/dnpm-dip/api-gateway/commit/a8f4fb2d96879c2c7f0661ffc52591cbcf8fee00))
* Added note for handling of system, version and display for Codings in uploads ([757b88a](https://github.com/dnpm-dip/api-gateway/commit/757b88ac72115b2c34d0e800dae8c2a8572fa9f2))
* Added note on provisional character of JSON schemas to README ([feaf6ef](https://github.com/dnpm-dip/api-gateway/commit/feaf6efd8c9133689308b9befaeb46368913a12f))
* Clean-up in build.sbt ([d2df3a3](https://github.com/dnpm-dip/api-gateway/commit/d2df3a3e539bf1020b0b9bf8b66e9e7fe85cc5ac))
* Corrected typos and improved formulation ([c682846](https://github.com/dnpm-dip/api-gateway/commit/c682846766ce69118e067ae0129f9c28a3388e7e))
* Docker Image: Corrected URI path in health check cmd and updated image to 'dnpm-dip' organization ([e5d618a](https://github.com/dnpm-dip/api-gateway/commit/e5d618a40d47320aa7cde0f6fd014beea1c11298))
* Dockerfile: Removed erroneous default USER assignment; Minor clean-up of 'cache-control' settings ([8520d59](https://github.com/dnpm-dip/api-gateway/commit/8520d59c616f97d46f8babb452a645f3025705d9))
* Minor improvement to API README ([aecae0c](https://github.com/dnpm-dip/api-gateway/commit/aecae0cbab06e86bfe513b8977e8a586039e7555))
* Minor layouting in README ([da0e900](https://github.com/dnpm-dip/api-gateway/commit/da0e900d78efa9aeecd61568820f1ce757f37b9d))
* Minor re-formulation ([0f6af9d](https://github.com/dnpm-dip/api-gateway/commit/0f6af9d1e57f366334092f046633cf6168b4fcdc))
* Minor update to link in README ([ac08fb5](https://github.com/dnpm-dip/api-gateway/commit/ac08fb5bd821d3de7dc914fd9e5f55498fc73391))
* Moved start-up commands into dedicated entrypoint.sh to remove 'JSONArgsRecommended' warning in image creation ([d0048a4](https://github.com/dnpm-dip/api-gateway/commit/d0048a49a23accd26f44c5e6963a1d4b0dd5db36))
* Reformulation on API doc README ([32db17f](https://github.com/dnpm-dip/api-gateway/commit/32db17f14f753a5404260b9597d856a6ecfabd22))
* Removed obsolete query parameter 'format' on JSON schema endpoint, and added lazy evaluation of the JSON schemata ([731b730](https://github.com/dnpm-dip/api-gateway/commit/731b73057ae48fb9f8bd104ea03bc2b2e4a929b9))
* Renamed endpoint for query default filters ([83a836e](https://github.com/dnpm-dip/api-gateway/commit/83a836eb4cfa272ec6c90a8ded31dfcb59528b08))
* Testing GitHub markdown rendering ([523c9af](https://github.com/dnpm-dip/api-gateway/commit/523c9afc15056c93fb4dd9e0ae65527debde3369))
* Updated Docker README ([6895013](https://github.com/dnpm-dip/api-gateway/commit/689501349b093926474ef879d3a5df519001a5a5))
* Updated link to API README ([7449555](https://github.com/dnpm-dip/api-gateway/commit/74495556fa1394f0ef60e06909f81379a19f90f1))
