# REST API
----

:warning: **IMPORTANT NOTE**:
The URI paths below assume that your backend is exposed via the reverse proxy as configured in the [deployment setup](https://github.com/dnpm-dip/deployment),
where the `/api` prefix is used for disambiguation of backend API requests,
but stripped in the [`proxy_pass` directive](https://github.com/dnpm-dip/deployment/blob/cedfb09d80b9b75f2b82fe17fb5a242c0fd4dac0/nginx/sites-available/tls-reverse-proxy.template.conf#L44-L46).
Otherwise, discard the `/api` prefix from URI paths.


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


### Synthetic JSON Data

API endpoint to request random generated, structurally/syntactically correct JSON examples of payload objects

#### Patient Records

```
GET /api/{use-case}/fake/data/patient-record
```

#### MVGenomSeq Submissions

i.e. a patient record + [MVH metadata](https://ibmi-ut.atlassian.net/wiki/spaces/DRD/pages/1474938/Data+Model+-+SE+dip#DataModel-SE:dip-MVHMetadata)

```
GET /api/{use-case}/fake/data/mvh-submission
```
> :warning: **NOTE about generated MVH metadata**:  
> The generated metadata does not contain examples of Research Consent entries.
> These are expected to be FHIR/JSON [Consent](https://hl7.org/fhir/R4/consent.html) resources conforming to MII Consent specifications.
> One example can be found [here](https://github.com/dnpm-dip/service-base/blob/main/src/test/resources/consent.json).
> Refer to MII Consent documentation or your local consent management for further info on Broad/Research Consent.


### Upload a Patient Record

> :warning: **IMPORTANT NOTE about Codings**:
> Given that many elements of a patient record are concepts from various code systems, the models contain many [Coding](https://hl7.org/fhir/datatypes.html#Coding) objects.
> In the random generated JSON examples (see above), these Coding elements are created not only with the `code`, but also `display`, `system` and possibly `version` attribute values.
> However, in uploads only the `code` attribute is _required_, and the latter attributes can generally be omitted, so bear the following in mind for your Coding management: 
>
>| Attribute | Use | Note |
>|-----------|-----|------|
>|`code`     | Required | |
>| `system ` | Optional | For most of the coded elements, the code system is clearly defined from the context (e.g. for attribute `patient.gender`). The `system` must thus only be explicitly specified for those few attributes which allow Codings from _multiple_ code systems (e.g. for rare diseases, the diagnosis codes from ICD-10-GM, Orphanet or Alpha-ID-SE). In these cases, the referenced Coding type in the JSON schema has a required `system` attribute and lists admissible code system identifiers. |
>| `version` | Optional  | The code system version need only be set in cases where the code-system is versioned and the version particularly relevant (e.g. for medications, the ATC version). Else, by default, the Coding is resolved against the _latest_ available version of the corresponding code system. |
>| `display` | Discarded | Upon import, Coding objects are validated against the respective code system and the `display` is completed, so no need to set it. |


<details>
<summary><b>Note on the validations performed and meaning of the different response codes</b></summary>

Upon upload to the following API endpoint, the payload undergoes two-fold validation: After _syntactic_ validation (i.e. if the JSON payload can be properly parsed as a PatientRecord data transfer object (DTO)) there is a _semantic_ validation step.
This is required for various reasons:

Beyond mere structural correctness, more advanced validations like referential integrity checks, whether coded entries (e.g. ICD-10 codes) are correctly
resolvable in the respective code system, or that a given entity is in a valid state depending on some `status` attribute, are not possible on the schema level anyway,
 but require custom application logic (in case of interest, see for instance [here](https://github.com/KohlbacherLab/dnpm-dip-mtb-validation-service/blob/a785f5ba612addf31d32204d62113fae04c5981b/impl/src/main/scala/de/dnpm/dip/mtb/validation/impl/MTBValidators.scala#L265).

Also, many of the attributes on the PatientRecord are kept _optional_ on the syntactic level, even though they are semantically required in order for a data set to be meaningful.
The reason this was made so, is to separate concerns: Say you are an ETL developer in charge of extracting the data from respective primary systems and send them to the DNPM:DIP node.
If this upload were to fail because semantically required attributes are missing or incorrect due to incomplete/erroneous documentation in the patient record, you'd be receiving upload rejections for which you can't really do anything.
Therefore, errors pertaining to the content of a patient record are raised in the "data quality issue report" created in the semantic validation step and (aside from being returned in the upload response)
 are stored in the validation module of the DNPM node.

The idea is that some kind of data quality improvement loop be established at your respective site: Whether this consists in having documentarists in charge to regularly check the data quality issue reports
 in the node's (upcoming) validation portal, and proceed from there to correct the data in primary systems and trigger a re-export, or that your ETL setup directly forwards the data quality issue report returned
 in the error response into a corresponding local "workflow", is up to your site.

Given this envisioned process of API usage, you will notice that although the syntactic and semantic error reports have the same JSON structure, the error messages are in English and German, respectively.
The reason for this is that the former errors directly originate from the used JSON parsing/validation library, whereas the semantic validation reports, being meant for a "documentarist" user class, are in German in order be understandable by this non-technician user class.

Aside from the different language, this difference of "error report addressee" is also reflected by the different HTTP status codes, to allow differentiated handling:

* `400` indicates errors concerning the ETL developer (syntactical incorrectness or e.g. breaks of referential integrity)
* `422` or `201` indicate more or less severe semantic issues, and thus concern documentarists.

</details>


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



