# REST API
----
## Catalogs (CodeSystems/ValueSets)

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
GET /api/coding/codesystems?uri={CodeSystem-URI}[&version={Version}]
```

**Response**
<details>
<summary>CodeSystem object </summary>

```javascript
{
    "uri": "https://hpo.jax.org",
    "name": "Human-Phenotype-Ontology",
    "title": "Human Phenotype Ontology",
    "date": "2023-09-01T00:00:00",
    "version": "2023-09-01",
    "properties": [
       // Properties defined on the CodeSystem's concepts, e.g.
       {
            "name": "type",
            "type": "string",
            "description": "Node type"
        },
        {
            "name": "definition",
            "type": "string",
            "description": "Definition of the concept ..."
        },
        {
            "name": "superClasses",
            "type": "string",
            "description": "Super-classes, i.e. parent concepts"
        }

    ],
    "concepts": [
        {
            "code": "HP:...",
            "display": "...",
            "version": "2023-09-01",
            "properties": {
                "type": [
                    "CLASS"
                ],
                "superClasses": [
                   HP:...,
                   HP:...,
                   HP:...
                ]
            },
            "children": [
                "HP:000...",
                "HP:000...",
                "HP:001...",
                "HP:003...",
                "HP:003...",
                "HP:004..."
            ]
        },
        ...
    ]
}
```
</details>

This CodeSystem structure is *conceptually* equivalent to [FHIR CodeSystem](https://hl7.org/fhir/R4/codesystem.html), but syntactically slightly different.

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
        "name": "Diagnosis-Category",
        "title": "Diagnosis-Category",
        "uri": "dnpm-dip/rd/diagnosis/category"
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
    "uri": "https://hpo.jax.org",
    "name": "Human-Phenotype-Ontology",
    "title": "Human Phenotype Ontology",
    "date": "2023-10-12T15:11:25.973661",
    "codings": [
        {
            "code": "HP:0000001",
            "display": "All",
            "system": "https://hpo.jax.org",
            "version": "2023-09-01"
        },
        {
            "code": "HP:0000002",
            "display": "Abnormality of body height",
            "system": "https://hpo.jax.org",
            "version": "2023-09-01"
        },
        {
            "code": "HP:0000003",
            "display": "Multicystic kidney dysplasia",
            "system": "https://hpo.jax.org",
            "version": "2023-09-01"
        },
        ...
    ]
}

```
</details>

This ValueSet structure is *conceptually* equivalent to [FHIR ValueSet](https://hl7.org/fhir/R4/valueset.html), but syntactically slightly different.


----
## Rare Disease Query Module

Possible Query Criteria by which to query for RD Patients (all optional):

| Scope  | Name | Type |
| -----  | ----      | ---- |
| HPO       | Term   |  Coding from [Human Phenotype Ontology](https://hpo.jax.org) (ValueSet binding: <code>BASE_URL/api/coding/valuesets?uri=https://hpo.jax.org</code>) |
| Diagnosis | Category | Coding from [Orphanet Rare Disease Ontology](https://www.orpha.net) (ValueSet binding: <code>BASE_URL/api/coding/valuesets?uri=https://www.orpha.net</code>) |
| Variant   | Gene     | Coding from [HGNC Gene Set](https://www.genenames.org) (ValueSet binding: <code>BASE_URL/api/coding/valuesets?uri=https://www.genenames.org/</code>)  |
| Variant   | cDNA change | Coding where code is a [HGVS DNA Change](https://varnomen.hgvs.org/recommendations/DNA/) text pattern |
| Variant   | gDNA change | Coding where code is a [HGVS DNA Change](https://varnomen.hgvs.org/recommendations/DNA/) text pattern |
| Variant   | protein change | Coding where code is a [HGVS Protein Change](https://varnomen.hgvs.org/recommendations/protein/) text pattern (using **3-letter amino acid code** ??) |


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
{
    "case": {
        "externalId": {
            "value": "3e81e210-2fad-47c5-893e-e65200332b21"
        },
        "face2geneId": {
            "system": "https://www.face2gene.com/",
            "value": "9fd827fc-c7f8-4d80-ad86-c60c41f28e4a"
        },
        "gestaltMatcherId": {
            "system": "https://www.gestaltmatcher.org/",
            "value": "a81d078b-98a2-42f3-85c4-2959d3d34de1"
        },
        "id": "5c6573a7-74ac-45fa-b1d3-ee15a36a1d1b",
        "patient": {
            "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
        },
        "reason": {
            "id": "d2ff9f74-84af-43dc-9e7f-d83c607a4819"
        },
        "recordedOn": "2023-10-04",
        "referrer": {
            "name": "Dr. House"
        }
    },
    "diagnosis": {
        "category": {
            "code": "organ abnormality",
            "display": "organ abnormality",
            "system": "dnpm-dip/rd/diagnosis/category"
        },
        "id": "d2ff9f74-84af-43dc-9e7f-d83c607a4819",
        "patient": {
            "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
        },
        "recordedOn": "2023-10-04",
        "status": {
            "code": "partially-solved",
            "display": "Teilweise gelöst",
            "system": "dnpm-dip/rd/diagnosis/status"
        }
    },
    "hpoTerms": [
        {
            "id": "c8542899-b598-4c42-b6bc-922cd6d5758d",
            "patient": {
                "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
            },
            "value": {
                "code": "ORPHA:315311",
                "system": "https://hpo.jax.org"
            }
        },
        {
            "id": "b62db10c-4a13-4e0e-8a83-b4398bef3958",
            "patient": {
                "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
            },
            "value": {
                "code": "ORPHA:142684",
                "system": "https://hpo.jax.org"
            }
        },
        {
            "id": "6adce0ff-2eb1-431b-9df6-7a7f43c5ea29",
            "patient": {
                "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
            },
            "value": {
                "code": "ORPHA:315440",
                "system": "https://hpo.jax.org"
            }
        },
        {
            "id": "1c56c92d-fd8a-49ca-96f2-7d816b5f7e4d",
            "patient": {
                "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
            },
            "value": {
                "code": "ORPHA:253592",
                "system": "https://hpo.jax.org"
            }
        }
    ],
    "ngsReport": {
        "autozygosity": {
            "id": "c105ac8d-1f81-4513-90ab-dc8ed701b4f8",
            "patient": {
                "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
            },
            "value": 0.03219771385192871
        },
        "id": "e7ab1968-b8af-4039-a8e2-ebd0d0173aa4",
        "metaInfo": {
            "kit": "Kit...",
            "sequencingType": "Seq. Type"
        },
        "patient": {
            "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
        },
        "performingLab": {
            "name": "Lab name"
        },
        "recordedOn": "2023-10-04",
        "type": {
            "code": "array",
            "display": "Array",
            "system": "dnpm-dip/rd/ngs-report/type"
        },
        "variants": [
            {
                "acmgClass": {
                    "code": "likely-benign",
                    "display": "Likely benign",
                    "system": "https://www.acmg.net"
                },
                "cDNAChange": {
                    "code": "NG_012232.1(NM_004006.2):c.93+1G>T",
                    "system": "https://varnomen.hgvs.org"
                },
                "clinVarAccessionID": [
                    "bdc73f79-c81f-4847-8ee3-954907148bba"
                ],
                "deNovo": {
                    "code": "no",
                    "display": "No",
                    "system": "dnpm-dip/rd/variant/de-novo"
                },
                "gDNAChange": {
                    "code": "NC_000023.11:g.33344590_33344592=/dup",
                    "system": "https://varnomen.hgvs.org"
                },
                "gene": {
                    "code": "HGNC:20",
                    "display": "AARS1",
                    "system": "https://www.genenames.org/"
                },
                "id": "3cba188f-bf0e-40af-a954-4f294015434a",
                "iscnDescription": "Some ISCN description",
                "levelOfEvidence": "Level of evidence",
                "modeOfInheritance": {
                    "code": "dominant",
                    "display": "Dominant",
                    "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                },
                "patient": {
                    "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
                },
                "proteinChange": {
                    "code": "NP_003997.2:p.Val7dup",
                    "system": "https://varnomen.hgvs.org"
                },
                "pubMedID": {
                    "system": "https://pubmed.ncbi.nlm.nih.gov/",
                    "value": "-2066413175"
                },
                "significance": {
                    "code": "primary",
                    "display": "Primary",
                    "system": "dnpm-dip/rd/variant/significance"
                },
                "zygosity": {
                    "code": "hemi",
                    "display": "Hemizygous",
                    "system": "dnpm-dip/rd/variant/zygosity"
                }
            },
            {
                "acmgClass": {
                    "code": "likely-pathogenic",
                    "display": "Likely pathogenic",
                    "system": "https://www.acmg.net"
                },
                "cDNAChange": {
                    "code": "G_199t1:c.54G>H",
                    "system": "https://varnomen.hgvs.org"
                },
                "clinVarAccessionID": [
                    "a2bb7fca-b4fe-43d6-9e13-265a0dba5aa0"
                ],
                "deNovo": {
                    "code": "no",
                    "display": "No",
                    "system": "dnpm-dip/rd/variant/de-novo"
                },
                "gDNAChange": {
                    "code": "NC_000023.11:g.pter_qtersup",
                    "system": "https://varnomen.hgvs.org"
                },
                "gene": {
                    "code": "HGNC:20",
                    "display": "AARS1",
                    "system": "https://www.genenames.org/"
                },
                "id": "8c45efa4-b888-400a-99ea-44d24017ea80",
                "iscnDescription": "Some ISCN description",
                "levelOfEvidence": "Level of evidence",
                "modeOfInheritance": {
                    "code": "recessive",
                    "display": "Recessive",
                    "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                },
                "patient": {
                    "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
                },
                "proteinChange": {
                    "code": "NP_003997.1:p.(Gly56Ala^Ser^Cys)",
                    "system": "https://varnomen.hgvs.org"
                },
                "pubMedID": {
                    "system": "https://pubmed.ncbi.nlm.nih.gov/",
                    "value": "449154329"
                },
                "significance": {
                    "code": "candidate",
                    "display": "Candidate",
                    "system": "dnpm-dip/rd/variant/significance"
                },
                "zygosity": {
                    "code": "comp-het",
                    "display": "Comp. het",
                    "system": "dnpm-dip/rd/variant/zygosity"
                }
            },
            {
                "acmgClass": {
                    "code": "pathogenic",
                    "display": "Pathogenic",
                    "system": "https://www.acmg.net"
                },
                "cDNAChange": {
                    "code": "G_199t1:c.54G>H",
                    "system": "https://varnomen.hgvs.org"
                },
                "clinVarAccessionID": [
                    "cc6fce60-7f1b-4889-a71b-408443146392"
                ],
                "deNovo": {
                    "code": "yes",
                    "display": "Yes",
                    "system": "dnpm-dip/rd/variant/de-novo"
                },
                "gDNAChange": {
                    "code": "NC_000023.11:g.32343183dup",
                    "system": "https://varnomen.hgvs.org"
                },
                "gene": {
                    "code": "HGNC:49667",
                    "display": "ABALON",
                    "system": "https://www.genenames.org/"
                },
                "id": "7d08ab08-2614-41c3-93f9-53f0e7864d36",
                "iscnDescription": "Some ISCN description",
                "levelOfEvidence": "Level of evidence",
                "modeOfInheritance": {
                    "code": "X-linked",
                    "display": "X-linked",
                    "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                },
                "patient": {
                    "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
                },
                "proteinChange": {
                    "code": "LRG_199p1:p.Trp24Cys",
                    "system": "https://varnomen.hgvs.org"
                },
                "pubMedID": {
                    "system": "https://pubmed.ncbi.nlm.nih.gov/",
                    "value": "401170830"
                },
                "significance": {
                    "code": "incidental",
                    "display": "Incidental",
                    "system": "dnpm-dip/rd/variant/significance"
                },
                "zygosity": {
                    "code": "hemi",
                    "display": "Hemizygous",
                    "system": "dnpm-dip/rd/variant/zygosity"
                }
            },
            {
                "acmgClass": {
                    "code": "pathogenic",
                    "display": "Pathogenic",
                    "system": "https://www.acmg.net"
                },
                "cDNAChange": {
                    "code": "LRG_199t1:c.79_80delinsTT",
                    "system": "https://varnomen.hgvs.org"
                },
                "clinVarAccessionID": [
                    "569182a8-e2fe-4e01-86e6-cac1c1e823a7"
                ],
                "deNovo": {
                    "code": "yes",
                    "display": "Yes",
                    "system": "dnpm-dip/rd/variant/de-novo"
                },
                "gDNAChange": {
                    "code": "NC_000023.11:g.(31060227_31100351)_(33274278_33417151)dup",
                    "system": "https://varnomen.hgvs.org"
                },
                "gene": {
                    "code": "HGNC:17929",
                    "display": "AADAT",
                    "system": "https://www.genenames.org/"
                },
                "id": "ccb73f7a-818d-4698-8e4b-07a9f054f08b",
                "iscnDescription": "Some ISCN description",
                "levelOfEvidence": "Level of evidence",
                "modeOfInheritance": {
                    "code": "dominant",
                    "display": "Dominant",
                    "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                },
                "patient": {
                    "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
                },
                "proteinChange": {
                    "code": "NP_003997.1:p.(Gly56Ala^Ser^Cys)",
                    "system": "https://varnomen.hgvs.org"
                },
                "pubMedID": {
                    "system": "https://pubmed.ncbi.nlm.nih.gov/",
                    "value": "709123527"
                },
                "significance": {
                    "code": "candidate",
                    "display": "Candidate",
                    "system": "dnpm-dip/rd/variant/significance"
                },
                "zygosity": {
                    "code": "homozygous",
                    "display": "Homozygous",
                    "system": "dnpm-dip/rd/variant/zygosity"
                }
            },
            {
                "acmgClass": {
                    "code": "unclear",
                    "display": "Unclear",
                    "system": "https://www.acmg.net"
                },
                "cDNAChange": {
                    "code": "G_199t1:c.54G>H",
                    "system": "https://varnomen.hgvs.org"
                },
                "clinVarAccessionID": [
                    "9b065175-8ad9-4ce4-a9a7-55682dc1a4a6"
                ],
                "deNovo": {
                    "code": "from-father",
                    "display": "Transmitted from father",
                    "system": "dnpm-dip/rd/variant/de-novo"
                },
                "gDNAChange": {
                    "code": "NC_000023.11:g.33344590_33344592=/dup",
                    "system": "https://varnomen.hgvs.org"
                },
                "gene": {
                    "code": "HGNC:17929",
                    "display": "AADAT",
                    "system": "https://www.genenames.org/"
                },
                "id": "46f3a8ec-4816-48fe-a49d-358058f55345",
                "iscnDescription": "Some ISCN description",
                "levelOfEvidence": "Level of evidence",
                "modeOfInheritance": {
                    "code": "recessive",
                    "display": "Recessive",
                    "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                },
                "patient": {
                    "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
                },
                "proteinChange": {
                    "code": "NP_003997.2:p.Val7dup",
                    "system": "https://varnomen.hgvs.org"
                },
                "pubMedID": {
                    "system": "https://pubmed.ncbi.nlm.nih.gov/",
                    "value": "-376749163"
                },
                "significance": {
                    "code": "incidental",
                    "display": "Incidental",
                    "system": "dnpm-dip/rd/variant/significance"
                },
                "zygosity": {
                    "code": "homozygous",
                    "display": "Homozygous",
                    "system": "dnpm-dip/rd/variant/zygosity"
                }
            }
        ]
    },
    "patient": {
        "birthDate": "1961-07-26",
        "gender": {
            "code": "male",
            "display": "Männlich",
            "system": "Gender"
        },
        "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
    },
    "therapy": {
        "id": "b57f1164-68bb-4428-8a85-a2c6abde5f0d",
        "notes": "Notes on the therapy...",
        "patient": {
            "id": "210a1de0-02a8-44b5-8399-db9cc4c42057"
        }
    }
}
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




