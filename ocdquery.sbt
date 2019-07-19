import sbt._
import Settings._

lazy val root = project.root
  .setName("ocdquery")
  .setDescription("OCD Query build")
  .configureRoot
  .aggregate(core)

lazy val core = project.from("core")
  .setName("core")
  .setDescription("Library for generating Doobie fragments out of higher-kinded data")
  .setInitialImport()
  .configureModule
  .configureTests()
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "ocdquery-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

addCommandAlias("fullTest", ";test;fun:test;it:test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
addCommandAlias("relock", ";unlock;reload;update;lock")
