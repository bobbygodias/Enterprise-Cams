# kotlinx.serialization supplies its own consumer rules.

# Authorized, bundled SDK modules are discovered via ServiceLoader.
-keep interface org.enterprisecams.app.cloud.CloudConnectorProvider
-keep class * implements org.enterprisecams.app.cloud.CloudConnectorProvider { public <init>(); }
