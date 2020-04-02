package no.nav.pdlsf

sealed class ServerStates {
    object IsOk : ServerStates()
    object KafkaIssues : ServerStates()
    object KafkaConsumerIssues : ServerStates()
    object ProtobufIssues : ServerStates()
}

object ServerState {
    var state: ServerStates = ServerStates.IsOk

    fun isOk(): Boolean = state == ServerStates.IsOk
    fun reset() { state = ServerStates.IsOk
    }
}
