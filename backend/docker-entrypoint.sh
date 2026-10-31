#!/bin/sh
set -eu

production_profile=false
case ",${SPRING_PROFILES_ACTIVE:-}," in
	*,production,*)
		production_profile=true
		: "${DB_HOST:?DB_HOST is required in production}"
		: "${DB_PORT:?DB_PORT is required in production}"
		: "${DB_NAME:?DB_NAME is required in production}"
		: "${DB_SSL_MODE:?DB_SSL_MODE is required in production}"
		[ "$DB_SSL_MODE" = "VERIFY_IDENTITY" ] || {
			echo "Production DB_SSL_MODE must be VERIFY_IDENTITY" >&2
			exit 1
		}
		: "${DB_TRUSTSTORE_PASSWORD:?DB_TRUSTSTORE_PASSWORD is required in production}"
		db_host_lower="$(printf '%s' "$DB_HOST" | tr '[:upper:]' '[:lower:]')"
		case "$db_host_lower" in
			localhost|127.0.0.1|::1)
				echo "Production DB_HOST must not be a local host" >&2
				exit 1
				;;
		esac
		DB_TRUSTSTORE_TYPE="${DB_TRUSTSTORE_TYPE:-PKCS12}"
		AIVEN_CA_CERT_FILE="${AIVEN_CA_CERT_FILE:-/etc/secrets/aiven-ca.pem}"
		test -r "$AIVEN_CA_CERT_FILE" || {
			echo "Aiven CA certificate file is not readable" >&2
			exit 1
		}
		umask 077
		keytool -importcert -noprompt -alias aiven-mysql-ca \
			-file "$AIVEN_CA_CERT_FILE" \
			-keystore /tmp/aiven-mysql-truststore.p12 \
			-storetype "$DB_TRUSTSTORE_TYPE" \
			-storepass:env DB_TRUSTSTORE_PASSWORD
		export DB_TRUSTSTORE_URL=file:/tmp/aiven-mysql-truststore.p12
		export DB_TRUSTSTORE_TYPE
		SPRING_DATASOURCE_URL="${DB_URL:-jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}}"
		;;
	*)
		SPRING_DATASOURCE_URL="${DB_URL:-jdbc:mysql://${DB_HOST:-localhost}:${DB_PORT:-3306}/${DB_NAME:-tutotext_db}}"
		;;
esac
if [ "$production_profile" = "true" ]; then
	database_url_lower="$(printf '%s' "$SPRING_DATASOURCE_URL" | tr '[:upper:]' '[:lower:]')"
	case "$database_url_lower" in
		*://localhost:*|*://localhost/*|*://127.0.0.1:*|*://127.0.0.1/*|*://\[::1\]*)
			echo "Production datasource URL must not use a local host" >&2
			exit 1
			;;
	esac
fi
export SPRING_DATASOURCE_URL
: "${DB_USERNAME:?DB_USERNAME is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
export SPRING_DATASOURCE_USERNAME="$DB_USERNAME"
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
if [ -n "${JWT_SECRET:-}" ]; then
	export APP_SECURITY_JWT_SECRET="$JWT_SECRET"
fi

exec java \
	-XX:+UseContainerSupport \
	-XX:InitialRAMPercentage=20.0 \
	-XX:MaxRAMPercentage=75.0 \
	-XX:+ExitOnOutOfMemoryError \
	-jar /app/app.jar
