package com.flowtrace.domain.cert

data class CertificateStatus(
  val hasCa: Boolean,
  val installed: Boolean,
  val fingerprintSha256: String?,
  val commonName: String?,
)

sealed interface CaMaterial {
  data class P12(val bytes: ByteArray, val password: String) : CaMaterial
  data class Pem(val caPem: String, val keyPem: String) : CaMaterial
}

interface CertificateRepository {
  suspend fun getStatus(): Result<CertificateStatus>
  suspend fun generateCa(commonName: String, password: String): Result<CaMaterial.P12>
  suspend fun importCa(p12: CaMaterial.P12): Result<Unit>
  suspend fun exportCa(): Result<CaMaterial>
}


