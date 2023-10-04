# REST API

## CodeSystems

### Get List of CodeSystems

```
GET /api/coding/codesystems
```

**Response**
<details>
<summary>List of CodeSystem info objects containing name, title and URI</summary>

```javascript
{
  "entries": [
    {
        "name": "Diagnosis-Category",
        "title": "Diagnosis-Category",
        "uri": "dnpm-dip/rd/diagnosis/category"
    },
    {
        "name": "mode-of-inheritance",
        "title": "Mode of inheritance",
        "uri": "dnpm-dip/rd/variant/mode-of-inheritance"
    },
    {
        "name": "ACMG-Class",
        "title": "ACMG-Class",
        "uri": "https://www.acmg.net"
    },
    ...
  ]
}
```
</details>


### Get specific CodeSystem

```
GET /api/coding/codesystems?uri={CodeSystem-URI}
```

**Response**
<details>
<summary>CodeSystem object</summary>
```javascript
{
  "name": "Gender",
  "title": "Gender",
  "uri": "Gender"
  "properties": [],
  "concepts": [
    {
      "code": "male",
      "display": "Männlich",
      "properties": {}
    },
    {
      "code": "female",
      "display": "Weiblich",
      "properties": {}
    },
    ...
  ],
}
```
</details>


## Rare Disease Query Module

Possible Query Criteria by which to query for RD Patients (all optional):

| Scope  | Name | Type |
| -----  | ----      | ---- |
| HPO       | Term   |  |
| Diagnosis | Category | Coding (CodeSystem dnpm-dip/rd/diagnosis/category) |
| Variant   | Gene     | HGNC Coding (CodeSystem https://www.genenames.org/)  |
| Variant   | cDNA change | Coding with [HGVS DNA Change](https://varnomen.hgvs.org/recommendations/DNA/) |
| Variant   | gDNA change | Coding with [HGVS DNA Change](https://varnomen.hgvs.org/recommendations/DNA/) |
| Variant   | protein change | Coding with [HGVS Protein Change](https://varnomen.hgvs.org/recommendations/protein/) using **3-letter amino acid code** |


### Submit Query

```
POST /api/rd/query
```
**Request Body**
<details>
<summary>Composite of Query Mode and RD Query Criteria object</summary>
```javascript
{
  "mode": { 
    "code": "local | federated"  
  },
  "criteria": {
    "diagnoses" : [ {
      "category" : {
        "code" : "endocrine",
      }
    } ],
    "hpoTerms" : [ {
      "code" : "HP:398974",
    } ],
    "variants" : [ {
      "gene" : {
        "code" : "HGNC:17929",
      },
      "cDNAChange" : {
        "code" : "NG_012232.1(NM_004006.2):c.93+1G>T",
      },
      "gDNAChange" : {
        "code" : "NC_000023.10:g.33038255C>A",
      },
      "proteinChange" : {
        "code" : "LRG_199p1:p.Trp24Ter (p.Trp24*)",
      }
    } ]
  }
}
```
</details>

#### Variant

```
POST /api/rd/query?mode={local|federated}
```

**Request Body**
<details>
<summary>RD Query Criteria object</summary>
```javascript
{
  "diagnoses" : [ {
    "category" : {
      "code" : "endocrine",
    }
  } ],
  "hpoTerms" : [ {
    "code" : "HP:398974",
  } ],
  "variants" : [ {
    "gene" : {
      "code" : "HGNC:17929",
    },
    "cDNAChange" : {
      "code" : "NG_012232.1(NM_004006.2):c.93+1G>T",
    },
    "gDNAChange" : {
      "code" : "NC_000023.10:g.33038255C>A",
    },
    "proteinChange" : {
      "code" : "LRG_199p1:p.Trp24Ter (p.Trp24*)",
    }
  } ]
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
  "mode" : {
    "code" : "local",
    "display" : "Lokal",
    "system" : "dnpm-dip/query/mode"
  },
  "criteria" : {
    // RD Query Criteria object as submitted, but with initialized "display" and "system" values in all codings
  },
  "expiresAfter" : 900,  // Validity period (seconds) after which query session expires unless refreshed by some operation
  "lastUpdate" : "2023-09-15T10:04:06.521634Z"
}

```
</details>


### Get Query

```
GET /api/rd/query/{Query-ID}
```

**Response**
<details>
<summary>Query session object</summary>
```javascript
{
  "id" : "1e3c229e-ffb4-47fa-9602-1c2b26c8117f",
  "submittedAt" : "2023-09-15T12:04:06.521604",
  "querier" : "Dummy-Querier-ID",
  "mode" : {
    "code" : "local",
    "display" : "Lokal",
    "system" : "dnpm-dip/query/mode"
  },
  "criteria" : {
    // RD Query Criteria object as submitted, but with initialized "display" and "system" values in all codings
  },
  "expiresAfter" : 900,  // Validity period (seconds) after which query session expires unless refreshed by some operation
  "lastUpdate" : "2023-09-15T10:04:06.521634Z"
}

```
</details>


### Get Query ResultSet Summary

```
GET /api/rd/query/{Query-ID}/summary
```

**Response**
<details>
<summary>Query Result Summary object</summary>
```javascript
{
  "id": "<Query-ID>",
  "numPatients": 31
}
```
</details>


### Get Patient Matches from Query ResultSet

```
GET /api/rd/query/{Query-ID}/patients
```

**Response**
<details>
<summary>List of Patient Match object, i.e. minimal Patient Info together with subset of matching query criteria</summary>
```javascript
{
  "entries": [
    {
      "id": "018b00bb-2898-49eb-82b8-dd88617da326",  // Patient-ID
      "age": 48,                                     // Age in years
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
GET /api/rd/query/{Query-ID}/patient-record/{Patient-ID}
```

**Response**
<details>
<summary>RD Patient Record</summary>
```javascript

```
</details>


### Update Query

```
PUT /api/rd/query/{Query-ID}
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




