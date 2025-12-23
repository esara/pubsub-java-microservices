.PHONY: help build build-all docker-build docker-build-local docker-setup-buildx up down logs clean test

# DockerHub username (set via DOCKERHUB_USERNAME env var or override)
DOCKERHUB_USERNAME ?= esara

# Image tag (set via IMAGE_TAG env var or override)
IMAGE_TAG ?= latest

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build all services with Maven
build-all: ## Build all services with Maven
	cd producer && mvn clean package -DskipTests
	cd order-processing && mvn clean package -DskipTests
	cd notification && mvn clean package -DskipTests

# Build Docker images using docker-compose
build: ## Build all Docker images using docker-compose
	docker-compose build

# Setup Docker buildx for multi-arch builds
docker-setup-buildx: ## Setup Docker buildx builder for multi-arch
	@echo "Setting up Docker buildx for multi-arch builds..."
	docker buildx create --name multiarch --use 2>/dev/null || docker buildx use multiarch
	docker buildx inspect --bootstrap

# Build and push multi-arch Docker images to DockerHub
docker-build: ## Build and push multi-arch images (amd64 + arm64) to DockerHub
	@echo "Building multi-arch images for arm64 and amd64 with tag: $(IMAGE_TAG)..."
	docker buildx build --platform linux/amd64,linux/arm64 \
		-t $(DOCKERHUB_USERNAME)/pubsub-producer:$(IMAGE_TAG) \
		--push ./producer
	docker buildx build --platform linux/amd64,linux/arm64 \
		-t $(DOCKERHUB_USERNAME)/pubsub-order-processing:$(IMAGE_TAG) \
		--push ./order-processing
	docker buildx build --platform linux/amd64,linux/arm64 \
		-t $(DOCKERHUB_USERNAME)/pubsub-notification:$(IMAGE_TAG) \
		--push ./notification
	@echo "✅ Images built and pushed to DockerHub with tag: $(IMAGE_TAG)"

# Build single-arch images locally (for testing, no push)
docker-build-local: ## Build images locally for current platform (no push)
	@echo "Building images locally for current platform (no push) with tag: $(IMAGE_TAG)..."
	docker build -t pubsub-producer:$(IMAGE_TAG) ./producer
	docker build -t pubsub-order-processing:$(IMAGE_TAG) ./order-processing
	docker build -t pubsub-notification:$(IMAGE_TAG) ./notification

up: ## Start all services
	docker-compose up -d

down: ## Stop all services
	docker-compose down

logs: ## Show logs from all services
	docker-compose logs -f

logs-producer: ## Show logs from producer service
	docker-compose logs -f producer

logs-order-processing: ## Show logs from order-processing service
	docker-compose logs -f order-processing

logs-notification: ## Show logs from notification service
	docker-compose logs -f notification

clean: ## Stop services and remove volumes
	docker-compose down -v
	cd producer && mvn clean
	cd order-processing && mvn clean
	cd notification && mvn clean

restart: ## Restart all services
	docker-compose restart

ps: ## Show running containers
	docker-compose ps

test: ## Run tests (placeholder)
	@echo "Tests not implemented yet"

