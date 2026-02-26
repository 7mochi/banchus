#!/usr/bin/env make
run:
	./gradlew :banchus-app:bootRun

run-caddy:
	caddy run --envfile .env --config ext/Caddyfile

lint:
	./gradlew spotlessApply


