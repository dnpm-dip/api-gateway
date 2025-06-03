package de.dnpm.dip.rest.api


import scala.util.Random
import java.time.LocalDate
import de.ekut.tbi.generators.Gen
import de.dnpm.dip.model.{
  Id,
  PatientRecord
}
import de.dnpm.dip.service.mvh.{
  Consent,
  ModelProjectConsent,
  Submission,
  TransferTAN,
}
import de.dnpm.dip.service.DataUpload
import play.api.libs.json.{
  JsPath,
  OWrites
}
import play.api.libs.functional.syntax._


trait FakeDataGen[T <: PatientRecord]
{

  protected implicit val rnd: Random =
    new Random

  protected implicit def genDataUpload(
    implicit genT: Gen[T]
  ): Gen[DataUpload[T]] =
    for {
      typ    <- Gen.`enum`(Submission.Type)
      ttan   <- Gen.uuidStrings.map(Id[TransferTAN](_))
      record <- Gen.of[T]
      metadata =
        Submission.Metadata(
          typ,
          ttan,
          ModelProjectConsent(
            "Patient Info TE Consent MVGenomSeq vers01",
            Some(LocalDate.now),
            ModelProjectConsent.Purpose.values
              .toList
              .map(
                Consent.Provision(
                  LocalDate.now,
                  _,
                  Consent.Provision.Type.Permit 
                )
              )
          ),
          Some(List.empty)
        )
    } yield DataUpload(
      record,
      Some(metadata)
    )

//  protected implicit def writesDataUpload[T <: PatientRecord: OWrites]: OWrites[DataUpload[T]] =
  protected implicit def writesDataUpload(
    implicit wt: OWrites[T]
  ): OWrites[DataUpload[T]] =
    (
      JsPath.write[T] and
      (JsPath \ "metadata").writeNullable[Submission.Metadata]
    )(
      unlift(DataUpload.unapply[T](_))
    )

}


/*
trait FakeDataGen
{

  protected implicit val rnd: Random =
    new Random


  protected implicit def genDataUpload[T <: PatientRecord: Gen]: Gen[DataUpload[T]] =
    for {
      typ    <- Gen.`enum`(Submission.Type)
      ttan   <- Gen.uuidStrings.map(Id[TransferTAN](_))
      record <- Gen.of[T]
      metadata =
        Submission.Metadata(
          typ,
          ttan,
          ModelProjectConsent(
            "Patient Info TE Consent MVGenomSeq vers01",
            Some(LocalDate.now),
            ModelProjectConsent.Purpose.values
              .toList
              .map(
                Consent.Provision(
                  LocalDate.now,
                  _,
                  Consent.Provision.Type.Permit 
                )
              )
          ),
          None
        )
    } yield DataUpload(
      record,
      Some(metadata)
    )

  protected implicit def writesDataUpload[T <: PatientRecord: OWrites]: OWrites[DataUpload[T]] =
    (
      JsPath.write[T] and
      (JsPath \ "metadata").writeNullable[Submission.Metadata]
    )(
      unlift(DataUpload.unapply[T](_))
    )

}
*/
