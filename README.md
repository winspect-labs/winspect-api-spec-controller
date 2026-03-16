# Winspect ApiSpec Controller

Kubernetes operator that watches ApiSpec CRDs and syncs them to the central Winspect platform. When you add or update an ApiSpec in your cluster, the controller automatically creates or updates the API and spec record in Winspect.

## Overview

| Component | Purpose |
|-----------|---------|
| **ApiSpec CRD** | Defines the schema for ApiSpec resources. Without it, Kubernetes doesn't know what ApiSpec is. |
| **Controller** | The running process that watches ApiSpec resources and syncs them to Winspect. **You must deploy both.** |

Applying only the CRD registers the resource type but does nothing—the controller must be running to process ApiSpecs.

## Features

- **Watch ApiSpec CRDs** — Monitors ApiSpec resources across namespaces
- **Sync to central API** — Creates or updates API + SpecRecord via `POST /api-management/discovery/spec-records/sync`
- **Inline and ConfigMapRef** — Supports both `definition.inline` and `definition.configMapRef`
- **Delete behavior** — When ApiSpec is deleted, the API and spec record remain unchanged (per design)

## Configuration

Configure via environment variables or `application.yml`:

| Variable | Description |
|----------|-------------|
| `APISPEC_ORG_ID` | Organization ID (required) |
| `APISPEC_API_KEY` | API key for central platform (required) |
| `APISPEC_CENTRAL_API_URL` | Central API base URL (required) |
| `APISPEC_CLUSTER_ID` | Cluster identifier (optional) |
| `APISPEC_SYNC_INTERVAL_MS` | Sync interval in ms (default: 60000) |

## Installation

### Step 1: Install the ApiSpec CRD

The CRD defines the ApiSpec resource type. Install from the repo or from the public URL:

```bash
# From local clone
kubectl apply -f crds/apispec-crd.yaml

# Or from public URL (no clone needed)
kubectl apply -f https://raw.githubusercontent.com/winspect-labs/winspect-api-spec-controller/main/crds/apispec-crd.yaml
```

Wait for the CRD to be established:

```bash
kubectl wait --for=condition=established crd/apispecs.winspect.io --timeout=60s
```

### Step 2: Create namespace and RBAC

```bash
kubectl create namespace winspect-system
kubectl apply -f manifests/rbac.yaml
```

### Step 3: Create Secret with credentials

Copy the example and fill in your values:

```bash
cp manifests/secret.yaml.example manifests/secret.yaml
# Edit secret.yaml with your APISPEC_ORG_ID, APISPEC_API_KEY, APISPEC_CENTRAL_API_URL
kubectl apply -f manifests/secret.yaml
```

### Step 4: Deploy the controller

```bash
kubectl apply -f manifests/deployment.yaml
```

The controller image is `ghcr.io/winspect-labs/winspect-api-spec-controller:latest`. To build and push your own image:

```bash
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=ghcr.io/your-org/winspect-api-spec-controller:latest
docker push ghcr.io/your-org/winspect-api-spec-controller:latest
```

Then edit `manifests/deployment.yaml` to use your image.

### Verify

```bash
kubectl get pods -n winspect-system -l app=winspect-api-spec-controller
kubectl logs -f deployment/winspect-api-spec-controller -n winspect-system
```

## Quick install (all steps)

```bash
# 1. CRD
kubectl apply -f https://raw.githubusercontent.com/winspect-labs/winspect-api-spec-controller/main/crds/apispec-crd.yaml
kubectl wait --for=condition=established crd/apispecs.winspect.io --timeout=60s

# 2. Namespace + RBAC
kubectl create namespace winspect-system
kubectl apply -f https://raw.githubusercontent.com/winspect-labs/winspect-api-spec-controller/main/manifests/rbac.yaml

# 3. Secret (create from example, then apply)
# 4. Deployment (after building/pushing image)
kubectl apply -f manifests/deployment.yaml
```

## Viewing logs

```bash
kubectl logs -f deployment/winspect-api-spec-controller -n winspect-system
```

With timestamps:

```bash
kubectl logs -f deployment/winspect-api-spec-controller -n winspect-system --timestamps
```

## Stopping the controller

```bash
kubectl delete deployment winspect-api-spec-controller -n winspect-system
```

The CRD and any ApiSpec resources remain. Existing APIs in Winspect are unchanged (per design).

## Usage with winspect-cli

In your CI/CD pipeline, use `winspect resolve` to resolve tokens and embed spec content before applying:

```bash
winspect resolve --spec api-spec.yaml | kubectl apply -f -
```

## Public CRD URL

For automation or docs, the CRD is available at:

```
https://raw.githubusercontent.com/winspect-labs/winspect-api-spec-controller/main/crds/apispec-crd.yaml
```

For a specific version (after tagging releases):

```
https://raw.githubusercontent.com/winspect-labs/winspect-api-spec-controller/v0.1.0/crds/apispec-crd.yaml
```

---

See the [ApiSpec CRD and Winspect Operator Plan](.cursor/plans/apispec_crd_and_winspect_operator_4d78f31b.plan.md) for full architecture.
