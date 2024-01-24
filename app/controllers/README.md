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
## MTB Query Module

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
## Rare Disease Query Module

Possible Query Criteria by which to query for RD Patients (all optional):

| Block  | Attribute Name | Type |
| -----  | ----      | ---- |
| HPO       | Term   |  Coding from [Human Phenotype Ontology](https://hpo.jax.org) (ValueSet binding: <code>BASE_URL/api/coding/valuesets?uri=https://hpo.jax.org</code>) |
| Diagnosis | Category | Coding from [Orphanet Rare Disease Ontology](https://www.orpha.net) (ValueSet binding: <code>BASE_URL/api/coding/valuesets?uri=https://www.orpha.net</code>) |
| Variant   | Gene     | Coding from [HGNC Gene Set](https://www.genenames.org) (ValueSet binding: <code>BASE_URL/api/coding/valuesets?uri=https://www.genenames.org/</code>)  |
| Variant   | cDNA change | Coding where code is a [HGVS DNA Change](https://varnomen.hgvs.org/recommendations/DNA/) text pattern |
| Variant   | gDNA change | Coding where code is a [HGVS DNA Change](https://varnomen.hgvs.org/recommendations/DNA/) text pattern |
| Variant   | protein change | Coding where code is a [HGVS Protein Change](https://varnomen.hgvs.org/recommendations/protein/) text pattern (using **3-letter amino acid code** ??) |


### Submit Query

```
POST /api/rd/queries
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
    "diagnoses" : [
      {
        "code" : "endocrine",
      }
    ],
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

#### Alternative Query submission

```
POST /api/rd/queries?mode={local|federated}
```

**Request Body**
<details>
<summary>Partial RD Query Criteria object</summary>

```javascript
{
  "criteria": {
    "diagnoses" : [
      {
        "code" : "endocrine"
      }
    ],
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


### Get Open Queries

```
GET /api/rd/queries
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
  ]
}

```
</details>


### Get Query by ID

```
GET /api/rd/queries/{Query-ID}

GET /api/rd/queries?id={Query-ID}
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
GET /api/rd/queries/{Query-ID}/summary
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
GET /api/rd/queries/{Query-ID}/patient-matches

GET /api/rd/queries/{Query-ID}/patients
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
GET /api/rd/queries/{Query-ID}/patient-record/{Patient-ID}
```

**Response**
<details>
<summary>RD Patient Record</summary>
```javascript
{
    "patient": {
        "birthDate": "1978-01-17",
        "gender": {
            "code": "other",
            "display": "Divers",
            "system": "Gender"
        },
        "id": "ba25035b-26db-4d52-87ad-312a27bc8079",
        "managingSite": {
            "code": "UKX",
            "display": "Fake Site",
            "system": "dnpm/site"
        }
    },
    "case": {
        "externalId": {
            "value": "3ff22a20-7a85-4494-b387-e81051561875"
        },
        "gestaltMatcherId": {
            "system": "https://www.gestaltmatcher.org/",
            "value": "b92e7f22-5bd6-42d6-af9e-6660f80e4ba0"
        },
        "id": "673caf7d-b182-4c70-aab6-771e9b002608",
        "patient": {
            "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
        },
        "reason": {
            "id": "324edd16-793f-4574-814f-89e685442aa2"
        },
        "recordedOn": "2023-11-02",
        "referrer": {
            "name": "Dr. House"
        }
    },
    "diagnosis": {
        "categories": [
            {
                "code": "ORPHA:984",
                "display": "Lungenagenesie",
                "system": "https://www.orpha.net",
                "version": "4.3"
            }
        ],
        "id": "324edd16-793f-4574-814f-89e685442aa2",
        "onsetAge": {
            "unit": "a",
            "value": 18
        },
        "patient": {
            "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
        },
        "prenatal": false,
        "recordedOn": "2023-11-02",
        "status": {
            "code": "partially-solved",
            "display": "Teilweise gelöst",
            "system": "dnpm-dip/rd/diagnosis/status"
        }
    },
    "hpoTerms": [
        {
            "id": "cde829a8-30b4-4701-bbc8-0c2df3c80663",
            "patient": {
                "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
            },
            "value": {
                "code": "HP:0100871",
                "display": "Abnormal palm morphology",
                "system": "https://hpo.jax.org",
                "version": "2023-09-01"
            }
        },
        {
            "id": "d8dc300e-b9ca-4b95-81da-1531aff0d9bb",
            "patient": {
                "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
            },
            "value": {
                "code": "HP:0100869",
                "display": "Palmar telangiectasia",
                "system": "https://hpo.jax.org",
                "version": "2023-09-01"
            }
        },
        {
            "id": "80f665cf-2eac-4fd0-9316-d001ca33033b",
            "patient": {
                "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
            },
            "value": {
                "code": "HP:0100863",
                "display": "Aplasia of the femoral neck",
                "system": "https://hpo.jax.org",
                "version": "2023-09-01"
            }
        },
        {
            "id": "2f3eeffe-b16c-4612-b37c-556c8fac4c42",
            "patient": {
                "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
            },
            "value": {
                "code": "HP:0100869",
                "display": "Palmar telangiectasia",
                "system": "https://hpo.jax.org",
                "version": "2023-09-01"
            }
        }
    ],
    "ngsReports": [
        {
            "autozygosity": {
                "id": "6d13b450-ec6c-43c7-9c6a-7c9bc95c08ce",
                "patient": {
                    "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
                },
                "value": 0.9874248504638672
            },
            "familyControls": {
                "code": "trio",
                "display": "trio",
                "system": "dnpm-dip/rd/ngs-report/family-control-level"
            },
            "id": "e1df79f5-6888-4f01-afae-35e0edaa8c62",
            "metaInfo": {
                "kit": "Kit...",
                "sequencingType": "Seq. Type"
            },
            "patient": {
                "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
            },
            "performingLab": {
                "name": "Lab name"
            },
            "recordedOn": "2023-11-02",
            "type": {
                "code": "panel",
                "display": "Panel",
                "system": "dnpm-dip/rd/ngs-report/type"
            },
            "variants": [
                {
                    "acmgClass": {
                        "code": "likely-benign",
                        "display": "Likely benign",
                        "system": "https://www.acmg.net/class"
                    },
                    "acmgCriteria": [
                        {
                            "code": "PP5",
                            "display": "PP5",
                            "system": "https://www.acmg.net/criteria"
                        },
                        {
                            "code": "BP6",
                            "display": "BP6",
                            "system": "https://www.acmg.net/criteria"
                        },
                        {
                            "code": "BP2",
                            "display": "BP2",
                            "system": "https://www.acmg.net/criteria"
                        }
                    ],
                    "cDNAChange": {
                        "code": "NG_012232.1(NM_004006.2):c.93+1G>T",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "clinVarAccessionID": [
                        "1ab4bfd4-813c-4af2-8155-1e09104e2a18"
                    ],
                    "gDNAChange": {
                        "code": "NC_000023.11:g.32343183dup",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "gene": {
                        "code": "HGNC:51526",
                        "display": "apoptosis associated transcript in bladder cancer",
                        "system": "https://www.genenames.org/"
                    },
                    "id": "e40aca00-2374-408c-9e43-72c05245a8ec",
                    "iscnDescription": "Some ISCN description",
                    "levelOfEvidence": "Level of evidence",
                    "modeOfInheritance": {
                        "code": "mitochondrial",
                        "display": "Mitochondrial",
                        "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                    },
                    "patient": {
                        "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
                    },
                    "proteinChange": {
                        "code": "LRG_199p1:p.Trp24=/Cys",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "pubMedID": {
                        "system": "https://pubmed.ncbi.nlm.nih.gov/",
                        "value": "195855529"
                    },
                    "segregationAnalysis": {
                        "code": "from-father",
                        "display": "Transmitted from father",
                        "system": "dnpm-dip/rd/variant/segregation-analysis"
                    },
                    "significance": {
                        "code": "candidate",
                        "display": "Candidate",
                        "system": "dnpm-dip/rd/variant/significance"
                    },
                    "zygosity": {
                        "code": "heterozygous",
                        "display": "Heterozygous",
                        "system": "dnpm-dip/rd/variant/zygosity"
                    }
                },
                {
                    "acmgClass": {
                        "code": "likely-benign",
                        "display": "Likely benign",
                        "system": "https://www.acmg.net/class"
                    },
                    "acmgCriteria": [
                        {
                            "code": "BA1",
                            "display": "BA1",
                            "system": "https://www.acmg.net/criteria"
                        },
                        {
                            "code": "PM6",
                            "display": "PM6",
                            "system": "https://www.acmg.net/criteria"
                        },
                        {
                            "code": "PP4",
                            "display": "PP4",
                            "system": "https://www.acmg.net/criteria"
                        }
                    ],
                    "cDNAChange": {
                        "code": "NG_012232.1(NM_004006.2):c.93+1G>T",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "clinVarAccessionID": [
                        "efad7330-1fe1-4f0f-867a-e9f6d652dca0"
                    ],
                    "gDNAChange": {
                        "code": "NC_000023.11:g.pter_qtersup",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "gene": {
                        "code": "HGNC:20",
                        "display": "alanyl-tRNA synthetase 1",
                        "system": "https://www.genenames.org/"
                    },
                    "id": "7193507f-c8b5-4a05-868e-cbdcb9e1644c",
                    "iscnDescription": "Some ISCN description",
                    "levelOfEvidence": "Level of evidence",
                    "modeOfInheritance": {
                        "code": "dominant",
                        "display": "Dominant",
                        "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                    },
                    "patient": {
                        "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
                    },
                    "proteinChange": {
                        "code": "LRG_199p1:p.Trp24Ter (p.Trp24*)",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "pubMedID": {
                        "system": "https://pubmed.ncbi.nlm.nih.gov/",
                        "value": "1216365734"
                    },
                    "segregationAnalysis": {
                        "code": "from-father",
                        "display": "Transmitted from father",
                        "system": "dnpm-dip/rd/variant/segregation-analysis"
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
                },
                {
                    "acmgClass": {
                        "code": "pathogenic",
                        "display": "Pathogenic",
                        "system": "https://www.acmg.net/class"
                    },
                    "acmgCriteria": [
                        {
                            "code": "BS1",
                            "display": "BS1",
                            "system": "https://www.acmg.net/criteria"
                        }
                    ],
                    "cDNAChange": {
                        "code": "LRG_199t1:c.79_80delinsTT",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "clinVarAccessionID": [
                        "186ae3d7-3b0f-4a02-9f07-20d2a275be44"
                    ],
                    "gDNAChange": {
                        "code": "NC_000023.11:g.(31060227_31100351)_(33274278_33417151)dup",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "gene": {
                        "code": "HGNC:20",
                        "display": "alanyl-tRNA synthetase 1",
                        "system": "https://www.genenames.org/"
                    },
                    "id": "b29636e2-0b1a-4311-8d34-29a82956e124",
                    "iscnDescription": "Some ISCN description",
                    "levelOfEvidence": "Level of evidence",
                    "modeOfInheritance": {
                        "code": "unclear",
                        "display": "Unclear",
                        "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                    },
                    "patient": {
                        "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
                    },
                    "proteinChange": {
                        "code": "LRG_199p1:p.Trp24Cys",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "pubMedID": {
                        "system": "https://pubmed.ncbi.nlm.nih.gov/",
                        "value": "282670945"
                    },
                    "segregationAnalysis": {
                        "code": "from-father",
                        "display": "Transmitted from father",
                        "system": "dnpm-dip/rd/variant/segregation-analysis"
                    },
                    "significance": {
                        "code": "primary",
                        "display": "Primary",
                        "system": "dnpm-dip/rd/variant/significance"
                    },
                    "zygosity": {
                        "code": "heterozygous",
                        "display": "Heterozygous",
                        "system": "dnpm-dip/rd/variant/zygosity"
                    }
                },
                {
                    "acmgClass": {
                        "code": "likely-benign",
                        "display": "Likely benign",
                        "system": "https://www.acmg.net/class"
                    },
                    "acmgCriteria": [
                        {
                            "code": "PM2",
                            "display": "PM2",
                            "system": "https://www.acmg.net/criteria"
                        },
                        {
                            "code": "BP5",
                            "display": "BP5",
                            "system": "https://www.acmg.net/criteria"
                        }
                    ],
                    "cDNAChange": {
                        "code": "G_199t1:c.54G>H",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "clinVarAccessionID": [
                        "1a73be31-b003-476b-8039-7ecb6387bebb"
                    ],
                    "gDNAChange": {
                        "code": "NC_000023.10:g.33038255C>A",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "gene": {
                        "code": "HGNC:21",
                        "display": "apoptosis associated tyrosine kinase",
                        "system": "https://www.genenames.org/"
                    },
                    "id": "2857a2c8-18dd-4e8a-a027-e208f035f3a8",
                    "iscnDescription": "Some ISCN description",
                    "levelOfEvidence": "Level of evidence",
                    "modeOfInheritance": {
                        "code": "mitochondrial",
                        "display": "Mitochondrial",
                        "system": "dnpm-dip/rd/variant/mode-of-inheritance"
                    },
                    "patient": {
                        "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
                    },
                    "proteinChange": {
                        "code": "NP_003997.2:p.Val7dup",
                        "system": "https://varnomen.hgvs.org"
                    },
                    "pubMedID": {
                        "system": "https://pubmed.ncbi.nlm.nih.gov/",
                        "value": "102034715"
                    },
                    "segregationAnalysis": {
                        "code": "from-mother",
                        "display": "Transmitted from mother",
                        "system": "dnpm-dip/rd/variant/segregation-analysis"
                    },
                    "significance": {
                        "code": "incidental",
                        "display": "Incidental",
                        "system": "dnpm-dip/rd/variant/significance"
                    },
                    "zygosity": {
                        "code": "comp-het",
                        "display": "Comp. het",
                        "system": "dnpm-dip/rd/variant/zygosity"
                    }
                }
            ]
        }
    ],
    "therapy": {
        "id": "d46030c2-52ad-4a44-88b7-b3517c43acb5",
        "notes": "Notes on the therapy...",
        "patient": {
            "id": "ba25035b-26db-4d52-87ad-312a27bc8079"
        }
    }
}
```
</details>


### Update Query

```
PUT /api/rd/queries/{Query-ID}
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

#### Alternative Query Update
```
PUT /api/rd/queries/{Query-ID}?mode={local|federated}
```
**Request Body**
<details>
<summary>Partial Query Update object</summary>
```javascript
// Optional here, only if query criteria changed
{
  "criteria": {   
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



