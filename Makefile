test: docker runProxy
	./gradlew test --rerun-tasks
	jps | grep "redis-proxy.jar" | awk '{print $$1}' | xargs kill

docker:
	docker-compose up -d

runProxy:
	./gradlew jar
	java -jar ./build/libs/redis-proxy.jar localhost 5 5000 &
