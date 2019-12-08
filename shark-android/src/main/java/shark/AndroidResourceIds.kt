package shark

import shark.HprofRecord.HeapDumpRecord.ObjectRecord.PrimitiveArrayDumpRecord.IntArrayDump

// TODO Better name?
class AndroidResourceIds private constructor(
  private val resourceIds: IntArray,
  private val names: Array<String>
) {

  // function to get it by int, uses binary search

  operator fun get(id: Int): String? {
    val indexOfId = resourceIds.binarySearch(id)
    return if (indexOfId >= 0) {
      names[indexOfId]
    } else {
      null
    }
  }

  companion object {
    @Volatile
    @JvmStatic
    private var holderField: AndroidResourceIds? = null

    val isSet
      get() = holderField != null

    /**
     * TODO Or maybe we expect something that calls us to register
     * increasing ids one by one and then save (ie put in map)
     *
     * Maybe not in initial impl, but next step.
     */
    fun saveResourceIds(idToNamePairs: List<Pair<Int, String>>) {
      check(holderField == null) {
        "holderField should only be set once, check isSet first"
      }
      val resourceIds = idToNamePairs.map { it.first }
          .toIntArray()
      val names = idToNamePairs.map { it.second }
          .toTypedArray()
      holderField = AndroidResourceIds(resourceIds, names)
    }

    fun readResourceIds(graph: HeapGraph): AndroidResourceIds? {
      return graph.context.getOrPut(AndroidResourceIds::class.java.name) {
        val className = AndroidResourceIds::class.java.name
        val holderClass = graph.findClassByName(className)
        holderClass?.let {
          val holderField = holderClass["holderField"]!!
          holderField.valueAsInstance?.let { instance ->
            val resourceIds =
              (instance[className, "resourceIds"]!!.valueAsPrimitiveArray!!.readRecord() as IntArrayDump).array
            val names = instance[className, "names"]!!.valueAsObjectArray!!.readElements()
                .map { it.readAsJavaString()!! }
                .toList()
                .toTypedArray()
            AndroidResourceIds(resourceIds, names)
          }
        }
      }
    }
  }

}