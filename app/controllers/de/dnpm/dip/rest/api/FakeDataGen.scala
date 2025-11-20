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
  BroadConsent,
  Submission,
  TransferTAN
}
import de.dnpm.dip.service.DataUpload


trait FakeDataGen[T <: PatientRecord]
{

  protected implicit val rnd: Random =
    new Random

  protected implicit def genDataUpload(
    implicit genT: Gen[T]
  ): Gen[DataUpload[T]] =
    for {
      ttan <- Gen.listOf(64, Gen.oneOf("0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F")).map(_.mkString)

      record <- Gen.of[T]

      reasonConsentMissing <- Gen.`enum`(BroadConsent.ReasonMissing)

      consentDate =
        record.getCarePlans
          .map(_.issuedOn)
          .minOption
          .map(_ minusWeeks 2)
          .getOrElse(LocalDate.now)

      metadata =
        Submission.Metadata(
          Submission.Type.Test,
          Id[TransferTAN](ttan),
          ModelProjectConsent(
            "Patient Info TE Consent MVGenomSeq vers01",
            Some(consentDate minusDays 1),
            ModelProjectConsent.Purpose.values
              .toList
              .map(
                Consent.Provision(
                  consentDate,
                  _,
                  Consent.Provision.Type.Permit 
                )
              )
          ),
          None,
          Some(reasonConsentMissing)
        )
    } yield DataUpload(
      record,
      Some(metadata)
    )

}
