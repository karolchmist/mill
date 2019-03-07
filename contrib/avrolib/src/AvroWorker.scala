package mill
package contrib.avrolib

import java.io.File
import java.lang.reflect.Modifier

import mill.api.Ctx
import mill.api.PathRef
import mill.modules.Jvm

class AvroWorker {
  private var avroInstanceCache = Option.empty[(Long, AvroWorkerApi)]

  private def avro(avroClasspath: Agg[os.Path])(implicit ctx: Ctx) = {
    val classloaderSig = avroClasspath.map(p => p.toString().hashCode + os.mtime(p)).sum
    avroInstanceCache match {
      case Some((sig, instance)) if sig == classloaderSig => instance
      case _ =>
        val instance = new AvroWorkerApi {
          override def compileAvro(source: File, avroOptions: String, generatedDirectory: File): Unit = {
            Jvm.runSubprocess("avrohugger.tool.Main", avroClasspath, mainArgs = Seq("generate", "schema", source.getCanonicalPath, generatedDirectory.getCanonicalPath))
          }
        }
        avroInstanceCache = Some((classloaderSig, instance))
        instance
    }
  }


  def compile(avroClasspath: Agg[os.Path], avroSources: Seq[os.Path], avroOptions: String, dest: os.Path)
             (implicit ctx: mill.api.Ctx): mill.api.Result[PathRef] = {
    val compiler = avro(avroClasspath)

    def compileAvroDir(inputDir: os.Path) {
      // ls throws if the path doesn't exist
      if (inputDir.toIO.exists) {
        os.walk(inputDir).filter(_.last.matches(".*.avsc"))
          .foreach { proto =>
            compiler.compileAvro(proto.toIO, avroOptions, dest.toIO)
          }
      }
    }
    avroSources.foreach(compileAvroDir)
    mill.api.Result.Success(PathRef(dest))
  }
}

trait AvroWorkerApi {
  def compileAvro(source: File, avroOptions: String, generatedDirectory: File)
}

object AvroWorkerApi {
  def avroWorker = new AvroWorker()
}
