# REST API
----

## Example Data and ETL API 

The following endpoints constitute the "ETL" API for both the Rare Diseases (RD) and Molecular Tumor Board (TMB) use cases. 

In the following: `use-case` in `{mtb,rd}` 

### JSON Schema of Patient Record Payloads

Request a JSON Schema description of the patient record (generated from the DTOs using library [scala-jsonschema](https://github.com/andyglow/scala-jsonschema)),
with optional indication of the desired JSON Schema version


```
GET /api/{use-case}/etl/patient-record/schema[?version={version}]
```
with `version` in `{draft-12, draft-09, draft-07, draft-04}`, Default: `draft-12`

**IMPORTANT NOTE**: Bear in mind that these JSON schemas for MTB and RD Patient Records are _provisional_, given that some adaptations
are to be expected once the technical specifications derived from the "Datenkranz" for the "MVGenomV" model project become available. 


### Synthetic JSON Data

API endpoint to request random generated, structurally/syntactically correct JSON examples of patient records

```
GET /api/{use-case}/fake/data/patient-record
```

### Upload a Patient Record

```
POST /api/{use-case}/etl/patient-record
```

**Response**

| Case | Response | Effect |
| ---- | -------- | ------ |
| Data OK                              | `200 OK` | Data set was saved in the query module |
| Data acceptable with quality issues  | `201 Created` with JSON issue report | Data Set was saved in the validation module with a corresponding issue report, but also in the query module |
| Unacceptable Issues                  | `422 Unprocessable Content` with JSON issue report | Data Set was saved in the validation module with a corresponding issue report |
| Fatal Issues Detected                | `400 Bad Request` with JSON issue report | Import transaction aborted | 

#### **IMPORTANT NOTE for MTB uploads (data uploads, JSON schemas)**:

In order to be backward compatible with the (soon obsolete) bwHC node's data import API, the DNPM:DIP system's API currently supports 2  MTB data upload formats:

| Format | Definition |
| ---- | -------- |
| `application/json`    | old bwHC MTBFile format (**Default**) |
| `application/json+v2` | new DNPM:DIP Patient record format |

In order to be non-breaking with the bwHC API, the old `application/json` is the default format of data upload requests, unless explicitly specified via the `Content-type` header.
Accordingly, the JSON schema returned for requests to `GET /api/mtb/etl/patient-record/schema[?version={version}]` corresponds to this `application/json` format.

However, the random generated MTB patient records are in the (new) `application/json+v2` format.
In order to obtain the JSON schema and perform data uploads for this `application/json+v2` format, it must (currently) be specified explicitly:

- **JSON Schema**: `GET /api/mtb/etl/patient-record/schema?format=application/json%2Bv2`
- **Data Upload** `POST /api/mtb/etl/patient-record` **with Header** 'Content-type: application/json+v2'

Apologies for the confusion and inconveniences this currently causes. Support for the old/obsolete bwHC format will eventually be dropped as soon as the data model has been aligned with Model Project specifications.


### Validate a Patient Record

This endpoint allows to simply _validate_ a patient record, without actually importing it in the system

```
POST /api/{use-case}/etl/patient-record:validate
```

**Response**

| Case | Response |
| ---- | -------- |
| Data fully acceptable        | `200 OK` |
| Data Acceptable with Issues  | `200 OK` with JSON issue report |
| Unacceptable Issues          | `422 Unprocessable Content` with JSON issue report |
| Fatal Issues Detected        | `400 Bad Request` with JSON issue report |



### Delete All of a Patient's Data

```
DELETE /api/{use-case}/etl/patient/{Patient-ID}
```

-------
## Catalogs (CodeSystems/ValueSets)

### Get List of CodeSystems

```
GET /api/coding/codesystems
```

**Response**
<details>
<summary>List of CodeSystem info objects containing name, title, URI, and possibly a list of available versions and pre-defined filters applicable to the CodeSystem</summary>

```javascript
{
  "entries": [
    {
      "filters": [
          {
              "description": "Filter ICD classes of kind 'chapter'",
              "name": "is-a-chapter"
          },
          {
              "description": "Filter ICD classes of kind 'block'",
              "name": "is-a-block"
          },
          {
              "description": "Filter ICD classes of kind 'category'",
              "name": "is-a-category"
          }
      ],
      "latestVersion": "2024",
      "name": "ICD-10",
      "title": "Internationale statistische Klassifikation der Krankheiten und verwandter Gesundheitsprobleme",
      "uri": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
      "versions": [
          "2019",
          "2023",
          "2020",
          "2024",
          "2021",
          "2022"
      ]
    },
    ...
  ]
}
```
</details>


### Get specific CodeSystem

```
GET /api/coding/codesystems?uri={CodeSystem-URI}[&version={Version}][&filter=filter-A|filter-B&filter=filter-C]
```

**Response**
<details>
<summary>CodeSystem object </summary>

```javascript
{
  "uri": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
  "name": "ICD-10",
  "title": "Internationale statistische Klassifikation der Krankheiten und verwandter Gesundheitsprobleme",
  "date": "2023-09-15T00:00:00",
  "version": "2024",
  "properties": [
    {
      "name": "kind",
      "type": "enum",
      "description": "Kind of ICD class",
      "valueSet": [
          "block",
          "category",
          "chapter"
      ]
    }
  ],
  "concepts": [
    {
      "code": "C25",
      "display": "Bösartige Neubildung des Pankreas",
      "version": "2024",
      "properties": {
          "kind": [
              "category"
          ]
      },
      "parent": "C15-C26",
      "children": [
        "C25.4",
        "C25.8",
        "C25.0",
        "C25.1",
        "C25.9",
        "C25.3",
        "C25.7",
        "C25.2"
      ]
    },
    ...
  ]
}
```
</details>

This CodeSystem structure is *conceptually* equivalent to [FHIR CodeSystem](https://hl7.org/fhir/R4/codesystem.html), but syntactically slightly different.

#### Applying CodeSystem filters:

CodeSystems can be requested with applied filters on the concepts by including the filter name(s) as URI query parameter `filter`.
The API supports combining filters with AND/OR logic: filter names concatenated into a pipe-separated value list as one `filter` parameter are combined as OR, whereas filters occurring in different "filter" parameter values are combined using AND.
For instance, the following request represents the query "Get CodeSystem ICD-O-3, picking only concepts from ICD-O-3-M (morphology) and of kind 'block' or 'category'":

```
GET /api/coding/codesystems?uri=urn:oid:2.16.840.1.113883.6.43.1&filter=morphology&filter=is-a-block|is-a-category
```


### Get List of ValueSets

```
GET /api/coding/valuesets
```

**Response**
<details>
<summary>List of ValueSet info objects containing name, title and URI (currently identical to CodeSystem Info objects)</summary>

```javascript
{
  "entries": [
    {
      "filters": [
          {
              "description": "Filter ICD classes of kind 'chapter'",
              "name": "is-a-chapter"
          },
          {
              "description": "Filter ICD classes of kind 'block'",
              "name": "is-a-block"
          },
          {
              "description": "Filter ICD classes of kind 'category'",
              "name": "is-a-category"
          }
      ],
      "latestVersion": "2024",
      "name": "ICD-10",
      "title": "Internationale statistische Klassifikation der Krankheiten und verwandter Gesundheitsprobleme",
      "uri": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
      "versions": [
          "2019",
          "2023",
          "2020",
          "2024",
          "2021",
          "2022"
      ]
    },
    ...
  ]
}
```
</details>


### Get specific ValueSet

```
GET /api/coding/valuesets?uri={ValueSet-URI}[&version={Version}]
```

**Response**
<details>
<summary>ValueSet object</summary>

```javascript
{
  "uri": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
  "name": "ICD-10",
  "title": "Internationale statistische Klassifikation der Krankheiten und verwandter Gesundheitsprobleme",
  "date": "2023-09-15T00:00:00",
  "version": "2024",
  "concepts": [
    {
      "code": "C25",
      "display": "Bösartige Neubildung des Pankreas",
      "system": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
      "version": "2024"
    },
    ...
  ]
}
```
</details>

This ValueSet structure is *conceptually* equivalent to [FHIR ValueSet](https://hl7.org/fhir/R4/valueset.html), but syntactically slightly different.


----
## Query API


### MTB Query Criteria

Query Criteria for MTB patient records:

| Block | Attribute Name | Type | Multiplicity | API Binding for CodeSystem/ValueSet (incl. Filters) |
| ----- | ----           | ---- | ----         | ----                        |
| Diagnosis Criteria          | Code | Coding[ICD-10-GM] | 0...N | <code>/api/coding/codesystems?uri=http://fhir.de/CodeSystem/bfarm/icd-10-gm&filter=is-a-category</code> |
| Tumor-Morphology Criteria   | Code | Coding[ICD-O-3-M] | 0...N | <code>/api/coding/codesystems?uri=urn:oid:2.16.840.1.113883.6.43.1&filter=morphology&filter=is-a-category</code> |
| SNV Criteria (0...N)        | Gene | Coding[HGNC]      | 0...1 | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>                |
| SNV Criteria (0...N)        | DNA Change     | Coding[HGVS.DNA]     | 0...1 |  |
| SNV Criteria (0...N)        | Protein Change | Coding[HGVS.Protein] | 0...1 | NOTE: 3-letter amino acid code required |
| CNV Criteria (0...N)        | Genes  | Coding[HGNC]      | 0...N | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>              |
| CNV Criteria (0...N)        | Type  | Coding[CNV.Type]   | 0...1 | <code>/api/coding/codesystems?uri=dnpm-dip/mtb/ngs-report/cnv/type</code>        |
| DNA-Fusion Criteria (0...N) | 5'-Gene  | Coding[HGNC] | 0...1 | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>          |
| DNA-Fusion Criteria (0...N) | 3'-Gene  | Coding[HGNC] | 0...1 | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>          |
| RNA-Fusion Criteria (0...N) | 5'-Gene  | Coding[HGNC] | 0...1 | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>          |
| RNA-Fusion Criteria (0...N) | 3'-Gene  | Coding[HGNC] | 0...1 | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>          |
| Medication Criteria (0...1) | Medication | Coding[ATC]   | 0...N |  <code>/api/coding/codesystems?uri=http://fhir.de/CodeSystem/bfarm/atc</code>  |
| Medication Criteria (0...1) | Usage Mode | Coding[Usage] | 0...2  | <code>/api/coding/codesystems?uri=dnpm-dip/mtb/query/medication-usage</code>  |
| Medication Criteria (0...1) | Operator | String | 0...1  | {and, or} Default: or  |
| Response Criteria | Code  | Coding[RECIST] | 0...N | <code>/api/coding/codesystems?uri=RECIST</code> |

See [MTBQueryCriteria DTO](https://github.com/KohlbacherLab/dnpm-dip-mtb-query-service/blob/main/api/src/main/scala/de/dnpm/dip/mtb/query/api/MTBQueryCriteria.scala#L113) for structure of the corresponding JSON form.

----
### RD Query Criteria

Query Criteria for RD patient records:

| Block | Attribute Name | Type | Multiplicity | API Binding for CodeSystem/ValueSet (incl. Filters) |
| ----- | ----           | ---- | ----         | ----                        |
| HPO Term         | Code   | Coding[HPO] | 0...N | <code>/api/coding/codesystems?uri=https://hpo.jax.org</code> |
| Disease Category | Code   | Coding[Orphanet] | 0...N | <code>/api/coding/codesystems?uri=https://www.orpha.net</code> |
| Variant  | Gene           | Coding[HGNC] | 0...N | <code>/api/coding/codesystems?uri=https://www.genenames.org/</code>  |
| Variant  | cDNA change    | Coding[HGVS.DNA] | 0...N |   |
| Variant  | gDNA change    | Coding[HGVS.DNA] | 0...N |   |
| Variant  | protein change | Coding[HGVS.Protein] | 0...N | NOTE: 3-letter amino acid code required |

See [RDQueryCriteria DTO](https://github.com/KohlbacherLab/dnpm-dip-rd-query-service/blob/main/api/src/main/scala/de/dnpm/dip/rd/query/api/RDQueryCriteria.scala#L48) for structure of the corresponding JSON form.


### Submit Query

```
POST /api/{use-case}/queries
```
**Request Body**
<details>
<summary>Composite of Query Mode and Query Criteria object</summary>

```javascript
{
  "mode": { 
    "code": "local"   // {local, federated} 
  },
  "criteria": { ... }
}
```
</details>

**Response**
<details>
<summary>Created Query session object</summary>

```javascript
{
  "id" : "1e3c229e-ffb4-47fa-9602-1c2b26c8117f",
  "submittedAt" : "2023-09-15T12:04:06.521604",
  "querier" : "Dummy-Querier-ID",
  "mode" : { ... },
  "criteria" : {
    // Query Criteria object as submitted, but with initialized "display" and "system" values in all codings
  },
  "expiresAfter" : 900,  // Validity period (seconds) after which query session expires unless refreshed by some operation
  "lastUpdate" : "2023-09-15T10:04:06.521634Z"
}

```
</details>

### Get Open Queries

```
GET /api/{use-case}/queries
```

**Response**
<details>
<summary>Collection of Query session objects</summary>

```javascript
{
  "entries": [
    {
      "id" : "1e3c229e-ffb4-47fa-9602-1c2b26c8117f",
      "submittedAt" : "2023-09-15T12:04:06.521604",
      "querier" : "Dummy-Querier-ID",
      "mode" : { ... },
      "criteria" : { ... },
      "expiresAfter" : 900,  // Validity period (seconds) after which query session expires unless refreshed by some operation
      "lastUpdate" : "2023-09-15T10:04:06.521634Z"
    }
  ]
}

```
</details>


### Get Query by ID

```
GET /api/{use-case}/queries/{Query-ID}

GET /api/{use-case}/queries?id={Query-ID}
```

**Response**
<details>
<summary>Query session object</summary>

```javascript
{
  "id" : "1e3c229e-ffb4-47fa-9602-1c2b26c8117f",
  "submittedAt" : "2023-09-15T12:04:06.521604",
  "querier" : "Dummy-Querier-ID",
  "mode" : { ... },
  "criteria" : { ... },
  "expiresAfter" : 900,  // Validity period (seconds) after which query session expires unless refreshed by some operation
  "lastUpdate" : "2023-09-15T10:04:06.521634Z"
}

```
</details>


### Get Query ResultSet Summary

```
GET /api/{use-case}/queries/{Query-ID}/summary
```

**Response**
<details>
<summary>Query Result Summary object</summary>

```javascript
{
  "id": "<Query-ID>",
  "numPatients": 31,
   ....
}
```
</details>


### Get Patient Matches from Query ResultSet

```
GET /api/{use-case}/queries/{Query-ID}/patient-matches

GET /api/{use-case}/queries/{Query-ID}/patients
```

**Response**
<details>
<summary>Collection of Patient Match objects, i.e. minimal Patient Info together with subset of matching query criteria</summary>

```javascript
{
  "entries": [
    {
      "id": "018b00bb-2898-49eb-82b8-dd88617da326",  // Patient-ID
      "age": { 
         "value": 48,
         "unit": "a"   // Years
      },
      "gender": {
          "code": "other",
          "display": "Divers",
          "system": "Gender"
      },
      "vitalStatus": {
          "code": "alive",
          "display": "Lebend",
          "system": "dnpm-dip/patient/vital-status"
      },
      "matchingCriteria": {
        // Subset of criteria matching the submitted criteria...
      },
    },
    ...
  ]
}

```
</details>


### Get specific PatientRecord from Query ResultSet

```
GET /api/{use-case}/queries/{Query-ID}/patient-record/{Patient-ID}
```

**Response**

Patient Record (see above for JSON Schema and example data)


### Update Query

```
PUT /api/{use-case}/queries/{Query-ID}
```

**Request Body**
<details>
<summary>Query Update object</summary>

```javascript
{
  "id": "<Query-ID>",
  "mode": {        // Optional here, only if mode changed
    "code": "local | federated"  
  },
  "criteria": {    // Optional here, only if query criteria changed
    ...
  }
}
```
</details>

**Response**
<details>
<summary>Updated Query session object</summary>

```javascript
{
  "id" : "1e3c229e-ffb4-47fa-9602-1c2b26c8117f",
  ...
  "lastUpdate" : "2023-09-15T10:04:06.521634Z"
}

```
</details>



### Delete Query by ID

```
DELETE /api/rd/queries/{Query-ID}
```

**Response**
<details>
<summary>Query session object</summary>

```javascript
{
  "id" : "1e3c229e-ffb4-47fa-9602-1c2b26c8117f",
  "submittedAt" : "2023-09-15T12:04:06.521604",
  "querier" : "Dummy-Querier-ID",
  "mode" : { ... },
  "criteria" : { ... },
  "expiresAfter" : 900,  // Validity period (seconds) after which query session expires unless refreshed by some operation
  "lastUpdate" : "2023-09-15T10:04:06.521634Z"
}

```
</details>



