#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REGISTRY="shopflow"

# ── helpers ──────────────────────────────────────────────────────────────────
log()  { echo "[$(date '+%H:%M:%S')] $*"; }
die()  { echo "ERROR: $*" >&2; exit 1; }

# ── pre-flight ────────────────────────────────────────────────────────────────
command -v minikube >/dev/null || die "minikube not found"
command -v kubectl  >/dev/null || die "kubectl not found"
command -v docker   >/dev/null || die "docker not found"

minikube status --format='{{.Host}}' 2>/dev/null | grep -q "Running" \
  || die "Minikube is not running. Start it with: minikube start --cpus=4 --memory=8g"

# Enable required addons
log "Enabling Minikube addons..."
minikube addons enable ingress   >/dev/null 2>&1 || true
minikube addons enable metrics-server >/dev/null 2>&1 || true

# Point Docker CLI at Minikube's daemon so built images are available in-cluster
log "Pointing Docker to Minikube daemon..."
eval "$(minikube docker-env)"

# ── build images ──────────────────────────────────────────────────────────────
SERVICES=(user-service catalog-service cart-service payment-service order-service notification-service)

for svc in "${SERVICES[@]}"; do
  svc_dir="$ROOT_DIR/$svc"
  [[ -d "$svc_dir" ]] || { log "WARN: $svc_dir not found, skipping"; continue; }
  log "Building $REGISTRY/$svc:latest ..."
  docker build -t "$REGISTRY/$svc:latest" "$svc_dir" --quiet
done

log "Building $REGISTRY/frontend:latest ..."
docker build -t "$REGISTRY/frontend:latest" "$ROOT_DIR/shopflow-frontend" --quiet

# ── apply manifests ───────────────────────────────────────────────────────────
K8S="$ROOT_DIR/k8s"

log "Applying namespace..."
kubectl apply -f "$K8S/namespace.yml"

log "Applying ConfigMap and Secret..."
kubectl apply -f "$K8S/configmaps/common-config.yml"
kubectl apply -f "$K8S/secrets/common-secrets.yml"

log "Applying infrastructure (Postgres, Redis, Kafka, Keycloak, Jaeger)..."
kubectl apply -f "$K8S/infrastructure/postgres.yml"
kubectl apply -f "$K8S/infrastructure/redis.yml"
kubectl apply -f "$K8S/infrastructure/kafka.yml"
kubectl apply -f "$K8S/infrastructure/keycloak.yml"

log "Waiting for Postgres to be ready..."
kubectl rollout status statefulset/postgres -n shopflow --timeout=120s

log "Waiting for Kafka to be ready..."
kubectl rollout status statefulset/kafka -n shopflow --timeout=120s

log "Applying microservices..."
for svc in "${SERVICES[@]}"; do
  svc_dir="$K8S/$svc"
  [[ -d "$svc_dir" ]] || { log "WARN: $svc_dir not found, skipping"; continue; }
  kubectl apply -f "$svc_dir/deployment.yml"
  kubectl apply -f "$svc_dir/service.yml"
done

log "Applying frontend..."
kubectl apply -f "$K8S/frontend/deployment.yml"
kubectl apply -f "$K8S/frontend/service.yml"

log "Applying Ingress..."
kubectl apply -f "$K8S/ingress.yml"

# ── wait for rollouts ─────────────────────────────────────────────────────────
log "Waiting for microservice rollouts..."
for svc in "${SERVICES[@]}"; do
  kubectl rollout status deployment/"$svc" -n shopflow --timeout=180s || \
    log "WARN: $svc rollout timed out — check: kubectl describe pod -n shopflow -l app=$svc"
done

kubectl rollout status deployment/frontend -n shopflow --timeout=60s

# ── print access info ─────────────────────────────────────────────────────────
MINIKUBE_IP="$(minikube ip)"
log ""
log "================================================================"
log "  ShopFlow deployed to Minikube"
log "================================================================"
log "  Frontend (NodePort): http://$MINIKUBE_IP:30080"
log ""
log "  To use the Ingress instead, add this to /etc/hosts:"
log "    $MINIKUBE_IP  shopflow.local"
log "  Then open: http://shopflow.local"
log ""
log "  Keycloak admin:      http://$MINIKUBE_IP:30081  (admin/admin)"
log ""
log "  Port-forward individual services:"
log "    kubectl port-forward svc/user-service    -n shopflow 8081:8081"
log "    kubectl port-forward svc/catalog-service -n shopflow 8082:8082"
log "    kubectl port-forward svc/cart-service    -n shopflow 8083:8083"
log "    kubectl port-forward svc/payment-service -n shopflow 8084:8084"
log "    kubectl port-forward svc/order-service   -n shopflow 8085:8085"
log "================================================================"
