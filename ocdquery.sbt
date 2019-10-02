import sbt._
import Settings._

lazy val root = project.root
  .setName("ocdquery")
  .setDescription("OCD Query build")
  .configureRoot
  .noPublish
  .aggregate(core, tests)

lazy val core = project.from("core")
  .setName("ocdquery-core")
  .setDescription("Library for generating Doobie fragments out of higher-kinded data")
  .setInitialImport()
  .configureModule
  .configureTests()
  .publish
  .settings(Compile / resourceGenerators += task[Seq[File]] {
    val file = (Compile / resourceManaged).value / "ocdquery-version.conf"
    IO.write(file, s"version=${version.value}")
    Seq(file)
  })

lazy val tests = project.from("tests")
  .setName("ocdquery-tests")
  .setDescription("Integration tests of ocdquery")
  .setInitialImport()
  .configureModule
  .configureIntegrationTests(requiresFork = true)
  .noPublish
  .dependsOn(core)

lazy val readme = scalatex.ScalatexReadme(
    projectId = "readme",
    wd        = file(""),
    url       = "https://github.com/scalalandio/ocdquery/tree/master",
    source    = "Readme"
  )
  .configureModule
  .noPublish
  .enablePlugins(GhpagesPlugin)
  .settings(
    siteSourceDirectory := target.value / "scalatex",
    git.remoteRepo := "git@github.com:scalalandio/ocdquery.git",
    Jekyll / makeSite / includeFilter := new FileFilter { def accept(p: File) = true }
  )

addCommandAlias("fullTest", ";test;it:test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;it:test;coverageReport;coverageAggregate;scalastyle")
