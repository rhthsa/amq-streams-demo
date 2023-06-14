CONTAINER_NAME=quay.io/voravitl/song
TAG=$1
mvn clean package -DskipTests=true
podman build -f src/main/docker/Dockerfile.jvm \
-t ${CONTAINER_NAME}:${TAG} .
