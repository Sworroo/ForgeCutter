package ru.justagod.mincer.util

import org.zeroturnaround.zip.ZipUtil
import ru.justagod.mincer.Mincer
import ru.justagod.mincer.control.MincerFS
import ru.justagod.mincer.control.MincerResultType
import ru.justagod.mincer.util.recursiveness.ByteArraySource
import ru.justagod.mincer.util.recursiveness.MincerFallbackFS
import ru.justagod.mincer.util.recursiveness.MincerTreeFS
import ru.justagod.mincer.util.recursiveness.MincerZipFS
import ru.justagod.model.ClassTypeReference
import java.io.File
import java.util.concurrent.*

object MincerUtils {

    fun processFolder(panel: Mincer, folder: File, threadsCount: Int = 10, listener: ProcessingListener = ProcessingListener()) {
        val executor = Executors.newFixedThreadPool(threadsCount)
        MincerUtils.processFolder(panel, executor, folder, threadsCount, listener)
        executor.shutdown()
    }
    fun processFolder(panel: Mincer, executor: ExecutorService, folder: File, threadsCount: Int = 10, listener: ProcessingListener = ProcessingListener()) {
        if (threadsCount < 1) error("threadsCount < 1")

        val completer = ExecutorCompletionService<Unit>(executor)
        do {
            val targetClasses = panel.targetClasses()
            val queue = if (targetClasses == null) getClassesInFolder(folder) else getFilesFromClasses(folder, targetClasses)

            listener.newPass(queue.size)
            val futures = arrayListOf<Future<*>>()
            try {
                repeat(threadsCount) {
                    futures += completer.submit {
                        var f = queue.poll()
                        while (f != null) {
                            if (Thread.interrupted()) return@submit

                            val name = f.absoluteFile.relativeTo(folder.absoluteFile)

                            val ref = ClassTypeReference.fromFilePath(name.path)
                            if (listener.filter(ref)) {
                                val result = panel.advance(ref).onModification {
                                    f.writeBytes(it)
                                }.onDeletion {
                                    f.delete()
                                }

                                listener.processed(ref, result.type)
                            } else {
                                listener.processed(ref, MincerResultType.SKIPPED)
                            }


                            f = queue.poll()
                        }
                    }
                }

                repeat(threadsCount) {
                    completer.take()
                }
                futures.forEach { it.get() }
            } catch (e: Exception) {
                futures.forEach { it.cancel(true) }
                throw e
            }
        } while (panel.endIteration())
        listener.newPass(0)
        folder.walkBottomUp().forEach { if (it.isDirectory) it.delete() }
    }

    fun getFilesFromClasses(root: File, classes: List<ClassTypeReference>): LinkedBlockingQueue<File> {
        val queue = LinkedBlockingQueue<File>()

        for (clazz in classes) {
            val file = root.resolve(clazz.internalName+".class")

            if (file.exists()) queue += file
        }

        return queue
    }

    fun getClassesInFolder(folder: File): LinkedBlockingQueue<File> {
        val queue = LinkedBlockingQueue<File>()
        folder.walkTopDown()
            .filter { it.path.endsWith(".class") && it.isFile }
            .forEach { queue += it }

        return queue
    }

    fun readZip(file: File): HashMap<String, ByteArraySource> {
        val result = hashMapOf<String, ByteArraySource>()
        ZipUtil.iterate(file) { input, entry ->
            if (!entry.isDirectory)
                result[entry.name] = ByteArraySource(entry.name, input!!.readBytes(estimatedSize = 256))
        }

        return result

    }

    class ProcessingListener {

        private var newPass: ((toProcess: Int) -> Unit)? = null
        private var onProcessed: ((name: ClassTypeReference, result: MincerResultType) -> Unit)? = null
        private var filter: ((ClassTypeReference) -> Boolean)? = null


        fun filter(name: ClassTypeReference) = filter?.invoke(name) ?: true

        fun newPass(toProcess: Int) = newPass?.invoke(toProcess)

        fun processed(name: ClassTypeReference, result: MincerResultType) = onProcessed?.invoke(name, result)

        fun onFilter(block: (ClassTypeReference) -> Boolean): ProcessingListener {
            filter = block
            return this
        }

        fun onProcessed(block: (name: ClassTypeReference, result: MincerResultType) -> Unit): ProcessingListener {
            onProcessed = block
            return this
        }

        fun onNewPass(block: (toProcess: Int) -> Unit): ProcessingListener {
            newPass = block
            return this
        }
    }

}