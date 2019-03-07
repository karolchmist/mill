package mill
package contrib.avrolib

import coursier.{Cache, MavenRepository}
import coursier.core.Version
import mill.define.Sources
import mill.api.PathRef
import mill.scalalib.Lib.resolveDependencies
import mill.scalalib._
import mill.api.Loose

trait AvroModule extends ScalaModule {

  override def generatedSources = T { super.generatedSources() :+ compileAvro() }

  override def ivyDeps = T {
    super.ivyDeps() ++ Agg(
      ivy"com.julianpeeters::avrohugger-core:${avroHuggerVersion()}",
      ivy"com.julianpeeters::avrohugger-filesorter:${avroHuggerVersion()}",
      ivy"com.julianpeeters::avrohugger-tools:${avroHuggerVersion()}"
    )
  }

  def avroHuggerVersion: T[String] // 1.0.0-RC15

  def avroSources: Sources = T.sources {
    millSourcePath / 'avro
  }

  def avroOptions: T[String] = T {
      Seq.empty.mkString(",")
  }

  def avroClasspath: T[Loose.Agg[PathRef]] = T {
    resolveDependencies(
      Seq(
        Cache.ivy2Local,
        MavenRepository("https://repo1.maven.org/maven2")
      ),
      Lib.depToDependency(_, "2.12.4"),
      Seq(
        ivy"com.julianpeeters::avrohugger-core:${avroHuggerVersion()}",
        ivy"com.julianpeeters::avrohugger-filesorter:${avroHuggerVersion()}",
        ivy"com.julianpeeters::avrohugger-tools:${avroHuggerVersion()}"
      )
    )
  }
  
  def compileAvro: T[PathRef] = T.persistent {
    AvroWorkerApi.avroWorker
      .compile(
        avroClasspath().map(_.path),
        avroSources().map(_.path),
        avroOptions(),
        T.ctx().dest)
  }
}
