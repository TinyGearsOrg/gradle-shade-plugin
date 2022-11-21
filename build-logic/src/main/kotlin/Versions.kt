object Versions {
    const val SHADE:         String = "0.0.1"
    const val SNAPSHOT_NAME: String = "main"
    const val JVM_TARGET:    String = "1.8"

    fun currentOrSnapshot(): String {
        if (System.getProperty("snapshot")?.toBoolean() == true) {
            return "$SNAPSHOT_NAME-SNAPSHOT"
        }
        return SHADE
    }
}
