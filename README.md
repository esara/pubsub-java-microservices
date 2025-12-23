# Google Pub/Sub Java Microservices Demo

A containerized microservices application demonstrating Google Cloud Pub/Sub messaging patterns with multiple microservices consuming and producing messages.

## Architecture

This application demonstrates a **pub-sub pattern** using Google Cloud Pub/Sub:

```
┌─────────────┐
│  Producer   │───publishes───┐
│   Service   │               │
└─────────────┘               │
                              ▼
                    ┌─────────────────┐
                    │  Pub/Sub Topic   │
                    │  (orders-topic)  │
                    └─────────────────┘
                              │
                              │ broadcasts
                              │
                ┌─────────────┴─────────────┐
                │                           │
                ▼                           ▼
        ┌──────────────┐          ┌──────────────┐
        │ Subscription 1│          │ Subscription 2│
        │ (processing)  │          │(notification)│
        └──────────────┘          └──────────────┘
                │                           │
                ▼                           ▼
        ┌──────────────┐          ┌──────────────┐
        │  Consumer 1  │          │  Consumer 2  │
        │   (Order     │          │(Notification)│
        │  Processing) │          │   Service)   │
        └──────────────┘          └──────────────┘
```

### Components

1. **Pub/Sub Producer Service**: Publishes order messages to a Pub/Sub topic
2. **Pub/Sub Topic**: Receives messages from producer and broadcasts to subscribed subscriptions
3. **Subscription 1**: Subscribed to Pub/Sub topic, consumed by Order Processing Service
4. **Subscription 2**: Subscribed to Pub/Sub topic, consumed by Notification Service
5. **Consumer Service 1**: Order Processing Service - processes orders
6. **Consumer Service 2**: Notification Service - sends order confirmations

## Prerequisites

- Docker and Docker Compose installed
- Java 21+ (for local development)
- Maven 3.6+ (for local development)
- At least 2GB of free disk space
- Port 8085 available (for Pub/Sub Emulator)

## Quick Start

1. **Clone or navigate to the project directory**

```bash
cd pubsub-java-microservices
```

2. **Start all services**

```bash
docker-compose up --build
```

This will:
- Start Pub/Sub Emulator (Google Cloud service emulator)
- Create Pub/Sub topic and subscriptions
- Start the producer service (publishes sample orders continuously)
- Start both consumer services (waiting for messages)

3. **Observe the output**

You should see:
- Producer publishing messages to Pub/Sub
- Consumer 1 processing orders
- Consumer 2 sending notifications

## How It Works

### Message Flow

1. **Producer** creates order messages and publishes them to the Pub/Sub topic
2. **Pub/Sub** receives the message and broadcasts it to all subscribed subscriptions
3. **Consumer 1** (Order Processing) receives messages from its subscription and processes orders
4. **Consumer 2** (Notification) receives messages from its subscription and sends notifications

### Key Features Demonstrated

- **Pub-Sub Pattern**: One producer, multiple consumers
- **Message Decoupling**: Services don't need to know about each other
- **Scalability**: Multiple consumers can process messages independently
- **Reliability**: Messages are persisted in subscriptions until processed

## Services Details

### Producer Service

- **Location**: `producer/`
- **Function**: Publishes order messages to Pub/Sub topic
- **Messages**: Creates sample orders continuously with customer info, items, and totals
- **Message Attributes**: Includes order type and priority for filtering
- **Technology**: Spring Boot, Google Cloud Pub/Sub Client

### Order Processing Service

- **Location**: `order-processing/`
- **Function**: Processes orders from the subscription
- **Actions**: 
  - Receives order messages
  - Updates order status to "processing"
  - Logs order details
- **Technology**: Spring Boot, Google Cloud Pub/Sub Client

### Notification Service

- **Location**: `notification/`
- **Function**: Sends notifications for orders
- **Actions**:
  - Receives order messages
  - Sends order confirmation notifications
  - Logs notification details
- **Technology**: Spring Boot, Google Cloud Pub/Sub Client

## Using with Real Google Cloud

To use with real Google Cloud instead of the emulator:

1. **Set up Google Cloud credentials**:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/service-account-key.json
   ```

2. **Create Pub/Sub resources**:
   ```bash
   # Set your project ID
   export GCP_PROJECT_ID=playground
   
   # Create topic
   gcloud pubsub topics create orders-topic --project=$GCP_PROJECT_ID
   
   # Create subscriptions
   gcloud pubsub subscriptions create order-processing-subscription \
     --topic=orders-topic \
     --project=$GCP_PROJECT_ID
   
   gcloud pubsub subscriptions create notification-subscription \
     --topic=orders-topic \
     --project=$GCP_PROJECT_ID
   ```

3. **Update environment variables** in `docker-compose.yml`:
   - Remove or set `PUBSUB_EMULATOR_HOST` to empty/null
   - Set `GCP_PROJECT_ID` to your project ID
   - Mount credentials volume if needed

## Kubernetes Deployment (Helm)

This application can be deployed to Kubernetes using Helm charts. The Helm chart is located in `helm/pubsub-java-microservices/`.

### Prerequisites

- Kubernetes cluster (GKE, EKS, AKS, or local like minikube/kind)
- `kubectl` configured for your cluster
- `helm` 3.0+ installed
- Google Cloud Pub/Sub topic and subscriptions already created (or use emulator)
- Docker images built and pushed to a registry

### Installation

#### 1. Build and Push Docker Images

Build and push the Docker images to your registry:

```bash
# Set your registry and tag
export DOCKERHUB_USERNAME=your-username
export IMAGE_TAG=latest

# Build images
cd producer && docker build -t ${DOCKERHUB_USERNAME}/pubsub-producer:${IMAGE_TAG} .
cd ../order-processing && docker build -t ${DOCKERHUB_USERNAME}/pubsub-order-processing:${IMAGE_TAG} .
cd ../notification && docker build -t ${DOCKERHUB_USERNAME}/pubsub-notification:${IMAGE_TAG} .

# Push images
docker push ${DOCKERHUB_USERNAME}/pubsub-producer:${IMAGE_TAG}
docker push ${DOCKERHUB_USERNAME}/pubsub-order-processing:${IMAGE_TAG}
docker push ${DOCKERHUB_USERNAME}/pubsub-notification:${IMAGE_TAG}
```

#### 2. Create Pub/Sub Resources

##### Option A: Using Real Google Cloud Pub/Sub

```bash
# Set your GCP project ID
export GCP_PROJECT_ID=playground

# Create topic
gcloud pubsub topics create orders-topic --project=$GCP_PROJECT_ID

# Create subscriptions
gcloud pubsub subscriptions create order-processing-subscription \
  --topic=orders-topic \
  --project=$GCP_PROJECT_ID

gcloud pubsub subscriptions create notification-subscription \
  --topic=orders-topic \
  --project=$GCP_PROJECT_ID
```

##### Option B: Using Pub/Sub Emulator (for testing)

If you want to use the Pub/Sub Emulator, deploy it separately and enable it in values.yaml:

```yaml
pubsub:
  emulator:
    enabled: true
    host: pubsub-emulator
    port: 8085
```

#### 3. Configure GCP Workload Identity (for GKE)

If running on GKE and using real GCP Pub/Sub, set up Workload Identity:
https://docs.cloud.google.com/kubernetes-engine/docs/how-to/workload-identity
Workload Identity allows Kubernetes service accounts to impersonate Google service accounts without managing keys. This is the recommended approach for GKE clusters.

**First, verify your cluster has Workload Identity enabled:**
```bash
export CLUSTER_NAME=your-cluster-name
export CLUSTER_LOCATION=us-central1-c

gcloud container clusters describe ${CLUSTER_NAME} \
  --region=${CLUSTER_LOCATION} \
  --format="value(workloadIdentityConfig.workloadPool)"
```

If this returns `PROJECT_ID.svc.id.goog`, Workload Identity is enabled. If it's empty, Workload Identity is not enabled.

**Important: Also verify the node pool has Workload Identity metadata service enabled:**

```bash
# Get the node pool name from a node
NODEPOOL_NAME=$(kubectl get nodes -o jsonpath='{.items[0].metadata.labels.cloud\.google\.com/gke-nodepool}')

# Check the node pool's metadata service mode
gcloud container node-pools describe ${NODEPOOL_NAME} \
  --cluster=${CLUSTER_NAME} \
  --region=${CLUSTER_LOCATION} \
  --format="value(config.workloadMetadataConfig.mode)"
```

This should return `GKE_METADATA`. If it returns `EXPOSED` or `GCE_METADATA`, the node pool needs to be updated to use Workload Identity. You can update an existing node pool:

```bash
gcloud container node-pools update ${NODEPOOL_NAME} \
  --cluster=${CLUSTER_NAME} \
  --region=${CLUSTER_LOCATION} \
  --workload-metadata=GKE_METADATA
```

**Note:** Updating the node pool metadata service requires recreating the nodes, which will cause a brief disruption.

**If Workload Identity is enabled, follow these steps:**

1. **Create Google Service Accounts (GSAs)**:
```bash
# Producer GSA
gcloud iam service-accounts create pubsub-producer \
  --project=$GCP_PROJECT_ID

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:pubsub-producer@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/pubsub.publisher"

# Order Processing GSA
gcloud iam service-accounts create pubsub-order-processing \
  --project=$GCP_PROJECT_ID

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:pubsub-order-processing@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/pubsub.subscriber"

# Notification GSA
gcloud iam service-accounts create pubsub-notification \
  --project=$GCP_PROJECT_ID

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:pubsub-notification@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/pubsub.subscriber"
```

2. **Bind Kubernetes Service Accounts to Google Service Accounts**:
```bash
export GCP_PROJECT_ID=playground-377422
export NAMESPACE=pubsub-microservices

# Producer binding
gcloud iam service-accounts add-iam-policy-binding \
  pubsub-producer@$GCP_PROJECT_ID.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:$GCP_PROJECT_ID.svc.id.goog[$NAMESPACE/producer-service-account]" \
  --project=$GCP_PROJECT_ID

# Order Processing binding
gcloud iam service-accounts add-iam-policy-binding \
  pubsub-order-processing@$GCP_PROJECT_ID.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:$GCP_PROJECT_ID.svc.id.goog[$NAMESPACE/order-processing-service-account]" \
  --project=$GCP_PROJECT_ID

# Notification binding
gcloud iam service-accounts add-iam-policy-binding \
  pubsub-notification@$GCP_PROJECT_ID.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:$GCP_PROJECT_ID.svc.id.goog[$NAMESPACE/notification-service-account]" \
  --project=$GCP_PROJECT_ID
```

3. **Update values.yaml with GSA annotations**:
```yaml
producer:
  serviceAccount:
    annotations:
      iam.gke.io/gcp-service-account: pubsub-producer@PROJECT_ID.iam.gserviceaccount.com

orderProcessing:
  serviceAccount:
    annotations:
      iam.gke.io/gcp-service-account: pubsub-order-processing@PROJECT_ID.iam.gserviceaccount.com

notification:
  serviceAccount:
    annotations:
      iam.gke.io/gcp-service-account: pubsub-notification@PROJECT_ID.iam.gserviceaccount.com
```

#### 4. Install the Chart

Install with default values:

```bash
helm upgrade --install --create-namespace \
  --namespace pubsub-microservices \
  pubsub-java-microservices \
  ./helm/pubsub-java-microservices
```

Install with custom values:

```bash
helm upgrade --install --create-namespace \
  --namespace pubsub-microservices \
  pubsub-java-microservices \
  ./helm/pubsub-java-microservices \
  --set pubsub.projectId=playground
```

### Configuration

#### Values

Key configuration values in `values.yaml`:

- `pubsub.projectId`: GCP project ID
- `pubsub.topic.name`: Pub/Sub topic name
- `pubsub.subscriptions.*`: Subscription names
- `producer.enabled`: Enable/disable producer service
- `orderProcessing.enabled`: Enable/disable order processing service
- `notification.enabled`: Enable/disable notification service
- `*.deployment.replicas`: Number of replicas for each service
- `*.hpa.enabled`: Enable Horizontal Pod Autoscaler
- `*.pdb.enabled`: Enable Pod Disruption Budget

#### Override Values

Create a custom `values.yaml` or use `--set` flags:

```bash
helm upgrade --install pubsub-java-microservices \
  ./helm/pubsub-java-microservices \
  -f my-values.yaml
```

### Upgrading

```bash
helm upgrade pubsub-java-microservices \
  ./helm/pubsub-java-microservices \
  --namespace pubsub-microservices
```

### Uninstalling

```bash
helm uninstall pubsub-java-microservices \
  --namespace pubsub-microservices
```

### Verification

After installation, verify all pods are running:

```bash
kubectl get pods -n pubsub-microservices
```

Check service logs:

```bash
kubectl logs -f deployment/producer -n pubsub-microservices
kubectl logs -f deployment/order-processing -n pubsub-microservices
kubectl logs -f deployment/notification -n pubsub-microservices
```

### Kubernetes Troubleshooting

#### Pods not starting

- Check pod logs: `kubectl logs <pod-name> -n pubsub-microservices`
- Check pod events: `kubectl describe pod <pod-name> -n pubsub-microservices`
- Verify ConfigMap exists: `kubectl get configmap pubsub-config -n pubsub-microservices`

#### Authentication issues (GCP)

- Verify Workload Identity is properly configured
- **Check node pool metadata service**: If pods are using `ComputeEngineCredentials` instead of Workload Identity, verify the node pool's metadata service is set to `GKE_METADATA`:
  ```bash
  NODEPOOL_NAME=$(kubectl get nodes -o jsonpath='{.items[0].metadata.labels.cloud\.google\.com/gke-nodepool}')
  gcloud container node-pools describe ${NODEPOOL_NAME} \
    --cluster=${CLUSTER_NAME} \
    --region=${CLUSTER_LOCATION} \
    --format="value(config.workloadMetadataConfig.mode)"
  ```
  If it's not `GKE_METADATA`, update the node pool (see Workload Identity setup section above).
- Check service account annotations in values.yaml
- Verify GSA has proper IAM roles
- Verify IAM bindings exist: `gcloud iam service-accounts get-iam-policy <GSA_EMAIL>`

#### Pub/Sub connection issues

- Verify topic and subscriptions exist
- Check PUBSUB_PROJECT_ID is correct
- If using emulator, verify it's accessible

### Advanced Features

#### Horizontal Pod Autoscaler

Enable HPA for automatic scaling:

```yaml
orderProcessing:
  hpa:
    enabled: true
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
    targetMemoryUtilizationPercentage: 80
```

#### Pod Disruption Budget

Enable PDB for high availability:

```yaml
orderProcessing:
  pdb:
    enabled: true
    minAvailable: 1
```

#### OpenTelemetry

Enable OpenTelemetry tracing:

```yaml
otlp:
  enabled: true
  endpoint: "http://opentelemetry-collector:4318"
  insecure: "true"
```

## Development

### Running Individual Services

You can run services individually for development:

```bash
# Start only Pub/Sub Emulator
docker-compose up pubsub-emulator

# Run producer (requires emulator running)
cd producer && mvn spring-boot:run

# Run order-processing (requires emulator running)
cd order-processing && mvn spring-boot:run

# Run notification (requires emulator running)
cd notification && mvn spring-boot:run
```

### Viewing Pub/Sub Emulator Resources

Access Pub/Sub Emulator using gcloud CLI:

```bash
# Set emulator host
export PUBSUB_EMULATOR_HOST=localhost:8085

# List topics
gcloud pubsub topics list --project=test-project

# List subscriptions
gcloud pubsub subscriptions list --project=test-project

# Pull messages manually
gcloud pubsub subscriptions pull order-processing-subscription --project=test-project
```

## Cleanup

Stop all services and remove containers:

```bash
docker-compose down
```

Remove Pub/Sub Emulator data:

```bash
docker-compose down -v
```

## Troubleshooting

### Pub/Sub Emulator not starting

- Check if port 8085 is available
- Check Docker logs: `docker-compose logs pubsub-emulator`

### Messages not being received

- Verify topic and subscriptions were created successfully
- Check subscription names match in environment variables
- Verify Pub/Sub Emulator is running and healthy

### Consumer services exiting

- Check logs: `docker-compose logs order-processing notification`
- Verify subscription names are correct
- Ensure Pub/Sub Emulator is running and healthy

## Architecture Benefits

1. **Decoupling**: Producer doesn't need to know about consumers
2. **Scalability**: Add more consumers without changing producer
3. **Reliability**: Messages persist in subscriptions if consumers are down
4. **Flexibility**: Different consumers can process messages differently
5. **Message Filtering**: Can use Pub/Sub message attributes for filtering

## Extending the Application

- Add more consumer services for different processing needs
- Implement message filtering using Pub/Sub message attributes
- Add dead-letter topics for failed message handling
- Implement message batching for better performance
- Add monitoring and observability (Cloud Monitoring, Cloud Trace)
- Implement retry logic and error handling

## License

This is a demonstration project. Feel free to use and modify as needed.

