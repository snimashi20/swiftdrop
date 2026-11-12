# SwiftDrop — Kubernetes Deployment & Helm Guide

This directory contains the production-grade Kubernetes orchestration manifests and the parameterized Helm chart for the SwiftDrop courier platform, designed and maintained by **Sandavi (M10)**.

---

## 1. Directory Structure

```
deploy/
├── k8s/                          # Raw declarative Kubernetes manifests
│   ├── namespace.yaml            # 'swiftdrop' namespace definition
│   ├── configmap.yaml            # Environment endpoints (Postgres, RabbitMQ, Mailpit)
│   ├── secret.yaml               # Sensitive credentials (DB passwords, JWT secret, OAuth)
│   ├── postgres.yaml             # StatefulSet + ClusterIP Service + 5Gi PVC
│   ├── rabbitmq.yaml             # StatefulSet + Service (AMQP 5672, Mgmt 15672) + 2Gi PVC
│   ├── mailpit.yaml              # Deployment + Service (SMTP 1025, Web UI 8025)
│   ├── core-api.yaml             # Deployment (2 replicas) + Service + Actuator Probes
│   ├── mock-merchant-store.yaml  # Deployment (1 replica) + Webhook receiver Service (8085)
│   ├── notification-service.yaml # Deployment (1 replica) + Email/SMS consumer Service (8081)
│   ├── frontend.yaml             # Deployment (2 replicas) + Nginx SPA Service (80)
│   └── ingress.yaml              # Nginx Ingress routing hostnames and WebSocket upgrade
│
└── helm/swiftdrop/               # Production-ready parameterized Helm chart
    ├── Chart.yaml                # Chart metadata (v1.0.0)
    ├── values.yaml               # Configurable overrides for local and cloud clusters
    └── templates/                # Templated manifests (_helpers, configs, workloads, ingress)
```

---

## 2. Prerequisites

* **Docker Desktop** (with Kubernetes enabled) OR **Minikube** installed.
* **kubectl** (Kubernetes command-line tool).
* **Helm v3** installed (`choco install kubernetes-helm` or `brew install helm`).

---

## 3. Option A: Quick Start with Minikube & Helm (Recommended)

### Step 1: Start Minikube & Enable NGINX Ingress
```bash
# Start Minikube cluster with sufficient resources
minikube start --cpus=4 --memory=8192 --disk-size=20g

# Enable the Ingress Controller addon
minikube addons enable ingress
```

### Step 2: Build or Load Local Docker Images into Minikube
```bash
# Point your shell to Minikube's internal Docker daemon
eval $(minikube docker-env)

# Build system container images
docker build -t swiftdrop-core-api:latest ./backend/core-api
docker build -t swiftdrop-mock-store:latest ./backend/mock-merchant-store
docker build -t swiftdrop-notification-service:latest ./backend/notification-service
docker build -t swiftdrop-frontend:latest ./frontend
```

### Step 3: Deploy SwiftDrop via Helm
```bash
# Create namespace
kubectl create namespace swiftdrop

# Install Helm release
helm install swiftdrop ./deploy/helm/swiftdrop -n swiftdrop
```

### Step 4: Verify Deployment Status
```bash
# Check all pods, services, and PVCs
kubectl get all -n swiftdrop

# Watch until all pods transition to Running / Ready
kubectl get pods -n swiftdrop -w
```

---

## 4. Option B: Deploy Using Raw Manifests (`kubectl`)

If you prefer applying raw YAML files without Helm:

```bash
# 1. Create namespace
kubectl apply -f deploy/k8s/namespace.yaml

# 2. Apply configuration and secrets
kubectl apply -f deploy/k8s/configmap.yaml
kubectl apply -f deploy/k8s/secret.yaml

# 3. Apply stateful infrastructure (Databases & Brokers)
kubectl apply -f deploy/k8s/postgres.yaml
kubectl apply -f deploy/k8s/rabbitmq.yaml
kubectl apply -f deploy/k8s/mailpit.yaml

# 4. Apply application workloads
kubectl apply -f deploy/k8s/core-api.yaml
kubectl apply -f deploy/k8s/mock-merchant-store.yaml
kubectl apply -f deploy/k8s/notification-service.yaml
kubectl apply -f deploy/k8s/frontend.yaml

# 5. Apply Ingress routes
kubectl apply -f deploy/k8s/ingress.yaml
```

---

## 5. Domain Name Resolution (`/etc/hosts`)

Add the following entry to your system's `hosts` file (`C:\Windows\System32\drivers\etc\hosts` on Windows, or `/etc/hosts` on macOS/Linux):

1. Find the Minikube IP:
   ```bash
   minikube ip
   ```
2. Add the hosts entry (replace `<MINIKUBE_IP>` with the output from above, typically `192.168.49.2` or `127.0.0.1`):
   ```
   <MINIKUBE_IP>   swiftdrop.local mock-store.local mailpit.local
   ```

---

## 6. Accessing Platform Services

| Service / Interface | URL in Browser | Routed Ingress Target |
| :--- | :--- | :--- |
| **Frontend Web App** | `http://swiftdrop.local/` | `frontend-service:80` |
| **Backend REST APIs** | `http://swiftdrop.local/api/v1/...` | `core-api-service:8080` |
| **Swagger UI Documentation** | `http://swiftdrop.local/swagger-ui.html` | `core-api-service:8080` |
| **Live Tracking WebSocket** | `ws://swiftdrop.local/ws` | `core-api-service:8080` |
| **Mock Merchant Store** | `http://mock-store.local/` | `mock-merchant-service:8085` |
| **Mailpit Email Inspector** | `http://mailpit.local/` | `mailpit-service:8025` |

---

## 7. Useful Troubleshooting & Inspection Commands

```bash
# View Core-API application logs
kubectl logs -f -l app=core-api -n swiftdrop

# View Webhook Receiver logs in Mock Merchant Store
kubectl logs -f -l app=mock-merchant-store -n swiftdrop

# Execute a bash shell inside PostgreSQL
kubectl exec -it postgres-0 -n swiftdrop -- psql -U swiftdrop -d swiftdrop

# Check Ingress routing table
kubectl describe ingress swiftdrop-ingress -n swiftdrop

# Uninstall and clean up Helm deployment
helm uninstall swiftdrop -n swiftdrop
kubectl delete namespace swiftdrop
```
