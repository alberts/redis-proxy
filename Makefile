test: docker
	docker exec -it redisproxy_proxy_1 ./gradlew test --rerun-tasks 

docker:
	docker-compose up -d
