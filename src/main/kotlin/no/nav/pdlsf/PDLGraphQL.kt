package no.nav.pdlsf

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.stringify
import mu.KotlinLogging
import org.http4k.core.Method
import org.http4k.core.Status

private val log = KotlinLogging.logger { }
private const val GRAPHQL_QUERY = "/graphql/query.graphql"

@ImplicitReflectionSerializer
private fun executeGraphQlQuery(
    query: String,
    variables: Map<String, Any>
): QueryResponseBase = Http.client.invoke(
        org.http4k.core.Request(Method.POST, ParamsFactory.p.pdlGraphQlUrl)
                .header("Tema", "GEN")
                .header("Authorization", "Bearer ${getStsToken()}")
                .header("Nav-Consumer-Token", "Bearer ${getStsToken()}")
                .header("Cache-Control", "no-cache")
                .header("Content-Type", "application/json")
                .body(json.stringify(QueryRequest(
                        query = query,
                        variables = QueryRequest.Variables(variables)
                )))
).let { response ->
    when (response.status) {
        Status.OK -> {
            log.debug { response.bodyString() }
            runCatching {
                val queryResponse = QueryResponse.fromJson(response.bodyString())
                val result = if (queryResponse is QueryResponse) {
                    queryResponse.errors?.let { errors -> QueryErrorResponse(errors) } ?: queryResponse
                } else {
                    queryResponse
                }
                result
            }.getOrDefault(InvalidQueryResponse)
        }
        else -> {
            log.error { "Request failed - ${response.status.description}(${response.status.code})" }
            InvalidQueryResponse
        }
    }
}

@ImplicitReflectionSerializer
fun queryGraphQlSFDetails(ident: String): QueryResponseBase {
        val query = getStringFromResource(GRAPHQL_QUERY).trim()
        return executeGraphQlQuery(query, mapOf("ident" to ident))
}